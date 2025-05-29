# Kafka Streams Metrics Demo

## Introduction

The purpose of this demo is to add monitoring to our streams application and RocksDB so we can monitor them.
We'll be exposing our metrics through Grafana dashboards.

## Steps

### Create the Grafana service

We want to monitor our metrics in a nice dashboard.

For convenience, the Grafana dashboards and configurations we'll need have already been included in the `grafana` folder in the root of this repository.

Open the file `docker-compose.yml` in this demo folder and add the following service.
```yaml
grafana:
    image: grafana/grafana:7.3.3
    environment:
      GF_INSTALL_PLUGINS: grafana-piechart-panel
    ports:
      - 3000:3000
    volumes:
      - ../grafana/provisioning/:/etc/grafana/provisioning/
```

> NOTE: YAML files are sensitive to indentation.
> Make sure that grafana is directly under the services and not indended below another service.
> If this happens, you'll get an error.

### Add JMX exporter binary and configurations

Now we need to add a JMX exporter library, which is included in this repository.
Create a folder in the root of this repository called `jmx-exporter`.

Locate the file `jmx_prometheus_javaagent-0.14.0.jar` in the `lib` folder and move it to the newly created folder.

Next, create a file called `kafka-broker.yml` in the same folder.
We'll use this file to configure JMX to map its MBean metrics into prometheus-style ones.
Paste
```yaml
lowercaseOutputName: true
lowercaseOutputLabelNames: true
rules:
# Special cases and very specific rules
- pattern : kafka.server<type=(.+), name=(.+), clientId=(.+), topic=(.+), partition=(.*)><>Value
  name: kafka_server_$1_$2
  type: GAUGE
  labels:
    clientId: "$3"
    topic: "$4"
    partition: "$5"
- pattern : kafka.server<type=(.+), name=(.+), clientId=(.+), brokerHost=(.+), brokerPort=(.+)><>Value
  name: kafka_server_$1_$2
  type: GAUGE
  labels:
    clientId: "$3"
    broker: "$4:$5"

- pattern : kafka.server<type=KafkaRequestHandlerPool, name=RequestHandlerAvgIdlePercent><>OneMinuteRate
  name: kafka_server_kafkarequesthandlerpool_requesthandleravgidlepercent_total
  type: GAUGE

- pattern : kafka.server<type=socket-server-metrics, clientSoftwareName=(.+), clientSoftwareVersion=(.+), listener=(.+), networkProcessor=(.+)><>connections
  name: kafka_server_socketservermetrics_connections
  type: GAUGE
  labels:
    client_software_name: "$1"
    client_software_version: "$2"
    listener: "$3"
    network_processor: "$4"

- pattern : 'kafka.server<type=socket-server-metrics, listener=(.+), networkProcessor=(.+)><>(.+):'
  name: kafka_server_socketservermetrics_$3
  type: GAUGE
  labels:
    listener: "$1"
    network_processor: "$2"

# Count and Value
- pattern: kafka.(.+)<type=(.+), name=(.+), (.+)=(.+), (.+)=(.+)><>(Count|Value)
  name: kafka_$1_$2_$3
  labels:
    "$4": "$5"
    "$6": "$7"
- pattern: kafka.(.+)<type=(.+), name=(.+), (.+)=(.+)><>(Count|Value)
  name: kafka_$1_$2_$3
  labels:
    "$4": "$5"
- pattern: kafka.(.+)<type=(.+), name=(.+)><>(Count|Value)
  name: kafka_$1_$2_$3

# Percentile
- pattern: kafka.(.+)<type=(.+), name=(.+), (.+)=(.*), (.+)=(.+)><>(\d+)thPercentile
  name: kafka_$1_$2_$3
  type: GAUGE
  labels:
    "$4": "$5"
    "$6": "$7"
    quantile: "0.$8"
- pattern: kafka.(.+)<type=(.+), name=(.+), (.+)=(.*)><>(\d+)thPercentile
  name: kafka_$1_$2_$3
  type: GAUGE
  labels:
    "$4": "$5"
    quantile: "0.$6"
- pattern: kafka.(.+)<type=(.+), name=(.+)><>(\d+)thPercentile
  name: kafka_$1_$2_$3
  type: GAUGE
  labels:
    quantile: "0.$4"
```
into the file.
This file isn't essential to monitoring streams, but can be nice to use to correlate what we're seeing in streams to what's happening on the broker.

Next, we need to do a similar configuration for streams.
Create a file called `kafka-streams.yml` in the same folder and paste the following into it.
```yaml
lowercaseOutputName: true
rules:
  - pattern : 'kafka.streams<type=stream-metrics, client-id=(.*)><>(.+): (.+)'
    value: 1
    name: kafka_streams_app_info
    labels:
      client-id: $1
      $2: $3
    type: COUNTER
  - pattern : 'kafka.streams<type=(.+), thread-id=(.+), task-id=(.*), (.+)=(.+)><>(.+):'
    name: kafka_streams_$1_$6
    type: GAUGE
    labels:
      thread-id: "$2"
      task-id: "$3"
      $4: "$5"
  - pattern : 'kafka.streams<type=stream-task-metrics, thread-id=(.+), task-id=(.*)><>(.+):'
    name: kafka_streams_stream-task-metrics_$3
    type: GAUGE
    labels:
      thread-id: "$1"
      task-id: "$2"
  - pattern : 'kafka.streams<type=(.+), thread-id=(.+)><>(.+):'
    name: kafka_streams_$1_$3
    type: GAUGE
    labels:
      thread-id: "$2"
```

### Configure Prometheus

Next, we need to tell Prometheus what to scrape, how often, etc.
Create a folder at the root of the repository called `prometheus`.
Inside that folder, create a file called `prometheus.yml` and paste the following configurations into it.
```yaml
global:
  scrape_interval:     15s # By default, scrape targets every 15 seconds.
  evaluation_interval: 15s # By default, scrape targets every 15 seconds.

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
    - targets: ['localhost:9090']

  - job_name: 'kafka'
    static_configs:
    - targets:
      - 'broker:1234'
      labels:
        env: 'dev'

  - job_name: 'producer'
    static_configs:
      - targets:
          - 'producer:1234'
        labels:
          env: 'dev'

  - job_name: 'streams'
    static_configs:
      - targets:
          - "streams:1234"
        labels:
          env: 'dev'
```

### Add the Prometheus service

Now we need to add our prometheus service.

Open the `docker-compose.yml` file again and add the prometheus service to our collection.
```yaml
prometheus:
    image: prom/prometheus:v2.11.1
    ports:
      - 9090:9090
    volumes:
      - ../prometheus/:/etc/prometheus/
```
Don't forget how sensitive YAML files are to correct indentation.

### Modify the Broker to use the JMX exporter

We need to add the JMX exporter to the broker's class path if we want to monitor it (again, this is optional but nice to have).

First, we need to add our binary and configurations to the container where Java can find it.

Open the `docker-compose.yml` file again and modify the `broker` service, adding a volume as follows.
```yaml
volumes:
  - ../jmx-exporter:/usr/share/jmx_exporter/
```
This volume should be at the level directly under the service.

Next, add the following environment variable to it.
```yaml
KAFKA_OPTS: -javaagent:/usr/share/jmx_exporter/jmx_prometheus_javaagent-0.14.0.jar=1234:/usr/share/jmx_exporter/kafka-broker.yml
```
and change the `KAFKA_JMX_PORT` variable to `7071` so the exporter can find it.
We don't need to expose it through the ports, but it doesn't hurt to leave the port settings alone.

### Modify the streams app to use the JMX exporter

We also need to add the JMX exporter to the streams application's classpath.
We can do this by opening the `docker-compose.yml` file again and modifying the `streams` service to add the same volume we had before.
```yaml
volumes:
  - ../jmx-exporter:/usr/share/jmx_exporter/
```

We also need to add a similar environment variable as before.
```yaml
KAFKA_METRICS_RECORDING_LEVEL: "DEBUG"
JAVA_OPTS: -javaagent:/usr/share/jmx_exporter/jmx_prometheus_javaagent-0.14.0.jar=1234:/usr/share/jmx_exporter/kafka-streams.yml -Xmx256M -Xms256M
```
This also includes more detailed metrics by setting the recording level to `DEBUG`.
This does not affect the logging level.

### Replace the Broker and Streams application

Now that we have all our changes in place, we're ready to update our `broker` and `streams` containers.

Since we made no code changes that require rebuilding our image, we simply need to run
```bash
docker compose up -d
```
from the terminal.

If all goes well, then the broker and streams containers will be modified, and our new Prometheus and Grafana containers should be spooled up.

### Checking the dashboards

Let's take a moment to check our work.
Just because the call to `docker compose up -d` worked doesn't mean our metrics are being properly recorded.

Let's open Grafana by navigating to: [http://localhost:3000](http://localhost:3000)

If the Grafana container is running, then you should see a login page appear.

To log in, use the default username and password combination admin/admin.
Press "skip" on the next screen if you don't want to bother setting a new password.

Next, click on the spyglass to search through the various dashboards.

Navigate the "Kafka Overview", "Kafka Streams", and "Kafka Streams - RocksDB Metrics" dashboards.
If you see the graphs populated with information, then you've successfully completed the demo.

Take a few minutes to look through and understand the graphs.

## Special thanks

Special thanks to [LGouelle](https://github.com/LGouellec) and [Monitoring RocksDB project](https://github.com/LGouellec/monitoring-rocksdb-kstream) for guidance.
Feel free to have a look at that repository for more information.

You can also find more JMX monitoring guidance [here](https://github.com/confluentinc/jmx-monitoring-stacks).
