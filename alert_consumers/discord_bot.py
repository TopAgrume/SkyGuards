import asyncio
import discord
from confluent_kafka import Consumer, KafkaException

DISCORD_TOKEN = ''
CHANNEL_ID = 1234

KAFKA_CONFIG = {
    'bootstrap.servers': 'localhost:29092',
    'group.id': 'discord_bot_group',
    'auto.offset.reset': 'latest'
}

intents = discord.Intents.default()
client = discord.Client(intents=intents)

async def send_message(message):
    print("Sending discord message:", message)
    await client.wait_until_ready()
    channel = client.get_channel(CHANNEL_ID)
    if channel:
        await channel.send(message)

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

@client.event
async def on_ready():
    print(f'Logged in as {client.user}')

async def main():
    task1 = client.start(DISCORD_TOKEN)
    task2 = consume_kafka_and_send()
    await asyncio.gather(task1, task2)

if __name__ == '__main__':
    asyncio.run(main())
