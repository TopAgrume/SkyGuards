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

# Authors
alexandre.devaux-riviere\
paul.duhot\
angelo.eap
