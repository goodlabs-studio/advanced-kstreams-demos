package studio.goodlabs;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class KafkaWordProducer {
    public static void main(String[] args) throws Exception {
        String bootstrapServers = System.getenv().getOrDefault("BOOTSTRAP_SERVERS", "localhost:9092");
        String topic = System.getenv().getOrDefault("TOPIC", "input-topic");

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        Producer<String, String> producer = new KafkaProducer<>(props);

        // Load words from resources/words.txt
        InputStream in = KafkaWordProducer.class
                .getClassLoader()
                .getResourceAsStream("words.txt");
        List<String> words = new BufferedReader(new InputStreamReader(in))
                .lines()
                .collect(Collectors.toList());

        int index = 0;
        while (true) {
            String word = words.get(index);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, word);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    exception.printStackTrace();
                } else {
                    System.out.printf("Sent word='%s' to %s[%d]@%d%n",
                            word, metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
            index = (index + 1) % words.size();
            TimeUnit.SECONDS.sleep(1);
        }
    }
}