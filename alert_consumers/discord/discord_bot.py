import asyncio
import discord
import os, time
from confluent_kafka import Consumer, KafkaException, KafkaError

DISCORD_TOKEN = os.getenv('DISCORD_TOKEN')
CHANNEL_ID = int(os.getenv('CHANNEL_ID'))

KAFKA_CONFIG = {
    'bootstrap.servers': os.getenv('KAFKA_BOOTSTRAP_SERVERS'),
    'group.id': os.getenv('KAFKA_DISCORD_GROUP'),
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

def connect_to_kafka_stream(retry_attempts=5):
    consumer = None
    for _ in range(retry_attempts):
        try:
            consumer = kafka_consumer()
            print('Connection with Kafka succeeded.')
            return consumer
        except Exception as e:
            print(f"Kafka connection failed: {e}, retrying in 5 seconds...")
            time.sleep(5)

    print("Failed to connect to Kafka after several attempts. Exiting.")
    return None

async def consume_kafka_and_send():
    consumer = connect_to_kafka_stream()
    if not consumer:
        return
    try:
        while True:
            msg = consumer.poll(0.5)
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() == 3: # UNKNOWN_TOPIC_OR_PARTITION
                    print(f"UNKNOWN_TOPIC_OR_PARTITION. Retrying in 5 seconds...")
                    time.sleep(5)
                    continue
                elif msg.error().code() != KafkaError._PARTITION_EOF:
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
    print('Discord bot API running.')
    print(f"{KAFKA_CONFIG=}")
    asyncio.run(main())
