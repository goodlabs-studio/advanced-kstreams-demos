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

### Verify the RocksDB settings

RocksDB is set up to expose very few logs to the streams application's log output.
Because of this, you'll use the script `print_latest_rocks_db_options.sh` conveniently located in streams container to manually search for and print out the latest configs.

> NOTE 1: RocksDB doesn't clean up old configs, but instead keeps all old configs in the same directory, but numerically ordered.
> The latest config will be the one with the highest number.

> NOTE 2: Remember that each task will get its own state store, so you will see configurations per task (with folders named 1_0, 1_1, 1_2, etc.) within the state directory.
> The script prints out the configurations for each task, which is why you'll generally see it print out more than one set of configurations.

To see the latest configurations automatically, run
```bash
docker exec demo_1-streams-1 /app/resources/print_latest_rocksdb_options.sh
```
or in a Windows environment you may have to run it as
```bash
docker exec demo_1-streams-1 //app/resources/print_latest_rocksdb_options.sh
```

If you'd like to search for a specific option, feel free to append `| grep [your_config_name_here]` or output the result to a file by appending `> my_file_name.txt`.

