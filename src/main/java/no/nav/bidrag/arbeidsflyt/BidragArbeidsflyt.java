package no.nav.bidrag.arbeidsflyt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BidragArbeidsflyt {

  @Value("${kafka.topic}")
  protected String topicName;

  @Value("${io.confluent.developer.topic.partitions}")
  protected int numPartitions;

  @Value("${io.confluent.developer.topic.replication}")
  protected short replicationFactor;

  public static void main(String... args) {
    SpringApplication.run(BidragArbeidsflyt.class, args);
  }
}
