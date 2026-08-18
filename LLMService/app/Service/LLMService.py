import json

from app.Config.ApifyConfigAnalyzeVideo import get_tiktok_video_with_url, get_instagram_video_with_url, get_facebook_video_with_url, get_x_video_with_url
from app.Config.ApifyConfigAnalyzeContent import search_youtube_videos, search_x_videos, search_tiktok_videos , search_instagram_videos, search_facebook_videos
from app.Config.RabitMqConfig import (rabbitmq_manager, ANALYZE_VIDEO_RESPONSE_ROUTING_KEY
, SCRAPING_ANALYZE_VIDEO_FINISHED_ROUTING_KEY, ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY)
from app.Config.RedisConfig import get_cache, set_cache
from app.Schemea.AnalyzeVideoSchema import AnalyzeVideoSchema, AnalyzeVideoResponse, AnalyzeVideoStatus, \
    ScrapingCompletedResponse, ScrapingStatus
from app.Schemea.AnalyzeContentSchema import AnalyzeContentScrape, Platform, AnalyzeContentScrapeResponse, \
    ExtractedVideoDetails
from app.Config.GeminiConfig import analyze_video_url, upload_video_url_to_gemini
from app.Util.CheckUrlPlatform import check_url_platform, Platform
from app.Util.PromptBuilder import build_video_url_analysis_prompt, build_video_file_analysis_prompt
from app.Util.SelectReleventVideos import select_top_relevant_videos
from app.Service.DiagramService import DiagramService
from app.Global.RedisGlobalVaribale import KEY_PREFIX_SCRAPE_ANALYZE_CONTENT, KEY_PREFIX_SCRAPE_NICHE_CONTENT, SCRAPE_ANALYZE_CONTENT_TTL, SCRAPE_NICHE_CONTENT_TTL
import logging
import asyncio

logger = logging.getLogger(__name__)


async def analyze_content_scrape_video(analyze_content_scrape: AnalyzeContentScrape, ) -> None:

    if (
            analyze_content_scrape.order_id is None
            or analyze_content_scrape.platform is None
            or analyze_content_scrape.sum_of_content is None
            or analyze_content_scrape.user_id is None
    ):
        logger.error("Something went missing, please check the variables: %s",analyze_content_scrape,)

        response = AnalyzeContentScrapeResponse(
            orderId=analyze_content_scrape.order_id,
            status=ScrapingStatus.FAILED,
            message="Required analyze content data is missing",
        )

        await rabbitmq_manager.publish(
            response=response,
            routing_key=ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY,
        )
        return
    niche_cache_key = (
        f"{KEY_PREFIX_SCRAPE_NICHE_CONTENT}:"
        f"{analyze_content_scrape.platform.value}:"
        f"{analyze_content_scrape.niche.strip().lower()}"
    )
    cached_video_details = await get_cache(niche_cache_key)

    if cached_video_details:
        logger.info("Video details found in cache, skipping search")

        video_details = [
            ExtractedVideoDetails.model_validate(video)
            for video in json.loads(cached_video_details)
        ]
        analyze_content_cache_key = (
            f"{KEY_PREFIX_SCRAPE_ANALYZE_CONTENT}:"
            f"{analyze_content_scrape.order_id}"
        )

        await set_cache( analyze_content_cache_key,
            json.dumps([video.model_dump(mode="json")for video in video_details]),
            SCRAPE_ANALYZE_CONTENT_TTL,
        )
        response = AnalyzeContentScrapeResponse(
            orderId=analyze_content_scrape.order_id,
            status=ScrapingStatus.SUCCEED,
            message="Analyze content scrape video successfully",
            videos=video_details,
        )

        await rabbitmq_manager.publish(
            response=response,
            routing_key=ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY,
        )
        return
    try:
        if analyze_content_scrape.platform == Platform.YOUTUBE:
            video_details = await asyncio.to_thread(
                search_youtube_videos,
                analyze_content_scrape.niche,
            )

        elif analyze_content_scrape.platform == Platform.X:
            video_details = await asyncio.to_thread(
                search_x_videos,
                analyze_content_scrape.niche,
            )

        elif analyze_content_scrape.platform == Platform.TIKTOK:
            video_details = await asyncio.to_thread(
                search_tiktok_videos,
                analyze_content_scrape.niche,
            )

        elif analyze_content_scrape.platform == Platform.FACEBOOK:
            video_details = await asyncio.to_thread(
                search_facebook_videos,
                analyze_content_scrape.niche,
            )

        elif analyze_content_scrape.platform == Platform.INSTAGRAM:
            video_details = await asyncio.to_thread(
                search_instagram_videos,
                analyze_content_scrape.niche,
            )

        else:
            logger.error(
                "Invalid platform: %s",
                analyze_content_scrape.platform,
            )

            response = AnalyzeContentScrapeResponse(
                orderId=analyze_content_scrape.order_id,
                status=ScrapingStatus.FAILED,
                message="Invalid platform",
            )

            await rabbitmq_manager.publish(
                response=response,
                routing_key=ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY,
            )
            return

        if not video_details:
            logger.error(
                "No videos found for the given niche: %s",
                analyze_content_scrape.niche,
            )

            response = AnalyzeContentScrapeResponse(
                orderId=analyze_content_scrape.order_id,
                status=ScrapingStatus.FAILED,
                message="No videos found for the given niche",
            )

            await rabbitmq_manager.publish(
                response=response,
                routing_key=ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY,
            )
            return

        relevant_video_details = select_top_relevant_videos(
            video_details
        )

        response = AnalyzeContentScrapeResponse(
            orderId=analyze_content_scrape.order_id,
            status=ScrapingStatus.SUCCEED,
            message="Analyze content scrape video successfully",
            videos=relevant_video_details,
        )

        await rabbitmq_manager.publish(
            response=response,
            routing_key=ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY,
        )
        # saving the data in redis
        analyze_content_cache_key = (
            f"{KEY_PREFIX_SCRAPE_ANALYZE_CONTENT}:"
            f"{analyze_content_scrape.order_id}"
        )

        niche_cache_key = (
            f"{KEY_PREFIX_SCRAPE_NICHE_CONTENT}:"
            f"{analyze_content_scrape.platform.value}:"
            f"{analyze_content_scrape.niche.strip().lower()}"
        )
        await set_cache(
            analyze_content_cache_key,
            json.dumps([
                video.model_dump(mode="json")
                for video in relevant_video_details
            ]),
            SCRAPE_ANALYZE_CONTENT_TTL,
        )

        await set_cache(
            niche_cache_key,
            json.dumps([
                video.model_dump(mode="json")
                for video in relevant_video_details
            ]),
            SCRAPE_NICHE_CONTENT_TTL,
        )

    except Exception as e:
        logger.exception("Failed to analyze content scrape video: %s",e,)

        response = AnalyzeContentScrapeResponse(
            orderId=analyze_content_scrape.order_id,
            status=ScrapingStatus.FAILED,
            message="Failed to analyze content scrape video",
        )

        await rabbitmq_manager.publish(
            response=response,
            routing_key=ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY,
        )


class LLMService:
    def __init__(self):
        self.diagram_service = DiagramService()
    async def analyze_video( self, analyze_video_schema: AnalyzeVideoSchema,) -> None:
        logger.info(f"Received analyze video request: {analyze_video_schema}")
        # 1. לא התקבל שום מקור וידאו
        if (analyze_video_schema.video_url is None and
            analyze_video_schema.video_gemini_url is None):
            logger.error("Video URL or S3 URL is not provided")

            response = AnalyzeVideoResponse(
                status=AnalyzeVideoStatus.SERVER_FAILED,
                message="Video URL or S3 URL is required",
                order_id=analyze_video_schema.order_id,
            )

            await rabbitmq_manager.publish(
                payload=response,
                routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
            )
            return

        # 2. המשתמש שלח URL
        if analyze_video_schema.video_url is not None:
            platform = check_url_platform(
                analyze_video_schema.video_url
            )

            if platform == Platform.NOT_SUPPORT:
                logger.error(
                    "Video URL is not supported, url: %s",
                    analyze_video_schema.video_url
                )

                scraping_response = ScrapingCompletedResponse(
                    order_id=analyze_video_schema.order_id,
                    status=ScrapingStatus.FAILED,
                    message="The provided video link is not supported",
                )

                await rabbitmq_manager.publish(
                    payload=scraping_response,
                    routing_key=SCRAPING_ANALYZE_VIDEO_FINISHED_ROUTING_KEY,
                )
                return

            try:
                if platform == Platform.TIKTOK:
                    video_details = await asyncio.to_thread(
                        get_tiktok_video_with_url,
                        analyze_video_schema.video_url,
                    )

                elif platform == Platform.INSTAGRAM:
                    video_details = await asyncio.to_thread(
                        get_instagram_video_with_url,
                        analyze_video_schema.video_url,
                    )

                elif platform == Platform.FACEBOOK:
                    video_details = await asyncio.to_thread(
                        get_facebook_video_with_url,
                        analyze_video_schema.video_url,
                    )

                elif platform == Platform.X:
                    video_details = await asyncio.to_thread(
                        get_x_video_with_url,
                        analyze_video_schema.video_url,
                    )

                elif platform == Platform.YOUTUBE:
                    video_details = {
                        "name": None,
                        "urls": [analyze_video_schema.video_url],
                        "platform": Platform.YOUTUBE,
                    }

                else:
                    return

            except Exception as e:
                logger.exception(
                    f"Failed to scrape video: {e}"
                )

                scraping_response = ScrapingCompletedResponse(
                    order_id=analyze_video_schema.order_id,
                    status=ScrapingStatus.FAILED,
                    message="Failed to scrape video",
                )

                await rabbitmq_manager.publish(
                    payload=scraping_response,
                    routing_key=SCRAPING_ANALYZE_VIDEO_FINISHED_ROUTING_KEY,
                )
                return

            # scraping הצליח
            scraping_response = ScrapingCompletedResponse(
                order_id=analyze_video_schema.order_id,
                status=ScrapingStatus.SUCCEED,
                message="Video scraping completed",
            )

            await rabbitmq_manager.publish(
                payload=scraping_response,
                routing_key=SCRAPING_ANALYZE_VIDEO_FINISHED_ROUTING_KEY,
            )

            prompt = build_video_url_analysis_prompt(
                video_details["platform"],
                video_details["name"],
                analyze_video_schema.business_context,
                analyze_video_schema.business_target_audience,

            )
            gemini_url = video_details["urls"][0]
            try:
                if video_details["platform"] != Platform.YOUTUBE:
                    gemini_url = await asyncio.to_thread(
                        upload_video_url_to_gemini,
                        gemini_url,
                        video_details.get("audioUrl"),
                        video_details.get("platform")
                    )

                diagram_create = await asyncio.to_thread(
                    analyze_video_url,
                    gemini_url,
                    prompt,
                )

            except Exception as e:
                logger.exception(
                    f"Failed to upload/analyze video: {e}"
                )

                response = AnalyzeVideoResponse(
                    status=AnalyzeVideoStatus.SERVER_FAILED,
                    message="Failed to analyze video",
                    order_id=analyze_video_schema.order_id,
                )

                await rabbitmq_manager.publish(
                    payload=response,
                    routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
                )
                return

            diagram_id = await asyncio.to_thread(
                self.diagram_service.create_diagram,
                diagram_create,
                analyze_video_schema.user_id,
            )


            response = AnalyzeVideoResponse(
                status=AnalyzeVideoStatus.SUCCEED,
                message="Video analyzed successfully",
                order_id=analyze_video_schema.order_id,
                diagram_id=diagram_id,
                video_gemini_url=gemini_url,
            )

            await rabbitmq_manager.publish(
                payload=response,
                routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
            )

            return

        # 3. המשתמש העלה קובץ ל-gemini
        elif analyze_video_schema.video_gemini_url is not None:
            prompt = build_video_file_analysis_prompt(analyze_video_schema.business_context, analyze_video_schema.business_target_audience, )

            try:
                diagram_create = await asyncio.to_thread(
                    analyze_video_url,
                    analyze_video_schema.video_gemini_url,
                    prompt,
                )

            except Exception as e:
                logger.exception(
                    f"Failed to analyze S3 video: {e}"
                )

                response = AnalyzeVideoResponse(
                    status=AnalyzeVideoStatus.SERVER_FAILED,
                    message="Failed to analyze uploaded video",
                    order_id=analyze_video_schema.order_id,
                )

                await rabbitmq_manager.publish(
                    payload=response,
                    routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
                )
                return

            diagram_id = await asyncio.to_thread(
                self.diagram_service.create_diagram,
                diagram_create,
                analyze_video_schema.user_id,
            )


            response = AnalyzeVideoResponse(
                status=AnalyzeVideoStatus.SUCCEED,
                message="Video analyzed successfully",
                order_id=analyze_video_schema.order_id,
                diagram_id=diagram_id,
                video_gemini_url=analyze_video_schema.video_gemini_url,
            )

            await rabbitmq_manager.publish(
                payload=response,
                routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
            )

            return


