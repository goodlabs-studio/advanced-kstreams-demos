package studio.goodlabs;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class DemoStreamsUtils {
    public static void waitForTopics(Properties adminProps, List<String> topics) {
        try (AdminClient admin = AdminClient.create(adminProps)) {
            for (String topic : topics) {
                while (true) {
                    try {
                        DescribeTopicsResult desc = admin.describeTopics(List.of(topic));
                        Map<String, TopicDescription> all = desc.allTopicNames().get();
                        if (all.containsKey(topic)) {
                            System.out.println("Topic " + topic + " exists.");
                            break;
                        }
                    } catch (ExecutionException e) {
                        // Topic doesn’t exist yet
                    }
                    System.out.println("Waiting for topic " + topic + "…");
                    Thread.sleep(Duration.ofSeconds(5).toMillis());
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }
}
