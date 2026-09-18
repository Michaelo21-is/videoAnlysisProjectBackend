from uuid import UUID
from enum import Enum
from pydantic import BaseModel, ConfigDict, Field, field_validator
from app.Schemea.AnalyzeVideoSchema import ScrapingStatus


class AnalyzeContentPlatform(str, Enum):
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
    model_config = ConfigDict(populate_by_name=True)

    order_id: int = Field(alias="orderId")
    platform: AnalyzeContentPlatform
    sum_of_content: SumOfContent = Field(alias="sumOfContent")
    niche: str

class CreatorDetails(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    name: str | None = None
    profile_url: str | None = Field(default=None, alias="profileUrl")
    avatar: str | None = None
    followers: int = 0
    is_verified: bool | None = Field(default=None, alias="isVerified")


class ExtractedVideoDetails(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: str | None = None
    caption: str | None = None
    url: str | None = None
    mp4Url: str | None = Field(default=None, alias="mp4Url")

    views: int = 0
    likes: int = 0
    comments: int = 0
    shares: int = 0

    created_at: str | None = Field(default=None, alias="createdAt")
    thumbnail: str | None = None

    creator: CreatorDetails

    # Seconds. Every platform reports duration differently (Apify's YouTube
    # actor returns "HH:MM:SS", TikTok/Instagram a number, X/Facebook nothing),
    # so it is normalized here: the published contract is always a number or
    # null, never a string.
    duration: int | None = None

    @field_validator("duration", mode="before")
    @classmethod
    def _normalize_duration(cls, value: object) -> int | None:
        if value is None or isinstance(value, bool):
            return None

        if isinstance(value, (int, float)):
            return int(value)

        if isinstance(value, str):
            raw = value.strip()

            if not raw:
                return None

            # "HH:MM:SS" / "MM:SS"
            if ":" in raw:
                seconds = 0

                for part in raw.split(":"):
                    try:
                        seconds = seconds * 60 + int(part)
                    except ValueError:
                        return None

                return seconds

            try:
                return int(float(raw))
            except ValueError:
                return None

        return None


class AnalyzeContentScrapeResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    order_id: int = Field(alias="orderId")
    message: str
    status: ScrapingStatus
    videos: list[ExtractedVideoDetails] = []


class videoDetails(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    video_url: str | None = Field(default=None, alias="videoUrl")
    mp4_url: str | None = Field(default=None, alias="mp4Url")
    video_name: str | None = Field(default=None, alias="videoName")
    should_save_diagram: bool = Field(default=False, alias="shouldSaveDiagram")


# Must stay in sync with purchase-service's ChooseVideosAnalyzeContentDto.
class AnalyzeVideoDto(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    user_id: UUID = Field(alias="userId")
    order_id: int = Field(alias="orderId")
    business_context: str | None = Field(default=None, alias="businessContext")
    business_target_audience: str | None = Field(default=None, alias="businessTargetAudience")
    sum_of_content: SumOfContent = Field(alias="sumOfContent")
    videos_details: list[videoDetails] = Field(alias="videos")
    platform: AnalyzeContentPlatform


class diagramDetails(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    video_name: str | None = Field(default=None, alias="videoName")
    diagram_id: str | None = Field(default=None, alias="diagramId")


# Must stay in sync with purchase-service's AnalyzeContentResponseDto.
class analyzeContentResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    analyze_video_details: list[diagramDetails] = Field(default_factory=list, alias="analyzedVideos")
    create_video_details: list[diagramDetails] = Field(default_factory=list, alias="createdVideos")
    order_id: int = Field(alias="orderId")
    status: ScrapingStatus
    message: str | None = None
