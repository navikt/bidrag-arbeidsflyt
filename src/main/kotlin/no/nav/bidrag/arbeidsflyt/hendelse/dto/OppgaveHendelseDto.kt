package no.nav.bidrag.arbeidsflyt.hendelse.dto

import java.time.LocalDate

import java.time.LocalDateTime



data class OppgaveKafkaHendelseV2(
    val hendelse: Hendelse,
    val utfortAv: UtfortAv,
    val oppgave: Oppgave
) {

    val erOppgaveOpprettetHendelse get() =  hendelse.hendelsestype == Hendelse.Hendelsestype.OPPGAVE_OPPRETTET
    val erOppgaveEndretHendelse get() =  hendelse.hendelsestype == Hendelse.Hendelsestype.OPPGAVE_ENDRET
    fun erTemaBIDEllerFAR(): Boolean = oppgave.kategorisering.tema == "BID" || oppgave.kategorisering.tema == "FAR"
    val oppgaveId get() = oppgave.oppgaveId
    data class Hendelse(val hendelsestype: Hendelsestype, val tidspunkt: LocalDateTime) {
        enum class Hendelsestype {
            OPPGAVE_OPPRETTET,
            OPPGAVE_ENDRET,
            OPPGAVE_FERDIGSTILT,
            OPPGAVE_FEILREGISTRERT
        }
    }

    data class UtfortAv(val navIdent: String, val enhetsnr: String)

    data class Oppgave(
        val oppgaveId: Long,
        val versjon: Int,
        val tilordning: Tilordning,
        val kategorisering: Kategorisering,
        val behandlingsperiode: Behandlingsperiode,
        val bruker: Bruker
    )

    data class Tilordning(
        val enhetsnr: String,
        val enhetsmappeId: Long,
        val navIdent: String
    )

    data class Kategorisering(
        val tema: String,
        val oppgavetype: String,
        val behandlingstema: String,
        val behandlingstype: String,
        val prioritet: Prioritet
    ) {
        enum class Prioritet {
            HOY,
            NORMAL,
            LAV
        }
    }

    data class Behandlingsperiode(
        val aktiv: LocalDate,
        val frist: LocalDate
    )

    data class Bruker(val ident: String, val identType: IdentType) {
        enum class IdentType {
            FOLKEREGISTERIDENT,
            NPID,
            ORGNR,
            SAMHANDLERNR
        }
    }
}