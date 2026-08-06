from uuid import UUID
from app.Config.ApifyConfig import get_tiktok_video_with_url, get_instagram_video_with_url, get_facebook_video_with_url, get_x_video_with_url
from app.Config.RabitMqConfig import rabbitmq_manager
from app.Routers.LLMRouter import create_diagram
from app.Schemea.AnalyzeVideoSchema import AnalyzeVideoSchema, AnalyzeVideoResponse, AnalyzeVideoStatus, ScrapingCompletedResponse
from app.Schemea.DiagramSchema import DiagramResponse
from app.Config.GeminiConfig import analyze_video_url, analyze_video_file
from app.Util.CheckUrlPlatform import check_url_platform, Platform
from app.Util.PromptBuilder import build_video_url_analysis_prompt, build_video_file_analysis_prompt
from app.Service.DiagramService import DiagramService
import logging
import asyncio

logger = logging.getLogger(__name__)

class LLMService:
    async def analyze_video(self, analyze_video_schema: AnalyzeVideoSchema, user_id: UUID, )->None:
        if analyze_video_schema.video_url is None and analyze_video_schema.video_mp4 is None:
            logger.exception("Video URL or Video MP4 is not provided")
            response = AnalyzeVideoResponse(
                status=AnalyzeVideoStatus.SERVER_FAILED,
                message="Video URL or Video MP4 is required",
                order_id=analyze_video_schema.order_id,
            )
            rabbitmq_manager.publish_message(
                message= response,
                exchange= "video-analysis-exchange",
                routing_key= "analyze_video_routing_key",
            )
            return
        if analyze_video_schema.video_url is not None:
            platform = check_url_platform(analyze_video_schema.video_url)
            if platform == Platform.NOT_SUPPORT:
                logger.exception("Video URL is not supported")
                response = AnalyzeVideoResponse(
                    status=AnalyzeVideoStatus.SERVER_FAILED,
                    message="Video URL is not supported",
                    order_id=analyze_video_schema.order_id,
                )
                rabbitmq_manager.publish_message(
                    message= response,
                    exchange= "video-analysis-exchange",
                )
                return
            try:
                if platform == Platform.TIKTOK:
                    video_details = await asyncio.to_thread(get_tiktok_video_with_url,
                analyze_video_schema.video_url,)
                elif platform == Platform.INSTAGRAM:
                    video_details = await asyncio.to_thread(get_instagram_video_with_url,
                analyze_video_schema.video_url,)
                elif platform == Platform.FACEBOOK:
                    video_details = await asyncio.to_thread(get_facebook_video_with_url,
                analyze_video_schema.video_url,)
                elif platform == Platform.X:
                    video_details = await asyncio.to_thread(get_x_video_with_url,
            analyze_video_schema.video_url, )
                elif platform == Platform.YOUTUBE:
                    video_details = {
                        "name": None,
                        "urls": [analyze_video_schema.video_url],
                        "platform": Platform.YOUTUBE,
                    }
                else:
                    logger.exception("Video URL is not supported")
                    scraping_response = ScrapingCompletedResponse(
                        order_id=analyze_video_schema.order_id,
                        status=ScrapingCompletedResponse.Status.FAILED,
                        message="platform not supported",
                    )
                    rabbitmq_manager.publish_message(
                        message= scraping_response,
                        exchange= "video-analysis-exchange",
                        routing_key= "scraping_finished_routing_key",
                    )
                    return
            except Exception as e:
                logger.exception(f"failed to scrape url the url is {analyze_video_schema.video_url} with error {e} platform is {platform}")
                scraping_response = ScrapingCompletedResponse(
                    order_id=analyze_video_schema.order_id,
                    status=ScrapingCompletedResponse.Status.FAILED,
                    message=f"failed to scrape url the url is {analyze_video_schema.video_url} with error {e} platform is {platform}",
                )
                rabbitmq_manager.publish_message(
                    message= scraping_response,
                    exchange= "video-analysis-exchange",
                    routing_key= "scraping_finished_routing_key",
                )
                return
            response_scraping = ScrapingCompletedResponse(
                order_id=analyze_video_schema.order_id,
                status=ScrapingCompletedResponse.status.SUCCEED,
                message="Video scraping completed"
            )
            rabbitmq_manager.publish_message(
                message= response_scraping,
                exchange= "video-analysis-exchange",
                routing_key= "scraping_finished_routing_key",
            )
            prompt = build_video_url_analysis_prompt(video_details["platform"], video_details["name"])
            try:
                diagram_create  = await asyncio.to_thread(analyze_video_url,video_details["urls"][0],prompt,)
            except Exception as e:
                logger.exception(f"failed to analyze video url the url is {analyze_video_schema.video_url} with error {e}")
                response = AnalyzeVideoResponse(
                    status=AnalyzeVideoStatus.SERVER_FAILED,
                    message="failed to analyze video url try again later check if the video is not private or the url is not valid",
                    order_id=analyze_video_schema.order_id,
                )
                rabbitmq_manager.publish_message(
                    message= response,
                    exchange= "video-analysis-exchange",
                    routing_key= "analyze_video_routing_key",
                )
                return
            await asyncio.to_thread(DiagramService.create_diagram, diagram_create, user_id,)
            diagram_response = DiagramResponse(
                name=diagram_create.name,
                private=True,
                prompt=diagram_create.prompt,
                nodes=diagram_create.nodes,
                arrows=diagram_create.arrows,
            )
            response = AnalyzeVideoResponse(
                status=AnalyzeVideoStatus.SUCCEED,
                message="Video analyzed successfully",
                order_id=analyze_video_schema.order_id,
                diagram=diagram_response,
                diagram_id=diagram_create.id,
            )
            rabbitmq_manager.publish_message(
                message= response,
                exchange= "video-analysis-exchange",
                routing_key= "analyze_video_routing_key",
            )
        elif analyze_video_schema.video_mp4 is not None:
            if analyze_video_schema.video_mp4.filename.endswith(".mp4") is not True:
                logger.exception("Video MP4 is not a valid MP4 file")
                scraping_response = ScrapingCompletedResponse(
                    order_id=analyze_video_schema.order_id,
                    status=ScrapingCompletedResponse.Status.FAILED,
                    message="Video MP4 is not a valid MP4 file",
                )
                rabbitmq_manager.publish_message(
                    message= scraping_response,
                    exchange= "video-analysis-exchange",
                    routing_key= "scraping_finished_routing_key",
                )
                return
            if analyze_video_schema.video_mp4.file.size > 100 * 1024 * 1024:
                logger.exception("Video MP4 is too large")
                scraping_response = ScrapingCompletedResponse(
                    order_id=analyze_video_schema.order_id,
                    status=ScrapingCompletedResponse.Status.FAILED,
                    message="Video MP4 is too large",
                )
                rabbitmq_manager.publish_message(
                    message= scraping_response,
                    exchange= "video-analysis-exchange",
                    routing_key= "scraping_finished_routing_key",
                )
                return
            scraping_response = ScrapingCompletedResponse(
                order_id=analyze_video_schema.order_id,
                status=ScrapingCompletedResponse.status.SUCCEED,
                message="Video scraping completed"
            )
            rabbitmq_manager.publish_message(
                message= scraping_response,
                exchange= "video-analysis-exchange",
                routing_key= "scraping_finished_routing_key",
            )
            prompt = build_video_file_analysis_prompt()
            diagram_create = await asyncio.to_thread(analyze_video_file, analyze_video_schema.video_mp4.file, prompt,)
            diagram_id = await asyncio.to_thread(DiagramService.create_diagram, create_diagram, user_id,)
            diagram_response = DiagramResponse(
                name=diagram_create.name,
                private=True,
                prompt=diagram_create.prompt,
                nodes=diagram_create.nodes,
                arrows=diagram_create.arrows,
            )
            response = AnalyzeVideoResponse(
                status=AnalyzeVideoStatus.SUCCEED,
                message="Video analyzed successfully",
                order_id=analyze_video_schema.order_id,
                diagram=diagram_response,
                diagram_id=diagram_id,
            )
            rabbitmq_manager.publish_message(
                message= response,
                exchange= "video-analysis-exchange",
                routing_key= "analyze_video_routing_key",
            )