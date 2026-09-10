import asyncio
import os
from app.Util.CheckUrlPlatform import UrlPlatform

from google import genai
from pydantic import ValidationError
import shutil
import tempfile
import subprocess
import time
from urllib.request import urlopen, Request

from app.Schemea.DiagramSchema import DiagramCreate
from app.Schemea.AnalyzeContentSchema import AnalyzeVideoDto

client = genai.Client()


def analyze_video_url( video_url: str, prompt: str, ) -> DiagramCreate:
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
        store=False,
    )

    output_text = interaction.output_text

    if not isinstance(output_text, str) or not output_text.strip():
        raise ValueError("Gemini returned an empty response")

    try:
        return DiagramCreate.model_validate_json(output_text)
    except ValidationError as error:
        raise ValueError(
            "Gemini response does not match DiagramCreate"
        ) from error
def upload_video_url_to_gemini(
    video_url: str,
    audio_url: str | None = None,
    platform: UrlPlatform | None = None,
):
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
async def upload_videos_urls_to_gemini(video_details: AnalyzeVideoDto) -> list[dict[str, str | bool]]:

    gemini_files = []

    for video in video_details.videos_details:
        gemini_url = await asyncio.to_thread(
            upload_video_url_to_gemini,
            video.mp4_link,
            None,
            None,
        )

        gemini_files.append({
            "videoLink": video.video_url,
            "cloudLink": gemini_url,
            "shouldSaveDiagram": video.shouldSaveDiagram,
            "videoName": video.video_name,
        })

    return gemini_files
