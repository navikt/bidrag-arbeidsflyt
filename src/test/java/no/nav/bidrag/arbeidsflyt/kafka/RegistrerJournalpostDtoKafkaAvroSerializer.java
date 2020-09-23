package no.nav.bidrag.arbeidsflyt.kafka;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import no.nav.bidrag.hendelse.producer.dto.RegistrerJournalpostDto;

import java.util.Map;

public class RegistrerJournalpostDtoKafkaAvroSerializer extends KafkaAvroSerializer {

  public RegistrerJournalpostDtoKafkaAvroSerializer() {
    super();
    super.schemaRegistry = new MockSchemaRegistryClient();
  }

  public RegistrerJournalpostDtoKafkaAvroSerializer(SchemaRegistryClient client) {
    super(new MockSchemaRegistryClient());
  }

  public RegistrerJournalpostDtoKafkaAvroSerializer(SchemaRegistryClient client, Map<String, ?> props) {
    super(new MockSchemaRegistryClient(), props);
  }
}
