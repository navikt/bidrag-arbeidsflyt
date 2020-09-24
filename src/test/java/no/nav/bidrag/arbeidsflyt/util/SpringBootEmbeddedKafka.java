package no.nav.bidrag.arbeidsflyt.util;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
@EmbeddedKafka(partitions = 1)
public abstract class SpringBootEmbeddedKafka {

    @Autowired
    public KafkaTemplate<String, String> template;

    @Autowired
    public KafkaEmbedded kafkaEmbedded;

    @ClassRule
    public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, 0);

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("spring.kafka.bootstrap-servers", embeddedKafka.getBrokersAsString());
        System.setProperty("spring.cloud.stream.kafka.binder.zkNodes", embeddedKafka.getZookeeperConnectionString());
    }

}
