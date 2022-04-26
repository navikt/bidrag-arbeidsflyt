package no.nav.bidrag.arbeidsflyt.persistence.repository

import no.nav.bidrag.arbeidsflyt.persistence.entity.DLKafka
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface DLKafkaRepository: CrudRepository<DLKafka, Long> {

    fun findByTopicName(topicName: String): List<DLKafka>
    override fun findAll(): List<DLKafka>
}