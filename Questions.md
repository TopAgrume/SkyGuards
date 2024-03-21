### What technical/business constraints should the data storage component of the program architecture meet to fulfill the requirement described by the customer in paragraph "Statistics"? So what kind of component(s) (listed in the lecture) will the architecture need?

Raw data: The raw data collected by the drones is sent in the form of JSON files. This includes information such as geographic coordinates, timestamp, surface area and number of people scanned, and possibly other contextual data such as battery level, speed, temperature for predictive maintenance.

Storage of current information on the area to be monitored: To store this data, we opted for a Cassandra database (fast distributed NoSQL for writing). The two constraints chosen for this database are Availability and Partioning, and the fact that it scales horizontally is an advantage for us. In fact, the frequency of report arrivals is very high, so we need to have access to the database at all times. As this system is designed to ensure the safety of a large number of people at major events, it must be able to function even during a machine breakdown (non-general outage).

On the datalake side: in order not to lose speed, we opted for another cassandra database storing all modifications made over a given period (e.g. 24h). The aim would be to efficiently store the data coming from processing, so as not to delay the processing to be carried out on the stream, and then to transfer it to the datalake (HDFS) without any major time constraints.

### What business constraint should the architecture meet to fulfill the requirement describe in the paragraph "Alert"? Which component to choose?
    
To meet the need for fast and efficient alert processing, we have opted for a distributed streaming architecture. This approach involves several distinct consumer groups, one of which is specifically dedicated to alert management. This segmentation significantly reduces the time needed to process reports. In addition, to ensure quick and accurate transmission of alerts to the right police stations/officers, we decided to use a distributed streaming architecture once again.

Obtaining the right emergency contacts: For greater performance in alert management, the officers contact are stored in a read-only document-oriented database. For this type of operation, we opted for a MongoDB (AP) database.
