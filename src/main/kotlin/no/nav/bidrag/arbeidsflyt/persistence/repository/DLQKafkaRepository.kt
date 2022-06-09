package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.DLQKafka
import org.springframework.data.repository.CrudRepository

interface DLQKafkaRepository: CrudRepository<DLQKafka, Long> {

    fun findByTopicName(topicName: String): List<DLQKafka>
    fun findByRetryTrue(): List<DLQKafka>

    fun deleteByMessageKey(key: String)
    override fun findAll(): List<DLQKafka>
}