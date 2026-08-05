from uuid import UUID
from app.Config.ApifyConfig import get_tiktok_video_with_url, get_instagram_video_with_url, get_facebook_video_with_url, get_x_video_with_url
from app.Config.RabitMqConfig import rabbitmq_manager
from app.Schemea.AnalyzeVideoSchema import AnalyzeVideoSchema, AnalyzeVideoResponse, AnalyzeVideoStatus
from app.Config.GeminiConfig import analyze_video
from app.Util.CheckUrlPlatform import check_url_platform, Platform
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
                user_id=user_id,
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
                    user_id=user_id,
                )
                rabbitmq_manager.publish_message(
                    message= response,
                    exchange= "video-analysis-exchange",
                )
                return

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
                }
            else:
                logger.exception("Video URL is not supported")
                response = AnalyzeVideoResponse(
                    status=AnalyzeVideoStatus.SERVER_FAILED,
                    message="Video URL is not supported",
                    order_id=analyze_video_schema.order_id,
                    user_id=user_id,
                )
                rabbitmq_manager.publish_message(
                    message= response,
                    exchange= "video-analysis-exchange",
                )
                return
            prompt = "not a true prompt"
            diagram = analyze_video(video_details["url"], prompt)
            response = AnalyzeVideoResponse(
                status=AnalyzeVideoStatus.SUCCEED,
                message="Video analyzed successfully",
                order_id=analyze_video_schema.order_id,
                user_id=user_id,
                diagram=diagram,
            )
            rabbitmq_manager.publish_message(
                message= response,
                exchange= "video-analysis-exchange",
            )
        if analyze_video_schema.video_mp4 is not None:
            if analyze_video_schema.video_mp4.filename.endswith(".mp4") is not True:
                logger.exception("Video MP4 is not a valid MP4 file")
                response = AnalyzeVideoResponse(
                    status=AnalyzeVideoStatus.SERVER_FAILED,
                    message="Video MP4 is not a valid MP4 file",
                    order_id=analyze_video_schema.order_id,
                    user_id=user_id,
                )
                rabbitmq_manager.publish_message(
                    message= response,
                    exchange= "video-analysis-exchange",
                    routing_key= "analyze_video_routing_key",
                )
                return
            if analyze_video_schema.video_mp4.file.size > 100 * 1024 * 1024:
                logger.exception("Video MP4 is too large")
                response = AnalyzeVideoResponse(
                    status=AnalyzeVideoStatus.SERVER_FAILED,
                    message="Video MP4 is too large should be less than 100MB",
                    order_id=analyze_video_schema.order_id,
                    user_id=user_id,
                )
                rabbitmq_manager.publish_message(
                    message= response,
                    exchange= "video-analysis-exchange",
                    routing_key= "analyze_video_routing_key",
                )
                return
            diagram = analyze_video()
            response = AnalyzeVideoResponse(
                status=AnalyzeVideoStatus.SUCCEED,
                message="Video analyzed successfully",
                order_id=analyze_video_schema.order_id,
                user_id=user_id,
                diagram=diagram,
            )
            rabbitmq_manager.publish_message(
                message= response,
                exchange= "video-analysis-exchange",
                routing_key= "analyze_video_routing_key",
            )