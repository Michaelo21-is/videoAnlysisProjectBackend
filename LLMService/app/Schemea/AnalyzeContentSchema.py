from enum import Enum
from pydantic import BaseModel, Field
from app.Schemea.AnalyzeVideoSchema import ScrapingStatus

class Platform(str, Enum):
    YOUTUBE = "YOUTUBE"
    TIKTOK = "TIKTOK"
    FACEBOOK = "FACEBOOK"
    INSTAGRAM = "INSTAGRAM"
    X="X"

class SumOfContent(str, Enum):
    THREE="THREE"
    FIVE="FIVE"
    SEVEN="SEVEN"

class AnalyzeContentScrape(BaseModel):
    order_id: int = Field(alias="orderId")
    platform: Platform = Field(alias="platform")
    sum_of_content: SumOfContent = Field(alias="sumOfContent")
    niche:str = Field(alias="niche")

class ScrapingCompletedResponse(BaseModel):
    order_id: int
    message: str
    status: ScrapingStatus