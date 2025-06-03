# RocksDB Configuration Demo

## Introduction

The purpose of this demo is to modify a simple streams application to edit the RocksDB configurations.

## Steps

### Run the WordCountStreamApp application

The `WordCountStreamApp` is a simple streams application that consumes from a topic called `output-topic` and counts the number of times a given word has appeared.

Run the Kafka services and demo stream using
```bash
docker compose up -d
```

> NOTE: Some people may be more used to`docker-compose`, which is the v1 API.\
> The v2 version uses `docker compose` without the dash.
> On some environments, you may have to use the v1 API, but the version without the dash should be preferred as v1 is deprecated.

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
docker exec streams /app/resources/print_latest_rocksdb_options.sh
```
or in a Windows environment you may have to run it as
```bash
docker exec streams //app/resources/print_latest_rocksdb_options.sh
```

If you'd like to search for a specific option, feel free to append `| grep [your_config_name_here]` or output the result to a file by appending `> my_file_name.txt`.
For example, run
```bash
docker exec streams /app/resources/print_latest_rocksdb_options.sh | grep compaction_style
```
to verify the compaction style RocksDB is using. Does it match what you expected?

### Add a RocksDB configuration class

Unfortunately, modifying the RocksDB configurations is not as simple as just creating and passing an instance of `Properties`.
We need to create a custom class that implements the `RocksDBConfigSetter` interface and then pass that class to our streams configs.

First, open `WordCountStreamApp.java` in your editor.

Let's add the imports for the types we'll need
```java
import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.rocksdb.Options;
import org.rocksdb.CompactionStyle;
import org.rocksdb.InfoLogLevel;
import org.rocksdb.RocksDB;
import java.util.Map;
```

Next, create a class called `OptimizedRocksDbConfig` that implements the interface
```java
public static class OptimizedRocksDBConfig implements RocksDBConfigSetter {
    // We'll put our overrides here
}
```

We'll need to make sure the RocksDB library is loaded before any RocksDB operations run, so add
```java
static { RocksDB.loadLibrary(); }
```
to the class.

Next, we need to override the expected `setConfig` method, which we can do by adding
```java
@Override
public void setConfig(final String storeName, final Options options, final Map<String, Object> configs) {
    // custom configs will go here
}
```
to the class.

Finally, the interface requires us to implement a `close` method. Since we didn't instantiate anything that requires cleaning-up, we can make this no-op by adding
```java
@Override
public void close(final String storeName, final Options options) { /* no-op */ }
```
to the class.

### Add custom configurations

Let's assume that our streams application is running on a system with some memory and storage constraints.
We need to restrict the storage amplification that the universal compaction style brings.
Let's also assume we need to limit memtable size to fix some OOM errors we've encountered.
Let's also tweak some settings related to CPU load and disk IO to reduce the strain on our system.

Let's set the configurations by filling our `setConfig` method as follows.
```java
@Override
public void setConfig(final String storeName, final Options options, final Map<String, Object> configs) {
    // Lower space amplification
    options.setCompactionStyle(CompactionStyle.LEVEL);
    // Prevent OOM: smaller memtables
    options.setWriteBufferSize(8 * 1024 * 1024); // 8 MB
    options.setMaxWriteBufferNumber(2); // 2 buffers
    // Limit threads to reduce I/O/CPU contention
    options.setMaxBackgroundCompactions(1); // 1 compaction thread
    options.setMaxBackgroundFlushes(1); // 1 flush thread
}
```

Finally, let's tie it all together by setting the RocksDB configurations to be provided by our custom class by adding
```java
// other props...
props.put(
    StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG,
    OptimizedRocksDBConfig.class.getName()
);
```

### Update the application to the new version

Let's stop and delete the container by running
```bash
docker stop streams
docker rm streams
```
in the terminal.

Next, let's delete the old image by running
```bash
docker rmi demo_1-streams
```

Next, let's rebuild the image with our new code and run the new container by running
```bash
docker-compose up -d
```

If all goes well, the docker image will build and the new container will now be running.

### Verify the changed RocksDB configs

Now that the new container is running, let's make sure our configurations have changed.

Once again, to see the latest configurations automatically, run
```bash
docker exec streams /app/resources/print_latest_rocksdb_options.sh
```
or in a Windows environment you may have to run it as
```bash
docker exec streams //app/resources/print_latest_rocksdb_options.sh
```

If you'd like to search for a specific option, feel free to append `| grep [your_config_name_here]` or output the result to a file by appending `> my_file_name.txt`.
For example, run
```bash
docker exec streams /app/resources/print_latest_rocksdb_options.sh | grep compaction_style
```
to verify the compaction style RocksDB is using.

Make sure all of the configurations match the ones you changed.

If all configurations match the optimizations you set, then the demo is successful!

### Cleanup

Finally, before moving on, let's clean up the containers and volumes by running
```bash
docker compose down -v
```
This will stop and remove all our demo's containers and also remove any named or unnamed volumes.
