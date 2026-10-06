from enum import Enum
from pydantic import BaseModel, Field
from uuid import UUID

class AnalyzeVideoStatus(str, Enum):
    PENDING = "PENDING"
    FAILED_TO_DOWNLOAD_VIDEO = "FAILED_TO_DOWNLOAD_VIDEO"
    FAILED_TO_ANALYZE_VIDEO = "FAILED_TO_ANALYZE_VIDEO"
    SERVER_FAILED = "SERVER_FAILED"
    SUCCEED = "SUCCEED"
class ScrapingStatus(str, Enum):
    SUCCEED = "SUCCEED"
    FAILED = "FAILED"


class ScrapingCompletedResponse(BaseModel):
    order_id: int
    message: str
    status: ScrapingStatus

class AnalyzeVideoSchema(BaseModel):
    order_id: int = Field(alias="orderId")
    user_id: UUID = Field(alias="userId")
    video_url: str | None = Field(
        default=None,
        alias="videoUrl",
    )
    video_gemini_url: str | None = Field(
        default=None,
        alias="videoGeminiUrl",
    )
    business_context : str | None = Field(
        default=None,
        alias="businessContext",
    )
    business_target_audience : str | None = Field(
        default=None,
        alias="businessTargetAudience",
    )

class AnalyzeVideoResponse(BaseModel):
    diagram_id: str | None = None
    diagram_name: str | None = None
    order_id: int
    message: str
    status: AnalyzeVideoStatus
    video_gemini_url: str | None = None
    prompt: str | None = None

class scraping_complete_response(BaseModel):
    order_id: int
    message: str
    status: ScrapingStatus
