import asyncio
import os, time, json
from confluent_kafka import Consumer, KafkaException, KafkaError
from telegram import Bot

TELEGRAM_TOKEN = os.getenv('TELEGRAM_TOKEN')
CHAT_ID_1 = int(os.getenv('CHAT_ID_1'))
CHAT_ID_2 = int(os.getenv('CHAT_ID_2'))
CHAT_ID_3 = int(os.getenv('CHAT_ID_3'))


KAFKA_CONFIG = {
    'bootstrap.servers': os.getenv('KAFKA_BOOTSTRAP_SERVERS'),
    'group.id': os.getenv('KAFKA_TELEGRAM_GROUP'),
    'auto.offset.reset': 'latest'
}

bot = Bot(token=TELEGRAM_TOKEN)

def convert_coordinates_to_sector(lat: float, long: float):
    mid_lat = 48.82 + (48.89 - 48.82) / 2
    mid_lon = 2.28 + (2.39 - 2.28) / 2
    if lat <= mid_lat:
        if long <= mid_lon:
            return '1'
        else:
            return '2'
    else:
        if long <= mid_lon:
            return '3'
        else:
            return '4'

async def send_message(message):
    try:
        message = json.loads(message)
    except json.JSONDecodeError:
        print("Failed to parse message as JSON")
        return

    print("Sending discord message:", message['timestamp'])
    print(f"Received coordinates: lat={message['pos']['lat']}, lon={message['pos']['lon']}")

    sector = convert_coordinates_to_sector(message['pos']['lat'], message['pos']['lon'])
    message = (
            f"🚨 **HIGH DENSITY ALERT** 🚨\n"
            f"**Location**: {message['pos']['lat']}, {message['pos']['lon']}\n"
            f"**Time**: {message['timestamp']}\n"
            f"**People**: {message['nbPeople']}\n"
            f"**Density**: {message['density']:.2f} people/m²\n"
            f"**Area**: {message['surface']} m²\n"
            f"**Sector**: {sector}\n"
            f"**Action**: Immediate response required."
        )
    await bot.send_message(chat_id=CHAT_ID_1, text=message, parse_mode='Markdown')
    await bot.send_message(chat_id=CHAT_ID_2, text=message, parse_mode='Markdown')
    await bot.send_message(chat_id=CHAT_ID_3, text=message, parse_mode='Markdown')

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

async def main():
    await consume_kafka_and_send()

if __name__ == '__main__':
    print('Telegram bot API running.')
    print(f"{KAFKA_CONFIG=}")
    asyncio.run(main())
