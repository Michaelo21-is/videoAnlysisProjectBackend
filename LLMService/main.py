from fastapi import FastAPI
from contextlib import asynccontextmanager
from app.Routers.LLMRouter import router as llm_router
import logging
from app.Config.RabitMqConfig import rabbitmq_manager

@asynccontextmanager
async def lifespan():
    await rabbitmq_manager.connect()

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
