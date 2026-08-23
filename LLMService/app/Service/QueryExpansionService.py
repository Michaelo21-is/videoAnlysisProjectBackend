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

    def generate_search_queries(  self,niche: str,amount: int = 6, ) -> list[str]:

        messages = cast(
            list[ChatCompletionRequestMessage],
            [
                {
                    "role": "system",
                    "content": (
                        "You generate semantic search queries for social media. "
                        "Your goal is to find different ways people search for "
                        "the same topic, not variations that simply repeat the "
                        "original niche keyword. "
                        "Return JSON only."
                    ),
                },
                {
                    "role": "user",
                    "content": f"""
                Generate {amount} diverse social media search queries
                semantically related to "{niche}".
                
                Rules:
                - Queries do not need to contain "{niche}".
                - Use synonyms, related concepts, problems, solutions,
                  use cases, and alternative terminology.
                - Think about what someone interested in "{niche}" would actually search for.
                - Each query must represent a different search intent or subtopic.
                - Avoid duplicate or nearly identical queries.
                - Use short, natural search phrases.
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