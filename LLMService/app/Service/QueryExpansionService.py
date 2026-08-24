import json
import threading
from typing import cast

from llama_cpp import Llama
from llama_cpp.llama_types import (
    ChatCompletionRequestMessage,
    ChatCompletionRequestResponseFormat,
)


class QueryExpansionService:

    def __init__(self):
        self.llm = Llama(
            model_path="/app/models/Qwen3-1.7B-Q4_K_M.gguf",
            n_ctx=2048,
            n_gpu_layers=0,
            n_threads=2,
            verbose=False,
        )

        self._lock = threading.Lock()

    # returning list of search queries 6 + the original niche
    def generate_search_queries(  self,niche: str,amount: int = 10, ) -> list[str]:
        messages = cast(
            list[ChatCompletionRequestMessage],
            [
                {
                    "role": "system",
                    "content": (
                        "You generate natural social media search phrases for potential "
                        "customers of a product type. "
                        "Your job is to understand what these people are trying to do, "
                        "solve, improve, learn or achieve, and turn those intents into "
                        "realistic TikTok, YouTube Shorts and Instagram searches. "
                        "Do not perform keyword expansion of the product name. "
                        "Return JSON only."
                    ),
                },
                {
                    "role": "user",
                    "content": f"""
                       Product type: "{niche}"

                       Generate {amount} natural social media search phrases
                       that people who could use this type of product would search for.

                       Before generating the searches, internally think about:
                       - who would use this product
                       - what problems they have
                       - what they are trying to accomplish
                       - what results they want
                       - what tasks they regularly perform
                       - what related content they would watch
                       - what situations create a need for this product

                       Do not output this analysis.
                       Output only the final search phrases.

                       The searches should discover videos whose viewers are likely
                       to be interested in this product type, even if the video never
                       mentions the product category itself.

                       Rules:
                       - Search for customer intent, not the product category.
                       - Do not simply expand or rewrite the product type.
                       - Do not attach extra words to the product type.
                       - Prefer problems, actions, goals, situations and desired results.
                       - Include different customer intents, not variations of one intent.
                       - Write searches like a real person using social media search.
                       - Use simple and natural language.
                       - Prefer phrases that could lead to useful, interesting or viral videos.
                       - Avoid corporate, academic and SEO language.
                       - Avoid generic category descriptions.
                       - Do not generate full sentences.
                       - Do not generate formal questions.
                       - Keep each query between 2 and 5 words.
                       - Do not include years or dates.
                       - Return exactly {amount} queries.
                       - Return JSON only.


                       {{
                           "queries": [
                               "query"
                           ]
                       }}
                       """,
                },
            ],
        )

        response_format = cast(
            ChatCompletionRequestResponseFormat,
            {
                "type": "json_object",
                "schema": {
                    "type": "object",
                    "properties": {
                        "queries": {
                            "type": "array",
                            "items": {
                                "type": "string",
                            },
                        },
                    },
                    "required": ["queries"],
                },
            },
        )

        with self._lock:
            response = self.llm.create_chat_completion(
                messages=messages,
                response_format=response_format,
                temperature=0.5,
                max_tokens=180,
            )

        content = response["choices"][0]["message"]["content"]

        if not isinstance(content, str):
            raise ValueError("Qwen returned an invalid response")

        data = json.loads(content)

        queries = [
            query.strip()
            for query in data.get("queries", [])
            if isinstance(query, str) and query.strip()
        ]

        # Remove duplicates while preserving order
        queries = list(dict.fromkeys(queries))

        # Always keep the original niche as the first search query
        queries = [
            query
            for query in queries
            if query.lower() != niche.lower()
        ]

        queries.insert(0, niche)

        return queries[:amount]


query_expansion_service = QueryExpansionService()