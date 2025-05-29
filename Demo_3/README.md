# Streams Join Behaviour Demo

## Introduction

Kafka Streams supports a variety of join operations.
The behaviour of these joins can vary greatly depending on the type of join, such as inner, left, outer, etc.
It also depends on what's being joined, such as stream-stream, stream-table, table-table, etc.

The purpose of this lab is to test join behaviours hands on and compare results with our expectations.

## Steps

If you're coming from a previous demo, make sure to delete all containers and volumes from your previous experiments.
This will keep each demo self-contained.
Don't worry, none of the demos rely on the work we did in a previous demo.

### Review your join behaviour

It's critical for this demo that you have a good working knowledge of the behaviour of the various Kafka Streams joins.

If you're a bit rusty, review the article [Crossing the Streams – Joins in Apache Kafka](https://www.confluent.io/blog/crossing-streams-joins-apache-kafka/) on the Confluent blog.
It goes into detail about each type of join, and how it behaves as messages arrive.
Choosing the wrong type of join for a given application can have severe consequences downstream.

### Run the services

First, we need to run all our containers.
We can do this by simply running
```bash
docker compose up  -d
```
in our terminal from this demo directory.

### Test various joins 

Review the code in the following streams applications:
- `StreamStreamJoinApp.java`
- `StreamTableJoinApp.java`
- `TableTableJoinApp.java`
- `StreamGTableJoinApp.java`

Try to identify for each application what type of join is being used: inner, left, etc.
You'll need to use this knowledge to check the applications against what you expect when you produce messages.

Each of the streams applications has 2 input topics: `*-orders-topic` and `*-address-topic`
While these topics don't reflect any real-world use case with much fidelity, they are intended to parallel each other to allow you to explore.
Each topic does a join on orders and addresses, sending the output as a JSON-formatted string to its respective output topic `*-output-topic`.

#### Sending messages through the browser:

Open the Control Center by navigating to: [http://localhost:9021](http://localhost:9021)

Duplicate your tab so you can monitor the output topic while sending input messages.
In one tab, navigate to the cluster->Topics->stream-stream-output-topic->Messages so you can monitor output.

In another tab, navigate to cluster->Topics->stream-stream-order-topic->Messages and click "Produce a new message".
The value of the message doesn't matter for this demo, but change the key from "test" to any value you'd like and send the message.

Next, navigate to cluster->Topics->stream-stream-address-topic->Messages and send message with a different key from the first one you sent, and ensure that nothing arrives at the output topic.
Now send a message with a key that matches the one you sent on the orders topic, and ensure that a message arrives on the output topic.

Did this exercise match your expectation of an inner join?

#### Sending messages using a demo Sample application

Unfortunately, the browser doesn't allow us to manipulate timestamps as we might want to test join behaviour when messages come out of order.
To help us do that, there is a Java application called `DemoMessageProducer` that will allow us to send a message with a timestamp of our choice, independent of the wall clock time.

To send a message using the application, run
```bash
docker exec -it stream-stream java -cp app.jar studio.goodlabs.DemoMessageProducer broker:29092 TOPIC KEY VALUE [TIMESTAMP]
```
where you can replace TOPIC, KEY, VALUE, and TIMESTAMP with the values of your choice.
You can run this from any of the join applications, not just the stream-stream one.

> NOTE: You can generate a timestamp an arbitrary number of seconds in the past by running `timestamp=$(( $(date +%s000) - seconds*1000 ))`

### Goals

This demo is intended to give you free-reign to answer some questions on your own about how joins behave.

Some questions I'd like you to explore:
- Other than the latter not needing co-partitioning, how is a Stream-Table join different than a Stream-GlobalTable join? Do they behave differently when it comes to out-of-order messages? What impact could this have on a streaming application?
- In a Stream-Stream join, is the windowing based on processing time (wallclock at the consumer), log-append time (when the message arrived at the broker), or the timestamp? What does this mean for out-of order messages?
- When does a Table-Table join produce messages to its output topic in an inner join? Does it behave like a Stream-Stream inner join? If not, how is it different? What is the impact of out-of-order messages on a Table-Table join, or a Table in general?
