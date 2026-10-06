import json

from app.Config.ApifyConfigAnalyzeVideo import get_tiktok_video_with_url, get_instagram_video_with_url, get_facebook_video_with_url, get_x_video_with_url
from app.Config.ApifyConfigAnalyzeContent import search_youtube_videos, search_x_videos, search_tiktok_videos , search_instagram_videos, search_facebook_videos
from app.Config.ApifyConfigAnalyzeContentGetMp4Link import get_mp4_link_from_x, get_mp4_link_from_facebook
from app.Config.RabitMqConfig import (rabbitmq_manager, ANALYZE_VIDEO_RESPONSE_ROUTING_KEY
, SCRAPING_ANALYZE_VIDEO_FINISHED_ROUTING_KEY, ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY,
ORDER_ANALYZE_CONTENT_RESPONSE_ROUTING_KEY)
from app.Config.RedisConfig import get_cache, set_cache
from app.Schemea.AnalyzeVideoSchema import AnalyzeVideoSchema, AnalyzeVideoResponse, AnalyzeVideoStatus, \
    ScrapingCompletedResponse, ScrapingStatus
from app.Schemea.AnalyzeContentSchema import AnalyzeContentScrape, AnalyzeContentPlatform, AnalyzeContentScrapeResponse, \
    ExtractedVideoDetails, AnalyzeVideoDto, analyzeContentResponse, diagramDetails
from app.Config.GeminiConfig import analyze_video_url, upload_video_url_to_gemini, upload_videos_urls_to_gemini, analyze_content, analyze_content_save_diagram, create_diagram_based_on_videos
from app.Util.CheckUrlPlatform import check_url_platform, UrlPlatform
from app.Util.PromptBuilder import build_video_url_analysis_prompt, build_video_file_analysis_prompt, build_diagram_for_analyze_content_prompt, analyze_content_prompt, create_diagram_based_on_videos_prompt
from app.Util.SelectReleventVideos import select_top_relevant_videos
from app.Service.DiagramService import DiagramService
from app.Service.QueryExpansionService import query_expansion_service
from app.Global.RedisGlobalVaribale import KEY_PREFIX_SCRAPE_ANALYZE_CONTENT, KEY_PREFIX_SCRAPE_NICHE_CONTENT, SCRAPE_ANALYZE_CONTENT_TTL, SCRAPE_NICHE_CONTENT_TTL
import logging
import asyncio

logger = logging.getLogger(__name__)

class LLMService:
    def __init__(self):
        self.diagram_service = DiagramService()
        self.query_expansion_service = query_expansion_service
    async def analyze_content_scrape(self, analyze_content_scrape: AnalyzeContentScrape, ) -> None:

        if (
                analyze_content_scrape.order_id is None
                or analyze_content_scrape.platform is None
                or analyze_content_scrape.sum_of_content is None
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
                exchange=rabbitmq_manager.order_analyze_content_exchange
            )
            return
        niche_cache_key = (
            f"{KEY_PREFIX_SCRAPE_NICHE_CONTENT}:"
            f"{analyze_content_scrape.platform.value}:"
            f"{analyze_content_scrape.niche.strip().lower()}"
        )
        cached_video_details = await get_cache(niche_cache_key)

        if cached_video_details:
            parsed_video_details = json.loads(cached_video_details)

            if parsed_video_details is not None:

                video_details = [
                    ExtractedVideoDetails.model_validate(video)
                    for video in parsed_video_details
                ]

                analyze_content_cache_key = (
                    f"{KEY_PREFIX_SCRAPE_ANALYZE_CONTENT}:"
                    f"{analyze_content_scrape.order_id}"
                )

                await set_cache(
                    analyze_content_cache_key,
                    json.dumps([
                        video.model_dump(mode="json")
                        for video in video_details
                    ]),
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
                    exchange=rabbitmq_manager.order_analyze_content_exchange,
                )

                return
        try:
            niche_queries = await asyncio.to_thread(
                self.query_expansion_service.generate_search_queries,
                analyze_content_scrape.niche,
            )
            logger.info("niche_queries: %s",niche_queries)
            if analyze_content_scrape.platform == AnalyzeContentPlatform.YOUTUBE:
                video_details = await asyncio.to_thread(
                    search_youtube_videos,
                    niche_queries,
                )

            elif analyze_content_scrape.platform == AnalyzeContentPlatform.X:
                video_details = await asyncio.to_thread(
                    search_x_videos,
                    niche_queries,
                )

            elif analyze_content_scrape.platform == AnalyzeContentPlatform.TIKTOK:
                video_details = await asyncio.to_thread(
                    search_tiktok_videos,
                    niche_queries,
                )

            elif analyze_content_scrape.platform == AnalyzeContentPlatform.FACEBOOK:
                video_details = await asyncio.to_thread(
                    search_facebook_videos,
                    niche_queries,
                )

            elif analyze_content_scrape.platform == AnalyzeContentPlatform.INSTAGRAM:
                video_details = await asyncio.to_thread(
                    search_instagram_videos,
                    niche_queries,
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
                    exchange=rabbitmq_manager.order_analyze_content_exchange
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
                    exchange=rabbitmq_manager.order_analyze_content_exchange
                )
                return

            relevant_video_details = select_top_relevant_videos(video_details)
            if analyze_content_scrape.platform in (AnalyzeContentPlatform.X,AnalyzeContentPlatform.FACEBOOK,):
                if analyze_content_scrape.platform == AnalyzeContentPlatform.X:
                    m4_links = await asyncio.to_thread(get_mp4_link_from_x, relevant_video_details)
                else:
                    m4_links = await asyncio.to_thread(get_mp4_link_from_facebook, relevant_video_details)
                for video, m4_link in zip(relevant_video_details, m4_links):
                    video.mp4_link = m4_link


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
            response = AnalyzeContentScrapeResponse(
                orderId=analyze_content_scrape.order_id,
                status=ScrapingStatus.SUCCEED,
                message="Select the videos you want to analyze.",
                videos=relevant_video_details,
            )

            await rabbitmq_manager.publish(
                response=response,
                routing_key=ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY,
                exchange=rabbitmq_manager.order_analyze_content_exchange
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
                exchange=rabbitmq_manager.order_analyze_content_exchange
            )
    # when user choosing the videos to analyze
    async def analyze_content_video(self, videos_details: AnalyzeVideoDto) -> None:
        summaries: list[str] = []
        videos_analyzed: list[diagramDetails] =[]
        video_created_diagram: list[diagramDetails] = []
        current_stage = "scraping data"

        try:
            if videos_details.platform is not AnalyzeContentPlatform.YOUTUBE:
                current_stage = "uploading videos to gemini"
                gemini_details = await upload_videos_urls_to_gemini(videos_details, videos_details.platform)
                logger.info(
                    "gemini_details: %s",
                    gemini_details,
                )
                current_stage = "analyzing videos"
                for video in gemini_details:
                    if video["shouldSaveDiagram"]:
                        prompt_for_diagram = build_diagram_for_analyze_content_prompt(videos_details.product_name,
                        videos_details.product_description, videos_details.product_target_audience, videos_details.platform, video["videoName"])
                        response = await asyncio.to_thread(analyze_content_save_diagram,video["cloudLink"],prompt_for_diagram,)
                        current_stage = "saving analyzed video diagram"
                        diagram_id = await asyncio.to_thread(self.diagram_service.create_diagram, response.diagram, videos_details.user_id,)
                        videos_analyzed.append(
                            diagramDetails(
                                videoName=video["videoName"],
                                diagramId=diagram_id,
                                prompt=prompt_for_diagram,
                            )
                        )
                        summaries.append(response.summary)
                    else:
                        prompt = analyze_content_prompt(videos_details.platform)
                        current_stage="analyzing video without creating diagram"
                        response = await asyncio.to_thread(analyze_content, video["cloudLink"],prompt)
                        summaries.append(response)
            else:
                current_stage = "analyzing youtube videos"
                for video in videos_details.videos_details:
                    if video.should_save_diagram:
                        prompt_for_diagram = build_diagram_for_analyze_content_prompt(videos_details.product_name,
                        videos_details.product_description, videos_details.product_target_audience, videos_details.platform, video.video_name)
                        current_stage = "analyzing youtube video then save it to diagram"
                        response = await asyncio.to_thread(analyze_content_save_diagram, video.video_url ,prompt_for_diagram)
                        current_stage = "saving analyzed video diagram"
                        diagram_id =await asyncio.to_thread(self.diagram_service.create_diagram,response.diagram,videos_details.user_id,)
                        videos_analyzed.append(
                            diagramDetails(
                                videoName=video.video_name,
                                diagramId=diagram_id,
                                prompt=prompt_for_diagram,
                            )
                        )
                        summaries.append(response.summary)
                    else:
                        current_stage = "analyzing youtube video without creating diagram"
                        prompt = analyze_content_prompt(videos_details.platform)
                        response = await asyncio.to_thread(analyze_content,video.video_url ,prompt )
                        summaries.append(response)

            create_video_prompt = create_diagram_based_on_videos_prompt(summaries, videos_details.sum_of_content, videos_details.product_name,
                videos_details.product_description, videos_details.product_target_audience)
            current_stage = "creating diagrams from videos"
            diagrams = await asyncio.to_thread(create_diagram_based_on_videos, create_video_prompt)
            current_stage = "saving created diagrams"
            for diagram in diagrams:
                video_created_diagram_id = await asyncio.to_thread(self.diagram_service.create_diagram,diagram, videos_details.user_id,)
                video_created_diagram.append(
                    diagramDetails(
                        videoName=diagram.name,
                        diagramId=video_created_diagram_id,
                        prompt=create_video_prompt,
                    )
                )
            response = analyzeContentResponse(
                analyzedVideos=videos_analyzed,
                createdVideos=video_created_diagram,
                message="Videos analyzed successfully",
                orderId=videos_details.order_id,
                status=ScrapingStatus.SUCCEED,
            )
            await rabbitmq_manager.publish(
                response=response,
                routing_key=ORDER_ANALYZE_CONTENT_RESPONSE_ROUTING_KEY,
                exchange=rabbitmq_manager.order_analyze_content_exchange,
            )
        except Exception as e:
            logger.exception("Failed to analyze content video. order_id=%s stage=%s error=%s", videos_details.order_id,
                             current_stage, e)
            response = analyzeContentResponse(
                analyzedVideos=videos_analyzed,
                createdVideos=video_created_diagram,
                message="Failed to analyze content video. Your credits will be refunded, please try again later.",
                orderId=videos_details.order_id,
                status=ScrapingStatus.FAILED,
            )

            await rabbitmq_manager.publish(
                response=response,
                routing_key=ORDER_ANALYZE_CONTENT_RESPONSE_ROUTING_KEY,
                exchange=rabbitmq_manager.order_analyze_content_exchange,
            )




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
                response=response,
                routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
                exchange=rabbitmq_manager.order_analyze_video_exchange
            )
            return

        # 2. המשתמש שלח URL
        if analyze_video_schema.video_url is not None:
            platform = check_url_platform(
                analyze_video_schema.video_url
            )

            if platform == UrlPlatform.NOT_SUPPORT:
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
                    response=scraping_response,
                    routing_key=SCRAPING_ANALYZE_VIDEO_FINISHED_ROUTING_KEY,
                    exchange=rabbitmq_manager.order_analyze_video_exchange
                )
                return

            try:
                if platform == UrlPlatform.TIKTOK:
                    video_details = await asyncio.to_thread(
                        get_tiktok_video_with_url,
                        analyze_video_schema.video_url,
                    )

                elif platform == UrlPlatform.INSTAGRAM:
                    video_details = await asyncio.to_thread(
                        get_instagram_video_with_url,
                        analyze_video_schema.video_url,
                    )

                elif platform == UrlPlatform.FACEBOOK:
                    video_details = await asyncio.to_thread(
                        get_facebook_video_with_url,
                        analyze_video_schema.video_url,
                    )

                elif platform == UrlPlatform.X:
                    video_details = await asyncio.to_thread(
                        get_x_video_with_url,
                        analyze_video_schema.video_url,
                    )

                elif platform == UrlPlatform.YOUTUBE:
                    video_details = {
                        "name": None,
                        "urls": [analyze_video_schema.video_url],
                        "platform": UrlPlatform.YOUTUBE,
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
                    response=scraping_response,
                    routing_key=SCRAPING_ANALYZE_VIDEO_FINISHED_ROUTING_KEY,
                    exchange=rabbitmq_manager.order_analyze_video_exchange
                )
                return

            # scraping הצליח
            scraping_response = ScrapingCompletedResponse(
                order_id=analyze_video_schema.order_id,
                status=ScrapingStatus.SUCCEED,
                message="Video scraping completed",
            )

            await rabbitmq_manager.publish(
                response=scraping_response,
                routing_key=SCRAPING_ANALYZE_VIDEO_FINISHED_ROUTING_KEY,
                exchange=rabbitmq_manager.order_analyze_video_exchange
            )

            prompt = build_video_url_analysis_prompt(
                video_details["platform"],
                video_details["name"],
                analyze_video_schema.business_context,
                analyze_video_schema.business_target_audience,

            )
            gemini_url = video_details["urls"][0]
            try:
                if video_details["platform"] != UrlPlatform.YOUTUBE:
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
                    response=response,
                    routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
                    exchange=rabbitmq_manager.order_analyze_video_exchange
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
                diagram_name=diagram_create.name,
                prompt=prompt,
                video_gemini_url=gemini_url,
            )

            await rabbitmq_manager.publish(
                response=response,
                routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
                exchange=rabbitmq_manager.order_analyze_video_exchange
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
                    response=response,
                    routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
                    exchange=rabbitmq_manager.order_analyze_video_exchange
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
                diagram_name=diagram_create.name,
                prompt=prompt,
                video_gemini_url=analyze_video_schema.video_gemini_url,
            )

            await rabbitmq_manager.publish(
                response=response,
                routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
                exchange=rabbitmq_manager.order_analyze_video_exchange
            )

            return
    @staticmethod
    async def get_video_details( order_id: int) -> AnalyzeContentScrapeResponse:
        analyze_content_cache_key = (
            f"{KEY_PREFIX_SCRAPE_ANALYZE_CONTENT}"
            f":{order_id}"
        )
        cached_video_details = await get_cache(analyze_content_cache_key)
        video_details = [
            ExtractedVideoDetails.model_validate(video)
            for video in json.loads(cached_video_details)
        ]
        response = AnalyzeContentScrapeResponse(
            orderId=order_id,
            status=ScrapingStatus.SUCCEED,
            message="Analyze content scrape video successfully",
            videos=video_details,
        )
        return response


