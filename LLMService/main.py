from fastapi import FastAPI
from app.Routers.LLMRouter import router as llm_router
import logging
import sys

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(name)s | %(message)s",
)
app = FastAPI()

app.include_router(llm_router)
