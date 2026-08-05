import json
from typing import Any

import aio_pika
from aio_pika import DeliveryMode, ExchangeType, Message
from aio_pika.abc import (
    AbstractRobustChannel,
    AbstractRobustConnection,
    AbstractRobustExchange,
    AbstractRobustQueue,
)


RABBITMQ_URL = "amqp://guest:guest@localhost:5672/"

EXCHANGE_NAME = "analyze_video_exchange"
QUEUE_NAME = "analyze_video_queue"
ROUTING_KEY = "analyze_video_routing_key"


class RabbitMQManager:
    def __init__(self, url: str):
        self.url = url

        self.connection: AbstractRobustConnection | None = None
        self.channel: AbstractRobustChannel | None = None
        self.exchange: AbstractRobustExchange | None = None
        self.queue: AbstractRobustQueue | None = None

    async def connect(self) -> None:
        # 1. Connection
        self.connection = await aio_pika.connect_robust(self.url)

        # 2. Channel
        self.channel = await self.connection.channel()

        # 3. Exchange
        self.exchange = await self.channel.declare_exchange(
            name=EXCHANGE_NAME,
            type=ExchangeType.DIRECT,
            durable=True,
        )

        # 4. Queue
        self.queue = await self.channel.declare_queue(
            name=QUEUE_NAME,
            durable=True,
        )

        # 5. Binding: Exchange -> Queue
        await self.queue.bind(
            exchange=self.exchange,
            routing_key=ROUTING_KEY,
        )

    async def publish(self, payload: dict[str, Any]) -> None:
        if self.exchange is None:
            raise RuntimeError("RabbitMQ exchange is not initialized")

        message = Message(
            body=json.dumps(payload).encode("utf-8"),
            content_type="application/json",
            delivery_mode=DeliveryMode.PERSISTENT,
        )

        await self.exchange.publish(
            message=message,
            routing_key=ROUTING_KEY,
        )

    async def close(self) -> None:
        if self.connection and not self.connection.is_closed:
            await self.connection.close()

        self.connection = None
        self.channel = None
        self.exchange = None
        self.queue = None


rabbitmq_manager = RabbitMQManager(RABBITMQ_URL)