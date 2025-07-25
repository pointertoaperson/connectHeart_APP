import paho.mqtt.client as mqtt
import time
import random

GROUP = "TEST"
DEVICE = "DEV01"
BROKER = "broker.hivemq.com"  # ✅ Corrected
PORT = 1883
TOPIC_DEV1 =  GROUP + "/"+ DEVICE + "/heart"
TOPIC_DEV2 = "TEST/DEV02/heart" 

def main():
    client = mqtt.Client()
    client.connect(BROKER, PORT, 60)

    try:
        while True:
            # Generate a random ADC value (0–1023)
            adc_value = random.randint(0, 1023)
            payload_1 = "Heart Rate: 91" 
            payload_2 = "Heart Rate: 77"

            # Publish to the topic
            client.publish(TOPIC_DEV1, payload_1)
            client.publish(TOPIC_DEV2,payload_2)
            print(f"Published: {payload_1} to  device 1 and {payload_2} to device 2")

            time.sleep(1)
    except KeyboardInterrupt:
        print("Exiting...")
        client.disconnect()

if __name__ == "__main__":
    main()

