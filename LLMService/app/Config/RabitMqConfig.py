import json
import os
from collections.abc import Awaitable, Callable
from typing import Any

import aio_pika
from aio_pika import DeliveryMode, ExchangeType, Message
from aio_pika.abc import (
    AbstractIncomingMessage,
    AbstractRobustChannel,
    AbstractRobustConnection,
    AbstractRobustExchange,
    AbstractRobustQueue,
)
from pydantic import BaseModel

from app.Schemea.AnalyzeVideoSchema import AnalyzeVideoSchema
from app.Schemea.AnalyzeContentSchema import AnalyzeContentScrape

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = os.getenv("RABBITMQ_PORT", "5672")
RABBITMQ_USERNAME = os.getenv("RABBITMQ_USERNAME", "guest")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "guest")

RABBITMQ_URL = (
    f"amqp://{RABBITMQ_USERNAME}:{RABBITMQ_PASSWORD}"
    f"@{RABBITMQ_HOST}:{RABBITMQ_PORT}/"
)

VIDEO_ANALYSIS_EXCHANGE = "video-analyze-exchange"

ANALYZE_VIDEO_QUEUE = "video-analyze-queue"
ANALYZE_VIDEO_ROUTING_KEY = "video-analyze-routing-key"

ANALYZE_VIDEO_RESPONSE_QUEUE = "analyze-video-response-queue"
ANALYZE_VIDEO_RESPONSE_ROUTING_KEY =  "analyze_video_response_routing_key"


SCRAPING_ANALYZE_VIDEO_FINISHED_QUEUE = "scraping-finished-queue"
SCRAPING_ANALYZE_VIDEO_FINISHED_ROUTING_KEY = "scraping_finished_routing_key"

ORDER_ANALYZE_CONTENT_EXCHANGE = "order-analyze-content-exchange"

ORDER_ANALYZE_CONTENT_SCRAPE_QUEUE = "order-analyze-content-scrape-queue"

ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_QUEUE = "order-analyze-content-scrape-response-queue"
ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY = "order_analyze_content.scrape_response.routing_key"

AnalyzeVideoHandler = Callable[
    [AnalyzeVideoSchema],
    Awaitable[None],
]
AnalyzeContentScrapeHandler = Callable[
    [AnalyzeContentScrape],
    Awaitable[None],
]


class RabbitMQManager:
    def __init__(self, url: str):
        self.url = url

        self.connection: AbstractRobustConnection | None = None
        self.channel: AbstractRobustChannel | None = None
        self.exchange: AbstractRobustExchange | None = None

        self.analyze_video_queue: AbstractRobustQueue | None = None
        self.analyze_content_scrape_queue: AbstractRobustQueue | None = None

        self.order_analyze_content_scrape_response_queue: AbstractRobustQueue | None = None
    async def connect(self) -> None:
        self.connection = await aio_pika.connect_robust(self.url)
        self.channel = await self.connection.channel()

        await self.channel.set_qos(prefetch_count=1)

        self.exchange = await self.channel.declare_exchange(
            name=VIDEO_ANALYSIS_EXCHANGE,
            type=ExchangeType.DIRECT,
            durable=True,
        )
        self.order_analyze_content_exchange = await self.channel.declare_exchange(
            name=ORDER_ANALYZE_CONTENT_EXCHANGE,
            type=ExchangeType.DIRECT,
            durable=True,
        )

        self.analyze_video_queue = (
            await self.channel.declare_queue(
                name=ANALYZE_VIDEO_QUEUE,
                durable=True,
            )
        )
        self.analyze_content_scrape_queue = (
            await self.channel.declare_queue(
                name=ORDER_ANALYZE_CONTENT_SCRAPE_QUEUE,
                durable=True,
            )
        )
        self.order_analyze_content_scrape_response_queue = (
            await self.channel.declare_queue(
                name=ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_QUEUE,
                durable=True,
            )
        )
        await self.analyze_content_scrape_queue.bind(
            exchange=self.order_analyze_content_exchange,
            routing_key=ORDER_ANALYZE_CONTENT_SCRAPE_RESPONSE_ROUTING_KEY,
        )
        await self.analyze_video_queue.bind(
            exchange=self.exchange,
            routing_key=ANALYZE_VIDEO_ROUTING_KEY,
        )

        analyze_video_response_queue = (
            await self.channel.declare_queue(
                name=ANALYZE_VIDEO_RESPONSE_QUEUE,
                durable=True,
            )
        )

        await analyze_video_response_queue.bind(
            exchange=self.exchange,
            routing_key=ANALYZE_VIDEO_RESPONSE_ROUTING_KEY,
        )

        scraping_finished_queue = (
            await self.channel.declare_queue(
                name=SCRAPING_ANALYZE_VIDEO_FINISHED_QUEUE,
                durable=True,
            )
        )

        await scraping_finished_queue.bind(
            exchange=self.exchange,
            routing_key=SCRAPING_ANALYZE_VIDEO_FINISHED_ROUTING_KEY,
        )

    async def consume_analyze_video( self,handler: AnalyzeVideoHandler,) -> None:
        if self.analyze_video_queue is None:
            raise RuntimeError(
                "Analyze video queue is not initialized"
            )

        async def on_message(
            message: AbstractIncomingMessage,
        ) -> None:
            async with message.process(requeue=False):
                payload = json.loads(
                    message.body.decode("utf-8")
                )

                analyze_video_schema = (
                    AnalyzeVideoSchema.model_validate(payload)
                )

                await handler(analyze_video_schema)

        await self.analyze_video_queue.consume(on_message)

    async def consume_analyze_content_scrape(self, handler: AnalyzeContentScrapeHandler) -> None:
        if self.analyze_content_scrape_queue is None:
            raise RuntimeError(
                "Analyze content scrape queue is not initialized"
            )
        async def on_message(
            message: AbstractIncomingMessage,
        ) -> None:
            async with message.process(requeue=False):
                payload = json.loads(
                    message.body.decode("utf-8")
                )
                analyze_content_scrape = (
                    AnalyzeContentScrape.model_validate(payload)
                )
                await handler(analyze_content_scrape)
        await self.analyze_content_scrape_queue.consume(on_message)

    async def publish(
        self,
        payload: dict[str, Any] | BaseModel,
        routing_key: str,
    ) -> None:
        if self.exchange is None:
            raise RuntimeError(
                "RabbitMQ exchange is not initialized"
            )

        if isinstance(payload, BaseModel):
            serialized_payload = payload.model_dump(mode="json")
        else:
            serialized_payload = payload

        message = Message(
            body=json.dumps(serialized_payload).encode("utf-8"),
            content_type="application/json",
            delivery_mode=DeliveryMode.PERSISTENT,
        )

        await self.exchange.publish(
            message=message,
            routing_key=routing_key,
        )

    async def close(self) -> None:
        if self.connection and not self.connection.is_closed:
            await self.connection.close()

        self.connection = None
        self.channel = None
        self.exchange = None
        self.analyze_video_queue = None


rabbitmq_manager = RabbitMQManager(RABBITMQ_URL)