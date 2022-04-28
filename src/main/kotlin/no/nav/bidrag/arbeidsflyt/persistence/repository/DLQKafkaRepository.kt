package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import org.springframework.data.repository.CrudRepository

interface DLQKafkaRepository: CrudRepository<DLQKafka, Long> {

    fun findByTopicName(topicName: String): List<DLQKafka>
    override fun findAll(): List<DLQKafka>
}