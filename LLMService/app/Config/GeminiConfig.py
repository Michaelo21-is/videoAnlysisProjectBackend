import os

from google import genai
from pydantic import ValidationError
import shutil
import tempfile
import time
from urllib.request import urlopen

from app.Schemea.DiagramSchema import DiagramCreate

client = genai.Client()


def analyze_video_url( video_url: str, prompt: str,) -> DiagramCreate:
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
def upload_video_url_to_gemini(video_url: str):
    temp_path = None

    try:
        with tempfile.NamedTemporaryFile(
            delete=False,
            suffix=".mp4",
        ) as temp_file:
            temp_path = temp_file.name

            with urlopen(video_url) as response:
                shutil.copyfileobj(response, temp_file)

        gemini_file = client.files.upload(
            file=temp_path,
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

    finally:
        if temp_path:
            try:
                os.remove(temp_path)
            except FileNotFoundError:
                pass
