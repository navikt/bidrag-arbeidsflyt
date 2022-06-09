package no.nav.bidrag.arbeidsflyt.persistence.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity(name = "dead_letter_kafka")
data class DLQKafka (
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
    var retryCount: Int = 0
)