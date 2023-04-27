package no.nav.bidrag.arbeidsflyt.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity(name = "dead_letter_kafka")
data class DLQKafka(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0,

    @Column(name = "topic_name")
    var topicName: String,

    @Column(name = "message_key")
    var messageKey: String,

    @Column(name = "payload", columnDefinition = "text", length = 10000)
    var payload: String,

    @Column(name = "retry")
    var retry: Boolean? = false,

    @Column(name = "retry_count")
    var retryCount: Int = 0,

    @Column(name = "opprettet_timestamp")
    var createdTimestamp: LocalDateTime = LocalDateTime.now()
)
