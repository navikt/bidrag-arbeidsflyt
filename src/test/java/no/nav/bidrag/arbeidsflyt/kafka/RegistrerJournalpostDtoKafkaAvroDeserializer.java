package no.nav.bidrag.arbeidsflyt.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;

public class RegistrerJournalpostDtoKafkaAvroDeserializer extends KafkaAvroDeserializer {
    public Object deserialize(String subject, byte[] byteArray) {
        return super.deserialize(subject, byteArray);
    }
}
