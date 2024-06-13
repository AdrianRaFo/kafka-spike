
# Kafka Spike

- [Design considerations](#design-considerations)
  - [Group id](#group-id)
  - [Client id](#client-id)
  - [Producer design](#producer-design)
    - [Partition](#partition)
    - [Same *client id* for producers](#same-client-id-for-producers)
  - [Consumer design](#consumer-design)
    - [Partition Rebalance](#partition-rebalance)
    - [Multi-threaded Processing](#multi-threaded-processing)
    - [auto.offset.reset](#autooffsetreset)
    - [enable.auto.commit](#enableautocommit)
    - [Same *client id* for consumers](#same-client-id-for-consumers)
- [Run app locally](#run-app-locally)


## Design considerations

### Group id

The basic idea is that if two consumers have the same group ID and subscribe to the same topic, 
each will be assigned a subset of the partitions in the topic and 
will therefore only read a subset of the messages individually 
(but all the messages will be read by the group as a whole). 
If you need a consumer to see every single message in isolation in the topics it should be subscribed to a unique `group.id`.

### Client id

This is an optional field. 
If you don't provide one Kafka client will generate one for you. 
This could be any string, and would be used by the brokers to identify messages sent from the client. 
It is used in logging and metrics, and for quotas.

The purpose of this is to be able to track the source of requests 
beyond just ip/port by allowing a logical application name to be included in server-side request logging. 
Standard Kafka producer and consumer clients use `client.id` in metric names to disambiguate JMX MBeans 
when multiple instances are running in the same JVM.

### Producer design

The producer is **thread safe** and **sharing a single producer instance** across threads 
will generally be faster than having multiple instances.

#### Partition

If a valid partition number is specified that partition will be used when sending the record. 
If no partition is specified but a key is present a partition will be chosen by using a hash of the key. 
**If neither key nor partition is present a partition will be assigned in a round-robin fashion**.

#### Same *client id* for producers

Create **unique client id per producer**.

Look at [Same *client id* for consumers](#same-client-id-for-consumers) for further reading.

### Consumer design

Kafka consumers are typically part of a **consumer group**. When multiple consumers are subscribed to a topic 
and belong to the same consumer group, 
each consumer in the group will receive messages from a **different subset of the partitions in the topic**.

Consumers in the **same consumer group split the partitions among them**. 
The main way we scale data consumption from a Kafka topic is by adding more consumers to a consumer group. 
**For each partition it is consuming, the consumer stores its current location**, 
so other consumers will know where to continue after a restart.

The main way consumers can **lose messages** is when committing offsets for events 
they've read but **haven't completely processed yet**.

Consumers can create throughput issues on the other side of the pipeline. 
**The maximum number of consumers for a topic is equal to the number of partitions**. 
You require enough partitions to handle all the consumers needed to keep up with the producers.

This is a good reason to create **topics with a large number of partitions**, 
it allows adding more consumers when the load increases. 
Keep in mind that **there is no point in adding more consumers than you have partitions in a topic**, 
some consumers will just be idle.

It is very common to have **multiple applications that need to read data from the same topic**. 
To make sure an application gets all the messages in a topic, **ensure the application has its own consumer group**.

Review `partition.assignment.strategy` property to define the strategy (Range, RoundRobin).

#### Partition Rebalance

Reassignment of partitions to consumers also happen when:

- a new consumer is added
- the consumer shuts down or crashes
- the topics the consumer group is consuming are modified

During a **rebalance**, consumers can't consume messages, 
so a **rebalance** is basically a **short window of unavailability** of the entire consumer group.

#### Multi-threaded Processing

The **Kafka consumer is NOT thread-safe**. General patterns

- One Consumer Per Thread
- Decouple Consumption and Processing

You **can't have multiple consumers that belong to the same group in one thread** 
and you **can't have multiple threads safely use the same consumer**. 
One consumer per thread is the rule. 
To run multiple consumers in the same group in one application, 
you will need to run each in its own thread.

To get more detail click [here](https://kafka.apache.org/23/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html#multithreaded)

#### auto.offset.reset

This parameter controls what the consumer will do when no offsets are committed 
(e.g., when the consumer first starts) or when the consumer asks for offsets that don't exist in the broker.

- **earliest**, the consumer will start from the beginning of the partition whenever
  it doesn't have a valid offset. This can lead to the consumer processing a lot of messages twice, 
  but it guarantees to minimize data loss.

- **latest** the consumer will start at the end of the partition. 
  This minimizes duplicate processing by the consumer 
  but almost certainly leads to some messages getting missed by the consumer.

#### enable.auto.commit

This parameter controls whether the consumer will commit offsets automatically, and defaults to **true**. 
**Consumer's offset will be periodically committed in the background**.

Set it to false if you prefer to control when offsets are committed, 
which is necessary to **minimize duplicates and avoid missing data**.

Instead of relying on the consumer to periodically commit consumed offsets, 
**users can also control when records should be considered as consumed and hence commit their offsets**. 
This is useful when the **consumption of the messages is coupled with some processing logic 
and hence a message should not be considered as consumed until it is completed processing**.

#### Same *client id* for consumers

Ideally, **unique client id per consumer**.

Sharing multiple client objects with the same client id is never a recommended manner as it have multiple issues: 
besides the metrics mess-up mentioned above, 
broker also rely on the client-ids.[[Source](https://github.com/apache/kafka/pull/3328#issuecomment-316137237)]

In general `client-ids` are used as distinguishing identifiers 
for client metrics(though in customizable reporters, users could choose to ignore the client-id to aggregate 
all clients into a single metric; in the default JmxReporter impl we choose to have one metric per client) 
and also for broker-side request logging, so users are suggested to use different values for different clients.
As for quotas, like mentioned in the PR we have been suggesting users to 
apply the `user id` as the principle 
if they want to have multiple clients sharing the same quota.[[Source](https://issues.apache.org/jira/browse/KAFKA-3992?focusedCommentId=16096662&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-16096662)]

You can find more info in this:

- [Stackoverflow](https://stackoverflow.com/questions/33874151/can-multiple-threads-able-to-use-the-same-client-id-in-apache-kafka).
- [Jira](https://issues.apache.org/jira/browse/KAFKA-3992?focusedCommentId=15823394&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-15823394)
- [github](https://github.com/apache/kafka/pull/3328)
- [User quotas](https://cwiki.apache.org/confluence/display/KAFKA/KIP-55%3A+Secure+Quotas+for+Authenticated+Users)

## Run app locally

Run kafka server

```sh
  > docker-compose up -d
```

Verify kafka is working using [Kafka-rest](https://github.com/confluentinc/kafka-rest)

```sh
    > curl "http://localhost:8082/topics"
```

Run consumer and http server

using kafka4s
```sh
  > sbt fs2kafka/run
```


Click to see [messages](http://localhost:8080/index.html). However, if you prefer you can query using:

```sh
  > curl http://localhost:8080/hello
```
