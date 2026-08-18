import os
from typing import List

from redis.asyncio import Redis
from app.Schemea.AnalyzeContentSchema import ExtractedVideoDetails

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD") or None

redis_client: Redis | None = None


async def init_redis() -> None:
    global redis_client

    redis_client = Redis(
        host=REDIS_HOST,
        port=REDIS_PORT,
        db=0,
        password=REDIS_PASSWORD,
        decode_responses=True,
    )

    await redis_client.ping()


async def close_redis() -> None:
    if redis_client is not None:
        await redis_client.aclose()

async def get_cache(key: str) -> str | None :
    if not redis_client:
        raise RuntimeError("Redis client is not initialized")
    return await redis_client.get(key)

async def set_cache(key: str, value: str, ttl: int) -> None:
    if not redis_client:
        raise RuntimeError("Redis client is not initialized")
    await redis_client.set(key, value, ex=ttl)

async def delete_cache(key: str) -> None:
    if not redis_client:
        raise RuntimeError("Redis client is not initialized")
    await redis_client.delete(key)