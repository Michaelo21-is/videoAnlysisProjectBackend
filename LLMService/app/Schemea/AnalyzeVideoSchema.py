from enum import Enum
from pydantic import BaseModel
from app.Schemea.DiagramSchema import DiagramResponse
from uuid import UUID
from fastapi import UploadFile, File

class AnalyzeVideoStatus(str, Enum):
    PENDING = "PENDING"
    FAILED_TO_DOWNLOAD_VIDEO = "FAILED_TO_DOWNLOAD_VIDEO"
    FAILED_TO_ANALYZE_VIDEO = "FAILED_TO_ANALYZE_VIDEO"
    SERVER_FAILED = "SERVER_FAILED"
    SUCCEED = "SUCCEED"


class AnalyzeVideoSchema(BaseModel):
    order_id: int
    video_url: str | None = None
    file: UploadFile = File(...)

class AnalyzeVideoResponse(BaseModel):
    diagram: DiagramResponse | None = None
    order_id: int
    user_id: UUID
    message: str
    status: AnalyzeVideoStatus