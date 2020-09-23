package no.nav.bidrag.arbeidsflyt;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.stream.LongStream;

@SpringBootApplication
public class BidragArbeidsflyt {
    @Value("${kafka.topic}")
    private String topicName;

    @Value("${io.confluent.developer.topic.partitions}")
    private int numPartitions;

    @Value("${io.confluent.developer.topic.replication}")
    private short replicationFactor;

    @Bean
    NewTopic myTestTopic() {
        return new NewTopic(topicName, numPartitions, replicationFactor);
    }

    public static void main(String... args) {
        SpringApplication.run(BidragArbeidsflyt.class, args);
    }
}

@Component
@RequiredArgsConstructor
class Producer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NewTopic topic;

    @EventListener(ApplicationStartedEvent.class)
    public void produce() {

        LongStream.range(0, 10).forEach(i -> {
            String key = "kafka";
            String value = key.toUpperCase() + i;
            kafkaTemplate.send(topic.name(), key, value);

        });
        kafkaTemplate.flush();
    }

}
