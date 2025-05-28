# RocksDB Configuration Demo

## Introduction

The purpose of this demo is to modify a simple streams application to edit the RocksDB configurations.

## Steps

### Run the WordCountStreamApp application

The `WordCountStreamApp` is a simple streams application that consumes from a topic called `output-topic` and counts the number of times a given word has appeared.

Run the Kafka services and demo stream using
```bash
docker-compose -f docker-compose.wordcount.yml -f ../docker-compose.yml up -d
```

Open [http://localhost:9021](http://localhost:9021) in your browser and test out the application:
1. In one tab, navigate to the cluster->Topics->output-topic->Messages so you can monitor output.
2. In another tab, navigate to cluster->Topics->input-topic->Messages and click "Produce a new message"
3. In the "value" textbox, replace the JSON with a word in quotations (e.g. "firetruck").