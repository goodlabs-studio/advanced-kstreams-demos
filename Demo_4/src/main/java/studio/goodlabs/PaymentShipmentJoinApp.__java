package studio.goodlabs;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.JoinWindows;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class PaymentShipmentJoinApp {
    private static final String PAYMENTS_TOPIC = "payments-topic";
    private static final String SHIPPING_TOPIC = "shipping-topic";
    private static final String OUTPUT_TOPIC = "joined-payment-shipment-topic";

    private static final String BOOTSTRAP_SERVERS = System.getenv("KAFKA_BOOTSTRAP_SERVERS");

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "payment-shipment-join-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        DemoStreamsUtils.waitForTopics(
                props,
                List.of(PAYMENTS_TOPIC, SHIPPING_TOPIC, OUTPUT_TOPIC)
        );

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> payments  = builder.stream(PAYMENTS_TOPIC);
        KStream<String, String> shipments = builder.stream(SHIPPING_TOPIC);

        payments.join(
            shipments,
            (payment, shipment) -> "{\"payment\":" + payment + ",\"shipment\":" + shipment + "}",
            JoinWindows.of(Duration.ofSeconds(30))
        )
        .to(OUTPUT_TOPIC);

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
