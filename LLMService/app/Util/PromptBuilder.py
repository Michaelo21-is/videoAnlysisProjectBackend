from textwrap import dedent

from app.Util.CheckUrlPlatform import Platform


def build_video_url_analysis_prompt(
    platform: Platform,
    video_name: str | None = None,
) -> str:
    known_video_name = (
        video_name.strip()
        if isinstance(video_name, str) and video_name.strip()
        else "Untitled video"
    )

    if platform == Platform.YOUTUBE:
        title_context = """
        TITLE RULES:
        - The provided video is a YouTube video.
        - Extract the actual YouTube video title.
        - Return it in the top-level "name" field.
        - Do not use the URL or channel name as the title.
        - Do not generate a replacement title when the real title is available.
        - If the title cannot be reliably determined, return
          "Untitled YouTube video".
        - Never invent a title.
        """
    else:
        title_context = f"""
        TITLE RULES:
        - The video name was already extracted from the source platform.
        - Return exactly this value in the top-level "name" field:
          "{known_video_name}"
        - Do not rewrite, shorten, translate, summarize, or replace it.
        - Do not use the video URL as the name.
        """

    return dedent(
        f"""
        You are an expert video structure analyst.

        Analyze the entire provided video and convert its structure into a
        concise chronological storyboard matching the DiagramCreate JSON schema.

        Source platform: {platform.value}

        {title_context}

        ANALYSIS GOAL:
        Break the video into only the meaningful sections a person needs to
        quickly understand how the video is structured.

        The diagram may contain only these node types:
        1. "hook"
        2. "videoPart"
        3. "cta"

        NODE CREATION RULES:
        - Start with one "hook" node representing the opening that captures
          attention.
        - Create a new "videoPart" only when there is a meaningful change in
          topic, scene, argument, demonstration, story beat, or purpose.
        - Do not create nodes for small cuts, camera changes, captions, or
          minor visual changes.
        - Create a "cta" node when an explicit or implicit CTA exists.
        - If no CTA exists, create one final "cta" node stating briefly that
          no CTA was detected.
        - Never invent events, dialogue, or a CTA.
        - Keep all nodes in chronological order.
        - Prefer fewer useful nodes over many detailed nodes.

        NODE TEXT RULES:
        Node text must be concise and easy to scan.

        Each node should normally contain only:
        - Approximate timestamp range.
        - A short description of what happens.
        - One notable detail ONLY when something especially important,
          unusual, attention-grabbing, or structurally significant happens.

        Recommended format:
        "00:00-00:04 | Opens with a surprising result before explaining it."

        When something notable happens:
        "00:04-00:10 | Demonstrates the product. Notable: fast before/after reveal."

        Keep each node focused on the main beat.
        Aim for roughly 10-30 words per node when possible.


        Mention one of those only when it is important to understanding why
        that specific section stands out.

        HOOK:
        - Briefly describe what happens in the opening.
        - Mention the hook technique only if it is useful or distinctive.
        - Do not add a long explanation of why the hook works.

        VIDEO PART:
        - Describe the main event, information, demonstration, or story beat.
        - Split into another node only when the video's purpose meaningfully changes.

        CTA:
        - State the action requested from the viewer.
        - Keep it short.
        - If no CTA exists, use wording such as:
          "00:42-00:45 | No CTA detected."

        CONNECTION RULES:
        - Connect every node chronologically.
        - Use the exact source and target node IDs.
        - Do not leave nodes disconnected.
        - Do not create arrows to missing nodes.
        - Arrow text must contain MAXIMUM 3 WORDS.
        - Prefer simple transition labels such as:
          "Builds tension"
          "Shows result"
          "Explains why"
          "Then demonstrates"
          "Leads to CTA"
          "Adds proof"
        - Never write a sentence inside an arrow.

        GENERATED PROMPT RULES:
        The top-level "prompt" field must contain a standalone prompt for
        creating a new original video inspired by the analyzed structure.

        It should describe:
        - The main content goal.
        - Target audience.
        - Hook approach.
        - Chronological scene structure.
        - Key messages.
        - Visual direction.
        - Narration or speaking style.
        - Editing pace.
        - On-screen text approach.
        - Audio direction.
        - Recommended CTA.
        - Important structural patterns found in the source video.

        Preserve useful structures and techniques, but do not copy exact
        sentences, branding, protected characters, or the creator's identity.

        LAYOUT RULES:
        The diagram should look like a left-to-right storyboard.

        Use these approximate positions as the visual pattern:

        Node 1:
        x = 90
        y = 110

        Node 2:
        x = 430
        y = 300

        Node 3:
        x = 770
        y = 110

        Node 4:
        x = 1110
        y = 300

        For additional nodes:
        - Continue from left to right.
        - Add approximately 340 to x for every new node.
        - Alternate y between approximately 110 and 300.
        - Do not stack nodes on top of each other.
        - Keep enough empty space between nodes for arrows and arrow labels.
        - Do not force the diagram into only four nodes.
        - The number of nodes must depend on the actual video structure.

        Example continuation:
        Node 5: x = 1450, y = 110
        Node 6: x = 1790, y = 300
        Node 7: x = 2130, y = 110

        For every node:
        - width = 240
        - height = 180
        - zIndex = 1

        Do not increase node height just because more analysis is available.
        Keep the text concise enough to fit the note.

        Generate a unique UUID string for every node and arrow.

        Return exactly one valid JSON object with this structure:

        {{
          "name": "Video title",
          "prompt": "Standalone prompt for creating a new original video",
          "private": true,
          "nodes": [
            {{
              "id": "unique UUID",
              "type": "hook | videoPart | cta",
              "text": "00:00-00:04 | Short description of what happens.",
              "x": 90,
              "y": 110,
              "width": 240,
              "height": 180,
              "zIndex": 1
            }}
          ],
          "arrows": [
            {{
              "id": "unique UUID",
              "sourceNodeId": "existing source node UUID",
              "targetNodeId": "existing target node UUID",
              "text": "Builds tension"
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
        - Ensure every arrow text contains no more than 3 words.
        - Ensure node text stays concise.
        - Ensure the result can be validated directly as DiagramCreate.
        """
    ).strip()


def build_video_file_analysis_prompt() -> str:
    return dedent(
        """
        You are an expert video structure analyst.

        The user uploaded an MP4 video.

        Analyze the entire uploaded video and convert its structure into a
        concise chronological storyboard matching the DiagramCreate JSON schema.

        TITLE RULES:
        - Determine the video's title from its actual content.
        - If a reliable title appears visually or is clearly spoken, use it
          in the top-level "name" field.
        - Otherwise, create a short descriptive title based only on the
          video's actual content.
        - Do not invent unrelated details.
        - Avoid generic titles such as "Uploaded video" when a meaningful
          descriptive title can be created.

        ANALYSIS GOAL:
        Break the video into only the meaningful sections a person needs to
        quickly understand how the video is structured.

        The diagram may contain only these node types:
        1. "hook"
        2. "videoPart"
        3. "cta"

        NODE CREATION RULES:
        - Start with one "hook" node representing the opening that captures
          attention.
        - Create a new "videoPart" only when there is a meaningful change in
          topic, scene, argument, demonstration, story beat, or purpose.
        - Do not create nodes for small cuts, camera changes, captions, or
          minor visual changes.
        - Create a "cta" node when an explicit or implicit CTA exists.
        - If no CTA exists, create one final "cta" node stating briefly that
          no CTA was detected.
        - Never invent events, dialogue, or a CTA.
        - Keep all nodes in chronological order.
        - Prefer fewer useful nodes over many detailed nodes.

        NODE TEXT RULES:
        Node text must be concise and easy to scan.

        Each node should normally contain only:
        - Approximate timestamp range.
        - A short description of what happens.
        - One notable detail ONLY when something especially important,
          unusual, attention-grabbing, or structurally significant happens.

        Recommended format:
        "00:00-00:04 | Opens with a surprising result before explaining it."

        When something notable happens:
        "00:04-00:10 | Demonstrates the product. Notable: fast before/after reveal."

        Keep each node focused on the main beat.
        Aim for roughly 10-30 words per node when possible.

        DO NOT turn node text into a full analysis report.

        Do not list all of these separately:
        - dialogue
        - visuals
        - captions
        - camera movement
        - transitions
        - editing
        - music
        - sound effects
        - purpose
        - effectiveness

        Mention one of those only when it is important to understanding why
        that specific section stands out.

        HOOK:
        - Briefly describe what happens in the opening.
        - Mention the hook technique only if it is useful or distinctive.
        - Do not add a long explanation of why the hook works.

        VIDEO PART:
        - Describe the main event, information, demonstration, or story beat.
        - Split into another node only when the video's purpose meaningfully changes.

        CTA:
        - State the action requested from the viewer.
        - Keep it short.
        - If no CTA exists, use wording such as:
          "00:42-00:45 | No CTA detected."

        CONNECTION RULES:
        - Connect every node chronologically.
        - Use the exact source and target node IDs.
        - Do not leave nodes disconnected.
        - Do not create arrows to missing nodes.
        - Arrow text must contain MAXIMUM 3 WORDS.
        - Prefer simple transition labels such as:
          "Builds tension"
          "Shows result"
          "Explains why"
          "Then demonstrates"
          "Leads to CTA"
          "Adds proof"
        - Never write a sentence inside an arrow.

        GENERATED PROMPT RULES:
        The top-level "prompt" field must contain a standalone prompt for
        creating a new original video inspired by the analyzed structure.

        It should describe:
        - The main content goal.
        - Target audience.
        - Hook approach.
        - Chronological scene structure.
        - Key messages.
        - Visual direction.
        - Narration or speaking style.
        - Editing pace.
        - On-screen text approach.
        - Audio direction.
        - Recommended CTA.
        - Important structural patterns found in the uploaded video.

        Preserve useful structures and techniques, but do not copy exact
        sentences, branding, protected characters, or the creator's identity.

        LAYOUT RULES:
        The diagram should look like a left-to-right storyboard.

        Use these approximate positions as the visual pattern:

        Node 1:
        x = 90
        y = 110

        Node 2:
        x = 430
        y = 300

        Node 3:
        x = 770
        y = 110

        Node 4:
        x = 1110
        y = 300

        For additional nodes:
        - Continue from left to right.
        - Add approximately 340 to x for every new node.
        - Alternate y between approximately 110 and 300.
        - Do not stack nodes on top of each other.
        - Keep enough empty space between nodes for arrows and arrow labels.
        - Do not force the diagram into only four nodes.
        - The number of nodes must depend on the actual video structure.

        Example continuation:
        Node 5: x = 1450, y = 110
        Node 6: x = 1790, y = 300
        Node 7: x = 2130, y = 110

        For every node:
        - width = 240
        - height = 180
        - zIndex = 1

        Do not increase node height just because more analysis is available.
        Keep the text concise enough to fit the note.

        Generate a unique UUID string for every node and arrow.

        Return exactly one valid JSON object with this structure:

        {
          "name": "Video title",
          "prompt": "Standalone prompt for creating a new original video",
          "private": true,
          "nodes": [
            {
              "id": "unique UUID",
              "type": "hook | videoPart | cta",
              "text": "00:00-00:04 | Short description of what happens.",
              "x": 90,
              "y": 110,
              "width": 240,
              "height": 180,
              "zIndex": 1
            }
          ],
          "arrows": [
            {
              "id": "unique UUID",
              "sourceNodeId": "existing source node UUID",
              "targetNodeId": "existing target node UUID",
              "text": "Builds tension"
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
        - Ensure every arrow text contains no more than 3 words.
        - Ensure node text stays concise.
        - Ensure the result can be validated directly as DiagramCreate.
        """
    ).strip()