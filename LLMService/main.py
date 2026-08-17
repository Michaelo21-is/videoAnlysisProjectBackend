from fastapi import FastAPI
from contextlib import asynccontextmanager
from app.Routers.LLMRouter import router as llm_router
from app.Service.LLMService import LLMService
import logging
from app.Config.RabitMqConfig import rabbitmq_manager


llm_service = LLMService()


@asynccontextmanager
async def lifespan(_app: FastAPI):
    await rabbitmq_manager.connect()

    await rabbitmq_manager.consume_analyze_video(
        llm_service.analyze_video
    )
    await rabbitmq_manager.consume_analyze_content_scrape(
        llm_service.analyze_content_scrape_video
    )

    try:
        yield
    finally:
        await rabbitmq_manager.close()


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(name)s | %(message)s",
)

app = FastAPI(lifespan=lifespan)

app.include_router(llm_router)