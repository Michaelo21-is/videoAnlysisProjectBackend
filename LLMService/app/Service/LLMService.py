from app.Config.ApifyConfig import get_tiktok_video_with_url, get_instagram_video_with_url, get_facebook_video_with_url, get_x_video_with_url
from app.Config.RabitMqConfig import rabbitmq_manager, ANALYZE_VIDEO_RESPONSE_ROUTING_KEY, SCRAPING_FINISHED_ROUTING_KEY
from app.Schemea.AnalyzeVideoSchema import AnalyzeVideoSchema, AnalyzeVideoResponse, AnalyzeVideoStatus, \
    ScrapingCompletedResponse, ScrapingStatus
from app.Config.GeminiConfig import analyze_video_url, upload_video_url_to_gemini
from app.Util.CheckUrlPlatform import check_url_platform, Platform
from app.Util.PromptBuilder import build_video_url_analysis_prompt, build_video_file_analysis_prompt
from app.Service.DiagramService import DiagramService
import logging
import asyncio

logger = logging.getLogger(__name__)

class LLMService:
    def __init__(self):
        self.diagram_service = DiagramService()
    async def analyze_video( self, analyze_video_schema: AnalyzeVideoSchema) -> None:
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
                    routing_key=SCRAPING_FINISHED_ROUTING_KEY,
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
                    routing_key=SCRAPING_FINISHED_ROUTING_KEY,
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
                routing_key=SCRAPING_FINISHED_ROUTING_KEY,
            )

            prompt = build_video_url_analysis_prompt(
                video_details["platform"],
                video_details["name"],
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
            prompt = build_video_file_analysis_prompt()

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