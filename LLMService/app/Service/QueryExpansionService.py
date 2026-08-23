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
            model_path=(
                "/app/models/"
                "qwen2.5-0.5b-instruct-q4_k_m.gguf"
            ),
            n_ctx=1024,
            n_gpu_layers=0,
            n_threads=2,
            verbose=False,
        )

        self._lock = threading.Lock()

    def generate_search_queries( self,niche: str,amount: int = 8,) -> list[str]:

        messages = cast(
            list[ChatCompletionRequestMessage],
            [
                {
                    "role": "system",
                    "content": (
                        "You generate search queries for discovering "
                        "successful and relevant content on social media platforms. "
                        "Return JSON only."
                    ),
                },
                {
                    "role": "user",
                    "content": f"""
                Generate {amount} diverse social media search queries
                for the niche "{niche}".
                
                Rules:
                - Every query must be strongly related to "{niche}".
                - Cover different subtopics and search intents.
                - Queries should contain 2-5 words.
                - The queries should work well across social media platforms
                  such as YouTube, TikTok, Instagram, Facebook, and X.
                - Focus on queries useful for discovering popular,
                  engaging, or successful content.
                - Avoid duplicate meanings.
                - Include the original niche.
                - Do not include platform names unless they are relevant to the niche.
                - Do not explain anything.
                
                Return:
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
                temperature=0.4,
                max_tokens=160,
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

        # Always include the original niche
        if niche.lower() not in {
            query.lower()
            for query in queries
        }:
            queries.insert(0, niche)

        return queries[:amount]


query_expansion_service = QueryExpansionService()