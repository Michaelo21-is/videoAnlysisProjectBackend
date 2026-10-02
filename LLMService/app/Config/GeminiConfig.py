import asyncio
import os
from app.Util.CheckUrlPlatform import UrlPlatform
from google import genai
from pydantic import BaseModel, ValidationError
import shutil
import tempfile
import subprocess
import time
from urllib.request import urlopen, Request

from app.Schemea.DiagramSchema import DiagramCreate, DiagramCreateListResponse
from app.Schemea.AnalyzeContentSchema import (AnalyzeVideoDto, AnalyzeContentPlatform,
    AnalyzeContentDiagramResponse, AnalyzeContentSummaryResponse)

client = genai.Client()


# Pydantic leaves a field out of "required" whenever it has a default, so
# DiagramCreate.nodes/arrows/prompt (default_factory=list, "") come out
# optional. As a validation contract that is right; as a generation contract
# it is not - it tells Gemini those fields may be skipped, and the model then
# returns a diagram with no nodes and no arrows that still validates, because
# the defaults fill the gap. Structured output has to demand every documented
# field, so mark the whole schema required.
#
# This is only safe while none of the response models have a genuinely
# optional (`| None`) field; add one and it must be exempted here.
def _require_all_properties(schema: dict) -> dict:
    if "properties" in schema:
        schema["required"] = list(schema["properties"])

    for nested in schema.get("properties", {}).values():
        _require_all_properties(nested)

    for nested in schema.get("$defs", {}).values():
        _require_all_properties(nested)

    items = schema.get("items")
    if isinstance(items, dict):
        _require_all_properties(items)

    return schema


# The prompts describe the expected JSON, but describing it is only a request:
# Gemini occasionally drops a field (a diagram arrow without "targetNodeId",
# for example) and the response then fails Pydantic validation. Sending the
# model's own JSON schema as the structured-output format makes the API
# constrain decoding instead, so required fields cannot go missing.
def _json_response_format(response_model: type[BaseModel]) -> dict:
    return {
        "type": "text",
        "mime_type": "application/json",
        "schema": _require_all_properties(response_model.model_json_schema()),
    }

def _send_video_to_gemini(video_url: str, prompt: str, response_model: type[BaseModel]) -> str:
    interaction = client.interactions.create(
        model="gemini-3.6-flash",
        input=[
            {
                "type": "video",
                "uri": video_url,
                "mime_type": "video/mp4",
            },
            {
                "type": "text",
                "text": prompt,
            },
        ],
        response_format=_json_response_format(response_model),
        store=False,
    )

    output_text = interaction.output_text

    if not isinstance(output_text, str) or not output_text.strip():
        raise ValueError("Gemini returned an empty response")

    return output_text

def analyze_video_url( video_url: str, prompt: str, ) -> DiagramCreate:
    output_text = _send_video_to_gemini(video_url, prompt, DiagramCreate)

    if not isinstance(output_text, str) or not output_text.strip():
        raise ValueError("Gemini returned an empty response")

    try:
        return DiagramCreate.model_validate_json(output_text)
    except ValidationError as error:
        raise ValueError(
            "Gemini response does not match DiagramCreate"
        ) from error
def analyze_content_save_diagram(video_url: str, prompt: str) -> AnalyzeContentDiagramResponse:
    output_text = _send_video_to_gemini(video_url, prompt, AnalyzeContentDiagramResponse)
    if not isinstance(output_text, str) or not output_text.strip():
        raise ValueError("Gemini returned an empty response")
    try:
        return AnalyzeContentDiagramResponse.model_validate_json(output_text)
    except ValidationError as error:
        raise ValueError(
            "Gemini response does not match AnalyzeContentDiagramResponse"
        ) from error
def analyze_content(video_url: str, prompt: str) -> str:
    output_text = _send_video_to_gemini(video_url, prompt, AnalyzeContentSummaryResponse)
    if not isinstance(output_text, str) or not output_text.strip():
        raise ValueError("Gemini returned an empty response")
    try:
        return AnalyzeContentSummaryResponse.model_validate_json(output_text).summary
    except ValidationError as error:
        raise ValueError(
            "Gemini response does not match AnalyzeContentSummaryResponse"
        ) from error


def create_diagram_based_on_videos(prompt: str, ) -> list[DiagramCreate]:

    interaction = client.interactions.create(
        model="gemini-3.6-flash",
        input=[
            {
                "type": "text",
                "text": prompt,
            }
        ],
        response_format=_json_response_format(DiagramCreateListResponse),
        store=False,
    )

    output_text = interaction.output_text

    if not isinstance(output_text, str) or not output_text.strip():
        raise ValueError("Gemini returned an empty response")

    try:
        return DiagramCreateListResponse.model_validate_json(output_text).diagrams

    except ValidationError as error:
        raise ValueError("Gemini response does not match diagrams response") from error

def upload_video_url_to_gemini(video_url: str, audio_url: str | None = None,platform: UrlPlatform | None = None,):
    video_path = None
    audio_path = None
    merged_path = None

    try:
        # 1. Download video
        with tempfile.NamedTemporaryFile(
            delete=False,
            suffix=".mp4",
        ) as video_file:
            video_path = video_file.name

            if platform == UrlPlatform.TIKTOK:
                request = Request(
                    video_url,
                    headers={
                        "Authorization": (
                            f"Bearer {os.environ['APIFY_API']}"
                        )
                    },
                )

                with urlopen(request) as response:
                    shutil.copyfileobj(
                        response,
                        video_file,
                    )

            else:
                with urlopen(video_url) as response:
                    shutil.copyfileobj(
                        response,
                        video_file,
                    )

        file_to_upload = video_path

        # 2. Separate audio stream
        if audio_url:
            with tempfile.NamedTemporaryFile(
                delete=False,
                suffix=".mp4",
            ) as audio_file:
                audio_path = audio_file.name

                with urlopen(audio_url) as response:
                    shutil.copyfileobj(
                        response,
                        audio_file,
                    )

            with tempfile.NamedTemporaryFile(
                delete=False,
                suffix=".mp4",
            ) as merged_file:
                merged_path = merged_file.name

            subprocess.run(
                [
                    "ffmpeg",
                    "-y",
                    "-i", video_path,
                    "-i", audio_path,
                    "-map", "0:v:0",
                    "-map", "1:a:0",
                    "-c:v", "copy",
                    "-c:a", "aac",
                    "-movflags", "+faststart",
                    merged_path,
                ],
                check=True,
                capture_output=True,
                text=True,
            )

            file_to_upload = merged_path

        # 3. Upload to Gemini Files
        gemini_file = client.files.upload(
            file=file_to_upload,
        )

        while (
            not gemini_file.state
            or gemini_file.state.name == "PROCESSING"
        ):
            time.sleep(2)

            gemini_file = client.files.get(
                name=gemini_file.name
            )

        if gemini_file.state.name == "FAILED":
            raise ValueError(
                "Gemini failed to process uploaded video"
            )

        return gemini_file.uri

    except subprocess.CalledProcessError as error:
        raise RuntimeError(
            f"Failed to merge Instagram video and audio: "
            f"{error.stderr}"
        ) from error

    finally:
        for path in (
            video_path,
            audio_path,
            merged_path,
        ):
            if path:
                try:
                    os.remove(path)
                except FileNotFoundError:
                    pass
async def upload_videos_urls_to_gemini(video_details: AnalyzeVideoDto, platform: AnalyzeContentPlatform) -> list[dict[str, str | bool]]:

    gemini_files = []
    url_platform = UrlPlatform(platform.value)
    for video in video_details.videos_details:
        if not video.mp4_url:
            raise ValueError(
                f"Missing mp4Url for video. "
                f"videoUrl={video.video_url}, "
                f"videoName={video.video_name}, "
                f"platform={platform.value}"
            )
        gemini_url = await asyncio.to_thread(
            upload_video_url_to_gemini,
            video.mp4_url,
            None,
            platform=url_platform,
        )

        gemini_files.append({
            "videoLink": video.video_url,
            "cloudLink": gemini_url,
            "shouldSaveDiagram": video.should_save_diagram,
            "videoName": video.video_name,
        })

    return gemini_files
