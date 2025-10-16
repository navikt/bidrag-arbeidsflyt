package no.nav.bidrag.arbeidsflyt.service

import no.nav.bidrag.arbeidsflyt.SECURE_LOGGER
import no.nav.bidrag.arbeidsflyt.hendelse.dto.OppgaveKafkaHendelse
import no.nav.bidrag.commons.util.secureLogger
import no.nav.bidrag.transport.behandling.hendelse.BehandlingHendelse
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import no.nav.bidrag.transport.felles.commonObjectmapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JsonMapperService {
    fun mapBehandlingHendelse(hendelse: String): BehandlingHendelse =
        try {
            commonObjectmapper.readValue(hendelse, BehandlingHendelse::class.java)
        } finally {
            secureLogger.debug { "${"Leser hendelse: {}"} $hendelse" }
        }

    fun mapJournalpostHendelse(hendelse: String): JournalpostHendelse =
        try {
            commonObjectmapper.readValue(hendelse, JournalpostHendelse::class.java)
        } finally {
            secureLogger.debug { "${"Leser hendelse: {}"} $hendelse" }
        }

    fun mapOppgaveHendelseV2(hendelse: String): OppgaveKafkaHendelse =
        try {
            commonObjectmapper.readValue(hendelse, OppgaveKafkaHendelse::class.java)
        } finally {
            secureLogger.debug { "${"Leser hendelse: {}"} $hendelse" }
        }
}
