import json
import threading
from pathlib import Path

import torch
from peft import AutoPeftModelForCausalLM
from transformers import AutoTokenizer


# Fine-tuned LoRA adapter for Qwen/Qwen3-1.7B.
# The base model is resolved from adapter_config.json.
MODEL_PATH = (
    Path(__file__).resolve().parents[1]
    / "LocalLLM"
    / "qwen-query-generator"
)

# The adapter was trained in bfloat16 and runs on CPU here.
# float32 roughly doubles the memory footprint of the model.
MODEL_DTYPE = torch.bfloat16

# Qwen3 non-thinking sampling settings.
# The small repetition penalty discourages near duplicate queries.
TEMPERATURE = 0.7
TOP_P = 0.8
TOP_K = 20
REPETITION_PENALTY = 1.05
MAX_NEW_TOKENS = 300


class QueryExpansionService:

    def __init__(self):
        self.tokenizer = AutoTokenizer.from_pretrained(str(MODEL_PATH))

        model = AutoPeftModelForCausalLM.from_pretrained(
            str(MODEL_PATH),
            dtype=MODEL_DTYPE,
        )

        # Fold the LoRA weights into the base model.
        # This is inference only and removes the adapter
        # overhead from every generation.
        self.model = model.merge_and_unload()
        self.model.eval()

        self._lock = threading.Lock()

    # This prompt must stay identical to the one used during fine-tuning.
    # The model was trained on the raw prompt text, not on a chat template.
    @staticmethod
    def _build_prompt(niche: str) -> str:
        return (
            "Generate exactly 10 natural YouTube Shorts search queries "
            "for this product.\n\n"
            f"Product: {niche}\n"
            "Every query must be something a potential customer of this exact "
            "product could realistically search for on YouTube Shorts. "
            "Queries may include closely related interests, problems, use cases, "
            "or outcomes, but they must not name or describe a different product. "
            "The 10 queries must represent different search intents. "
            "Do not repeat queries and do not create trivial rewordings of the "
            "same query.\n"
            "Return only valid JSON in exactly this format:\n"
            '{"queries": ["query 1", "query 2", "..."]}'
        )

    def _generate(self, prompt: str) -> str:
        inputs = self.tokenizer(
            prompt,
            return_tensors="pt",
        ).to(self.model.device)

        with self._lock, torch.no_grad():
            outputs = self.model.generate(
                **inputs,
                max_new_tokens=MAX_NEW_TOKENS,
                do_sample=True,
                temperature=TEMPERATURE,
                top_p=TOP_P,
                top_k=TOP_K,
                repetition_penalty=REPETITION_PENALTY,
                pad_token_id=self.tokenizer.eos_token_id,
                eos_token_id=self.tokenizer.eos_token_id,
            )

        # Keep only the tokens the model generated after the prompt
        generated_tokens = outputs[0][inputs["input_ids"].shape[1]:]

        response = self.tokenizer.decode(
            generated_tokens,
            skip_special_tokens=True,
        )

        return response.strip()

    @staticmethod
    def _parse_response(response: str) -> dict:
        try:
            return json.loads(response)
        except json.JSONDecodeError:
            pass

        # The model is trained to return JSON only, but fall back to the
        # first JSON object in the response instead of losing the generation
        start = response.find("{")
        end = response.rfind("}")

        if start == -1 or end <= start:
            raise ValueError("Qwen returned an invalid response")

        try:
            return json.loads(response[start:end + 1])
        except json.JSONDecodeError as error:
            raise ValueError("Qwen returned an invalid response") from error

    # returning list of search queries 6 + the original niche
    def generate_search_queries(  self,niche: str,amount: int = 10, ) -> list[str]:
        prompt = self._build_prompt(niche)

        response = self._generate(prompt)

        data = self._parse_response(response)

        if not isinstance(data, dict):
            raise ValueError("Qwen returned an invalid response")

        generated_queries = data.get("queries")

        if not isinstance(generated_queries, list):
            raise ValueError("Qwen returned an invalid response")

        queries = [
            query.strip()
            for query in generated_queries
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
