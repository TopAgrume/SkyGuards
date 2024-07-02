# Data Engineering Project
Final Project for the data engineering course.

# SkyGuard
SkyGuard is a project aimed at preventing mass casualties in crowded spaces. By deploying drones strategically, we monitor crowd density and alert authorities when danger thresholds are exceeded. Our proactive approach aims to reduce the annual toll of 2000 lives lost to overcrowding incidents, enhancing public safety and saving lives, with advanced analytics and real-time insights.

## Workflow

1. **Report Generation and Kafka Streaming**
   - **Skyguards (Drones):** Generate reports stored in `./skyguards`.
   - **Kafka Stream:** These reports are sent to a Kafka stream for real-time processing.

2. **Spark Processing**
   - **Spark Streaming (./spark-streaming):** Consumes reports from the Kafka stream.
     - **Alert System:** This process evaluates the reports. If a report indicates a density higher than 7, an alert is sent to both Telegram and Discord servers via another stream.
   - **Influx (./influx):** Another Spark process consumes messages from Kafka.
     - **Data Formatting and Storage:** This process formats the messages and stores them in an InfluxDB database.
     - **Web Display:** The data is then displayed on a website with a map of Paris.
   - **Daily HDFS Reporting:** At the end of each day, this process (the same as Influx) writes data into HDFS (`./hdfs`) and generates reports analysis.

## How to run the project 

```bash
42sh$ docker-compose up all         
42sh$ docker-compose up skyguards   // generate reports
42sh$ docker-compose up analysis    // Run the hdfs service and generate analysis
42sh$ ./start_website.sh            // Start website
```

# Project Architecture
![image_archi](https://github.com/TopAgrume/SkyGuards/blob/main/SkyGuards_Architecture.png)

# Alerts Telegram & Discord

![image_discord](https://cdn.discordapp.com/attachments/1218983554244022314/1257812079398752297/image.png?ex=6685c448&is=668472c8&hm=a36290600b2f5bdfef78c794e5126a6a33ded6f7270a305d362fa2ea565348a4&)\
![image_telegram](https://cdn.discordapp.com/attachments/1218983554244022314/1257812326300516412/image.png?ex=6685c483&is=66847303&hm=7bb84f5eaf15b1ed05592a5dde93e3269f41a130a780f4f758e18ee3699547f5&)

# Website

![image_website](https://cdn.discordapp.com/attachments/1218983554244022314/1257813022181953629/image.png?ex=6685c529&is=668473a9&hm=6538b6a10f72110c5f24454bc0e706818cc913623c836c394b2062228cd40746&)

# Authors
alexandre.devaux-riviere\
paul.duhot\
angelo.eap
