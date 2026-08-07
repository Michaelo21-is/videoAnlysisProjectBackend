from enum import Enum
from pydantic import BaseModel, Field
from app.Schemea.DiagramSchema import DiagramResponse

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
    video_url: str | None = Field(
        default=None,
        alias="videoUrl",
    )
    video_s3_url: str | None = Field(
        default=None,
        alias="videoS3Url",
    )

class AnalyzeVideoResponse(BaseModel):
    diagram: DiagramResponse | None = None
    diagram_id: str | None = None
    order_id: int
    message: str
    status: AnalyzeVideoStatus
class ScrapingCompletedResponse(BaseModel):
    order_id: int
    message: str
    status: ScrapingStatus
