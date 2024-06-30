import asyncio
from confluent_kafka import Consumer, KafkaException
from telegram import Bot
from telegram.ext import Updater

TELEGRAM_TOKEN = ''
CHAT_ID = ''

KAFKA_CONFIG = {
    'bootstrap.servers': 'localhost:29092',
    'group.id': 'telegram_bot_group',
    'auto.offset.reset': 'latest'
}

bot = Bot(token=TELEGRAM_TOKEN)

async def send_message(message):
    print("Sending telegram message:", message)
    await bot.send_message(chat_id=CHAT_ID, text=message)

def kafka_consumer():
    consumer = Consumer(KAFKA_CONFIG)
    consumer.subscribe(['alerts'])
    return consumer

async def consume_kafka_and_send():
    consumer = kafka_consumer()
    try:
        while True:
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() != KafkaError._PARTITION_EOF:
                    print(msg.error())
                    raise KafkaException(msg.error())
                continue

            message = msg.value().decode('utf-8')
            await send_message(message)
    except Exception as e:
        print(f"Error: {e}")
    finally:
        consumer.close()

async def main():
    await consume_kafka_and_send()

if __name__ == '__main__':
    asyncio.run(main())
