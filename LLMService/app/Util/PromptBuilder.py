from textwrap import dedent

from app.Util.CheckUrlPlatform import Platform


def build_video_url_analysis_prompt(platform: Platform, video_name: str | None = None, ) -> str:
    known_video_name = (
        video_name.strip()
        if isinstance(video_name, str) and video_name.strip()
        else "Untitled video"
    )

    if platform == Platform.YOUTUBE:
        title_context = """
        TITLE RULES:
        - The provided video is a YouTube video.
        - Extract the actual title of the provided YouTube video.
        - Return the extracted YouTube title in the top-level "name" field.
        - Do not use the YouTube URL as the name.
        - Do not use the channel name as the video name.
        - Do not generate a generic or descriptive title when the actual
          YouTube title is available.
        - If the actual title cannot be reliably determined, return
          "Untitled YouTube video".
        - Never invent a title.
        """
    else:
        title_context = f"""
        TITLE RULES:
        - The video name was already extracted from the source platform.
        - Return exactly the following value in the top-level "name" field:
          "{known_video_name}"
        - Do not extract another name from the video.
        - Do not rewrite, summarize, translate, shorten, or replace the
          provided video name.
        - Do not use the video URL as the name.
        """

    return dedent(
        f"""
        You are an expert short-form and long-form video analyst.

        Analyze the entire provided video and convert its structure into a
        diagram that matches the DiagramCreate JSON schema described below.

        Source platform: {platform.value}

        {title_context}

        ANALYSIS GOAL:
        Create a chronological diagram that explains how the video is
        structured, why each section exists, and how the sections connect
        to one another.

        The diagram may contain only these node types:
        1. "hook"
        2. "videoPart"
        3. "cta"

        NODE RULES:
        - Create one "hook" node for the opening section that captures
          attention.
        - Split the body into multiple "videoPart" nodes whenever there is a
          meaningful change in topic, scene, argument, demonstration,
          story beat, or purpose.
        - Create a "cta" node when the video contains an explicit or implicit
          call to action.
        - If there is no call to action, create one "cta" node whose text
          clearly says that no CTA was detected.
        - Do not invent a CTA that does not exist.
        - Keep the nodes in the same chronological order as the video.
        - Do not combine unrelated sections into one node.
        - Do not create unnecessary nodes for minor visual changes that do
          not affect the meaning or structure.

        Every node's "text" must clearly describe all important detected
        details:

        - Section title
        - Approximate start and end timestamps
        - What happens in this section
        - Spoken message, narration, or dialogue
        - Important visual content or actions
        - Important on-screen text or captions
        - Editing style, cuts, transitions, pacing, or camera movement
        - Music, sound effects, silence, or important audio changes
        - The purpose of the section
        - Why the section is important to the video's effectiveness

        HOOK ANALYSIS:
        For the hook, also identify:
        - The hook technique
        - The curiosity gap, promise, question, surprise, problem, or visual
          pattern used to gain attention
        - Why a viewer may continue watching
        - Whether the hook is visual, verbal, textual, audio-based, or a
          combination

        VIDEO PART ANALYSIS:
        For every videoPart, also identify:
        - Its role in the story or explanation
        - The information or emotional value it provides
        - How it maintains attention
        - How it leads into the next section

        CTA ANALYSIS:
        For the CTA, identify:
        - The requested viewer action
        - Whether the CTA is spoken, visual, written, or implied
        - Its placement and timing
        - How it relates to the rest of the video

        CONNECTION RULES:
        - Connect every node chronologically using arrows.
        - Every arrow must use the exact ID of its source and target nodes.
        - The "text" of every arrow must briefly explain the transition or
          relationship.
        - Do not create arrows that point to missing nodes.
        - Do not leave nodes disconnected.
        - The first node should normally be the hook.
        - The final node should normally be the CTA when one exists.

        GENERATED PROMPT RULES:
        The top-level "prompt" field must contain a detailed, standalone
        prompt for creating a new original video inspired by the analyzed
        video's successful structure.

        The generated prompt must include:
        - The main content goal
        - Intended target audience
        - Recommended hook approach
        - Chronological scene or section plan
        - Key messages to communicate
        - Visual direction
        - Speaking or narration style
        - Editing pace and transition style
        - On-screen text and caption approach
        - Audio or music direction
        - Recommended CTA
        - Important structural patterns found in the source video

        The generated prompt must preserve useful structures and techniques
        without copying exact sentences, protected characters, branding, or
        the creator's identity from the source video.

        LAYOUT RULES:
        - Generate a unique UUID string for every node and arrow.
        - Arrange nodes from left to right in chronological order.
        - Use approximately 360 pixels of horizontal spacing between nodes.
        - Use a width of 320 and a height of at least 220 for each node.
        - Increase the height when the text requires more space.
        - Use y = 100 for the first row.
        - When there are more than four nodes, continue on another row.
        - Use zIndex = 1 for every node.

        Return exactly one valid JSON object with this structure:

        {{
          "name": "Video title",
          "prompt": "Detailed standalone prompt for creating a new video",
          "private": true,
          "nodes": [
            {{
              "id": "unique UUID",
              "type": "hook | videoPart | cta",
              "text": "Complete section analysis",
              "x": 80,
              "y": 100,
              "width": 320,
              "height": 220,
              "zIndex": 1
            }}
          ],
          "arrows": [
            {{
              "id": "unique UUID",
              "sourceNodeId": "existing source node UUID",
              "targetNodeId": "existing target node UUID",
              "text": "Short explanation of the transition"
            }}
          ]
        }}

        OUTPUT REQUIREMENTS:
        - Return JSON only.
        - Do not return Markdown.
        - Do not wrap the JSON in a code block.
        - Do not include explanations before or after the JSON.
        - Use only the documented fields.
        - Use only valid node types.
        - Ensure every required field is present.
        - Ensure the result can be validated directly as DiagramCreate.
        """
    ).strip()

def build_video_file_analysis_prompt() -> str:
    return dedent(
        """
        You are an expert short-form and long-form video analyst.

        The user uploaded an MP4 video video.

        Analyze the entire uploaded video and convert its structure into a
        diagram that matches the DiagramCreate JSON schema described below.

        TITLE RULES:
        - Determine the video's title from its actual content.
        - If a reliable title appears visually or is clearly spoken, use it
          in the top-level "name" field.
        - Otherwise, generate a short and accurate descriptive title based
          only on the uploaded video's content.
        - Do not invent unrelated details.
        - Do not use a generic value such as "Uploaded video" unless no
          meaningful title can be determined.

        ANALYSIS GOAL:
        Create a chronological diagram that explains how the video is
        structured, why each section exists, and how the sections connect
        to one another.

        The diagram may contain only these node types:
        1. "hook"
        2. "videoPart"
        3. "cta"

        NODE RULES:
        - Create one "hook" node for the opening section that captures
          attention.
        - Split the body into multiple "videoPart" nodes whenever there is a
          meaningful change in topic, scene, argument, demonstration,
          story beat, or purpose.
        - Create a "cta" node when the video contains an explicit or implicit
          call to action.
        - If there is no call to action, create one "cta" node whose text
          clearly states that no CTA was detected.
        - Do not invent a CTA that does not exist.
        - Keep all nodes in the same chronological order as the video.
        - Do not combine unrelated sections into one node.
        - Do not create unnecessary nodes for minor visual changes.

        Every node's "text" must include:

        - A short section title
        - Approximate start and end timestamps
        - What happens in the section
        - Spoken dialogue, narration, or message
        - Important visuals and actions
        - Important on-screen text or captions
        - Editing style, transitions, pacing, and camera movement
        - Music, sound effects, silence, or important audio changes
        - The purpose of the section
        - Why the section is important to the video's effectiveness

        HOOK ANALYSIS:
        For the hook, also identify:
        - The hook technique
        - The curiosity gap, promise, question, surprise, or problem
        - Why a viewer may continue watching
        - Whether the hook is visual, verbal, textual, audio-based, or a
          combination

        VIDEO PART ANALYSIS:
        For every "videoPart", also identify:
        - Its role in the story or explanation
        - The information or emotional value it provides
        - How it maintains the viewer's attention
        - How it leads into the next section

        CTA ANALYSIS:
        For the CTA, identify:
        - The requested viewer action
        - Whether it is spoken, visual, written, or implied
        - Its placement and timing
        - How it relates to the rest of the video

        CONNECTION RULES:
        - Connect every node chronologically using arrows.
        - Every arrow must use the exact ID of its source and target nodes.
        - Every arrow's "text" must briefly explain the transition.
        - Do not create arrows that point to missing nodes.
        - Do not leave nodes disconnected.
        - The first node should normally be the hook.
        - The final node should normally be the CTA.

        GENERATED PROMPT RULES:
        The top-level "prompt" field must contain a detailed, standalone
        prompt for creating a new original video inspired by the analyzed
        video's successful structure.

        The generated prompt must include:
        - The main content goal
        - The intended target audience
        - The recommended hook approach
        - A chronological scene or section plan
        - Key messages
        - Visual direction
        - Speaking or narration style
        - Editing pace and transitions
        - On-screen text and captions
        - Audio or music direction
        - A recommended CTA
        - Important structural patterns found in the uploaded video

        Preserve useful structures and techniques without copying exact
        sentences, protected characters, branding, or the original
        creator's identity.

        LAYOUT RULES:
        - Generate a unique UUID string for every node and arrow.
        - Arrange nodes from left to right in chronological order.
        - Use approximately 360 pixels of horizontal spacing.
        - Use a width of 320 and a height of at least 220 for every node.
        - Increase the height when the text requires more space.
        - Use y = 100 for the first row.
        - When there are more than four nodes, continue on another row.
        - Use zIndex = 1 for every node.

        Return exactly one valid JSON object with this structure:

        {
          "name": "Video title",
          "prompt": "Detailed standalone prompt for creating a new video",
          "private": true,
          "nodes": [
            {
              "id": "unique UUID",
              "type": "hook | videoPart | cta",
              "text": "Complete section analysis",
              "x": 80,
              "y": 100,
              "width": 320,
              "height": 220,
              "zIndex": 1
            }
          ],
          "arrows": [
            {
              "id": "unique UUID",
              "sourceNodeId": "existing source node UUID",
              "targetNodeId": "existing target node UUID",
              "text": "Short explanation of the transition"
            }
          ]
        }

        OUTPUT REQUIREMENTS:
        - Return JSON only.
        - Do not return Markdown.
        - Do not wrap the JSON in a code block.
        - Do not include explanations before or after the JSON.
        - Use only the documented fields.
        - Use only valid node types.
        - Ensure every required field is present.
        - Ensure the result can be validated directly as DiagramCreate.
        """
    ).strip()