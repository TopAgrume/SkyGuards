import asyncio
import discord
import os, time, json
from confluent_kafka import Consumer, KafkaException, KafkaError

DISCORD_TOKEN = os.getenv('DISCORD_TOKEN')
CHANNEL_ID_SECTOR_1=int(os.getenv('CHANNEL_ID_SECTOR_1'))
CHANNEL_ID_SECTOR_2=int(os.getenv('CHANNEL_ID_SECTOR_2'))
CHANNEL_ID_SECTOR_3=int(os.getenv('CHANNEL_ID_SECTOR_3'))
CHANNEL_ID_SECTOR_4=int(os.getenv('CHANNEL_ID_SECTOR_4'))

KAFKA_CONFIG = {
    'bootstrap.servers': os.getenv('KAFKA_BOOTSTRAP_SERVERS'),
    'group.id': os.getenv('KAFKA_DISCORD_GROUP'),
    'auto.offset.reset': 'latest'
}

intents = discord.Intents.default()
client = discord.Client(intents=intents)

def convert_coordinates_to_sector(lat: float, long: float):
    mid_lat = 48.82 + (48.89 - 48.82) / 2
    mid_lon = 2.28 + (2.39 - 2.28) / 2
    if lat <= mid_lat:
        if long <= mid_lon:
            return CHANNEL_ID_SECTOR_1, '1'
        else:
            return CHANNEL_ID_SECTOR_2, '2'
    else:
        if long <= mid_lon:
            return CHANNEL_ID_SECTOR_3, '3'
        else:
            return CHANNEL_ID_SECTOR_4, '4'

async def send_message(message):
    try:
        message = json.loads(message)
    except json.JSONDecodeError:
        print("Failed to parse message as JSON")
        return

    await client.wait_until_ready()
    print("Sending discord message:", message['timestamp'])
    print(f"Received coordinates: lat={message['pos']['lat']}, lon={message['pos']['lon']}")


    CHANNEL_ID, sector = convert_coordinates_to_sector(message['pos']['lat'], message['pos']['lon'])
    channel = client.get_channel(CHANNEL_ID)
    if not channel:
        return
    embed = discord.Embed(
        title="High Density Alert!",
        description=f"**Location (lat/lon):**\n`{message['pos']['lat']}`,`{message['pos']['lon']}`",
        color=0xff0000
    )
    date_, time_ = message['timestamp'].split(' ')
    embed.add_field(name="Date", value=date_, inline=True)
    embed.add_field(name="Time", value=time_, inline=True)
    embed.add_field(name="Sector", value=sector, inline=True)

    embed.add_field(name="Number of People", value=message['nbPeople'], inline=True)
    embed.add_field(name="Surface Area", value=f"{message['surface']} m²", inline=True)
    embed.add_field(name="Density", value=f"{round(message['density'], 2)} people/m²", inline=True)
    embed.add_field(name="", value=f"[Check our website](http://localhost:3006)", inline=True)

    await channel.send(embed=embed)

def kafka_consumer():
    consumer = Consumer(KAFKA_CONFIG)
    consumer.subscribe(['alerts'])
    return consumer

def connect_to_kafka_stream(retry_attempts: int = 5):
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
