# KStream Reconciliation Demo

## Introduction

Reconciliation is an important consideration in robust streams applications.

The purpose of this demo is to explore message deduplication logic.

## Steps

### Ensure instructions are readable

If these instructions appear in Markdown, switch to Preview mode in your editor.
In Visual Studio Code, you can do this by right-clicking on the README's tab and selecting Open Preview.

### Run the services

First, run the services by running
```bash
docker-compose up -d
```
in the terminal from this directory.

### Test sending a duplicate message

#### Sending messages through the browser:

Open the Control Center by navigating to: [http://localhost:9021](http://localhost:9021)

Duplicate your tab so you can monitor the output topic while sending input messages.
In one tab, navigate to the cluster->Topics->clean-order-topic->Messages so you can monitor output.

In another tab, navigate to cluster->Topics->raw-order-topic->Messages and click "Produce a new message".
The value of the message doesn't matter for this demo, but change the key from "test" to any value you'd like and send the message.

Next, navigate to cluster->Topics->stream-stream-address-topic->Messages and send a message with a key that matches the one you sent on the orders topic, and ensure that a message arrives on the output topic.

#### Sending messages using a demo Sample application

To send a message using the application, run
```bash
docker exec -it order-processor java -cp app.jar studio.goodlabs.DemoMessageProducer broker:29092 raw-order-topic CUSTOMER-ID ORDER
```

Now that you've sent a duplicate message, reflect on the fact that this is an order keyed by the customer ID. What impact might it have downstream?

#### Create deduplication transformer

There are many ways of handling deduplication.
In this approach, we're going to try using a custom transformer that maintains a key-value store and a TTL time.
To avoid introducing unbounded state, which would put unnecessary strain on our state storage and would increase the time needed to rebuild state from the changelog, we're going to assume duplicate message will arrive within a reasonable amount of time.

First, let's create our custom transformer.
Navigate to `src/main/java/studio/goodlabs` in this demo's directory and create a file called `DeduplicationTransformer.java`.

Let's add the package and imports we'll need to the top of the file by pasting the following into it.
```java
package studio.goodlabs;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;
```

Next, let's create our custom transformer class, which will implement an interface called `Transformer`.
Create the class
```java
public class DeduplicationTransformer implements Transformer<String, String, KeyValue<String, String>> {
    // members go here
}
```

We'll need to store some properties, so add a constructor using
```java
private final String storeName;
private final long ttlMs;
private KeyValueStore<String, Long> store;
private ProcessorContext context;

public DeduplicationTransformer(String storeName, long ttlMs) {
    this.storeName = storeName;
    this.ttlMs = ttlMs;
}
```
As you can see, we'll be storing our key-value store here along with the other properties.

Now we need to implement some lifecycle methods for initialization, maintenance, and cleanup.
Add
```java
@Override
@SuppressWarnings("unchecked")
public void init(ProcessorContext context) {
    this.context = context;
    this.store = (KeyValueStore<String, Long>) context.getStateStore(storeName);
    // Schedule cleanup every 30 seconds
    this.context.schedule(Duration.ofSeconds(30), PunctuationType.WALL_CLOCK_TIME, this::cleanup);
}

private void cleanup(long timestamp) {
    try (KeyValueIterator<String, Long> iter = store.all()) {
        while (iter.hasNext()) {
            KeyValue<String, Long> entry = iter.next();
            if (timestamp - entry.value > ttlMs) {
                store.delete(entry.key);
            }
        }
    }
}

@Override
public void close() {
    // no need to handle
}
```
to the class.
There's no need to add any cleanup to our close, since we haven't instantiated anything in our constructor that requires manual cleanup.

Finally, let's add the `transform` method itself.
Add
```java
@Override
public KeyValue<String, String> transform(String key, String value) {
    long now = System.currentTimeMillis();
    Long lastSeen = store.get(key);
    if (lastSeen == null || now - lastSeen > ttlMs) {
        store.put(key, now);
        return KeyValue.pair(key, value);
    }

    return null;
}
```
to the class.
The transform checks to see if the key has been seen within the TTL.
If it has, it's a duplicate and the method will return null.

> NOTE: In more recent versions of Kafka Streams, all transformers have been deprecated in favour of processors.
> This demo uses transformers due to the continued widespread usage of pre-3.3.x Kafka.

### Wire in the custom transform

Open the `OrderProcessor.java` file.

First, we need to add some imports to the ones at the top of the file.
```java
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.time.Duration;
```

Next, let's introduce some constants to the class to hold some of the information we'll need.
Add
```java
private static final String STORE_NAME = "seen-store";
private static final long STORE_TTL_MS = Duration.ofMinutes(1).toMillis();
```
to the `OrderProcessorApp` class.

We now need to build our state store, which will be used by our custom transformer.
After instantiating the stream builder in the `main` method, create the state store builder and configure the state store.
```java
StoreBuilder<KeyValueStore<String, Long>> seenStoreBuilder =
    Stores.keyValueStoreBuilder(
        Stores.persistentKeyValueStore(STORE_NAME),
        Serdes.String(),
        Serdes.Long()
    );
builder.addStateStore(seenStoreBuilder);
```

Finally, add the transform to the stream by adding these calls before sending to the output topic.
```java
.transform(() -> new DeduplicationTransformer(STORE_NAME, STORE_TTL_MS), STORE_NAME)
.filter((key, value) -> value != null) // deduplication returns null for dupes
```

> NOTE: The null filter is necessary because, as you'll remember, we return null if the message is a duplicate.
> Since we don't want to send a null value, we filter on null.

### Updating the stream

Let's stop and delete the container by running
```bash
docker stop order-processor
docker rm order-processor
```
in the terminal.

Next, let's delete the old image by running
```bash
docker rmi demo_4_order-processor
```

Next, let's rebuild the image with our new code and run the new container by running
```bash
docker-compose up -d
```

### Testing the results

Once again, open the Control Center by navigating to: [http://localhost:9021](http://localhost:9021)

Duplicate your tab so you can monitor the output topic while sending input messages.
In one tab, navigate to the cluster->Topics->clean-order-topic->Messages so you can monitor output.

In another tab, navigate to cluster->Topics->raw-order-topic->Messages and click "Produce a new message".
The value of the message doesn't matter for this demo, but change the key from "test" to any value you'd like and send the message.

Next, navigate to cluster->Topics->stream-stream-address-topic->Messages and send a message with a key that matches the one you sent on the orders topic, and ensure that a message arrives on the output topic.

#### Sending messages using a demo Sample application

To send a message using the application, run
```bash
docker exec -it order-processor java -cp app.jar studio.goodlabs.DemoMessageProducer broker:29092 raw-order-topic CUSTOMER-ID ORDER
```

If all goes well, you should see no output on the output topic.
Feel free to send a few more messages to verify.

### Cleanup

Finally, before moving on, let's clean up the containers and volumes by running
```bash
docker-compose down -v
```
This will stop and remove all our demo's containers and also remove any named or unnamed volumes.
