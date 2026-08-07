from google import genai
from pydantic import ValidationError

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

