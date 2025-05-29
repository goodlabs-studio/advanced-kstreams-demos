package studio.goodlabs;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;

public class DemoMessageProducer {
    public static void main(String[] args) {
        if (args.length < 4 || args.length > 5) {
            System.err.println("Usage: MessageProducerApp <bootstrap-servers> <topic> <key> <value> [timestamp-ms]");
            System.exit(1);
        }
        String bootstrapServers = args[0];
        String topic = args[1];
        String key = args[2];
        String value = args[3];
        Long timestamp = (args.length == 5) ? Long.parseLong(args[4]) : null;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record =
                    (timestamp != null)
                            ? new ProducerRecord<>(topic, null, timestamp, key, value)
                            : new ProducerRecord<>(topic, key, value);

            producer.send(record, (RecordMetadata metadata, Exception exception) -> {
                if (exception != null) {
                    exception.printStackTrace();
                } else {
                    System.out.println("Sent to partition " + metadata.partition() + " @ offset " + metadata.offset());
                }
            });
            producer.flush();
        }
    }
}
