from google import genai

client = genai.Client()

def analyze_video(video_url: str, prompt: str) -> str:
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

    return interaction.output_text