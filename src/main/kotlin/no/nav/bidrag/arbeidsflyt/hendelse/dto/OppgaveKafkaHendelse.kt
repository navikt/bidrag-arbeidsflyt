package no.nav.bidrag.arbeidsflyt.hendelse.dto

import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import java.time.LocalDate
import java.time.LocalDateTime

data class OppgaveKafkaHendelse(
    val hendelse: Hendelse,
    val utfortAv: UtfortAv?,
    val oppgave: Oppgave,
) {
    val erOppgaveOpprettetHendelse get() = hendelse.hendelsestype == Hendelse.Hendelsestype.OPPGAVE_OPPRETTET
    val erOppgaveEndretHendelse get() = hendelse.hendelsestype != Hendelse.Hendelsestype.OPPGAVE_OPPRETTET

    fun erTemaBIDEllerFAR(): Boolean = oppgave.kategorisering?.tema == "BID" || oppgave.kategorisering?.tema == "FAR"

    val oppgaveId get() = oppgave.oppgaveId
    val erJournalforingOppgave get() = oppgave.kategorisering?.oppgavetype == OppgaveType.JFR.name
    val tema get() = oppgave.kategorisering?.tema
    val tildeltEnhetsnr get() = oppgave.tilordning?.enhetsnr

    data class Hendelse(
        val hendelsestype: Hendelsestype,
        val tidspunkt: LocalDateTime,
    ) {
        enum class Hendelsestype {
            OPPGAVE_OPPRETTET,
            OPPGAVE_ENDRET,
            OPPGAVE_FERDIGSTILT,
            OPPGAVE_FEILREGISTRERT,
        }
    }

    data class UtfortAv(
        val navIdent: String?,
        val enhetsnr: String?,
    )

    data class Oppgave(
        val oppgaveId: Long,
        val versjon: Int,
        val tilordning: Tilordning? = null,
        val kategorisering: Kategorisering? = null,
        val behandlingsperiode: Behandlingsperiode? = null,
        val bruker: Bruker? = null,
    )

    data class Tilordning(
        val enhetsnr: String?,
        val enhetsmappeId: Long?,
        val navIdent: String?,
    )

    data class Kategorisering(
        val tema: String,
        val oppgavetype: String,
        val behandlingstema: String? = null,
        val behandlingstype: String? = null,
        val prioritet: Prioritet? = null,
    ) {
        enum class Prioritet {
            HOY,
            NORMAL,
            LAV,
        }
    }

    data class Behandlingsperiode(
        val aktiv: LocalDate?,
        val frist: LocalDate?,
    )

    data class Bruker(
        val ident: String?,
        val identType: IdentType?,
    ) {
        enum class IdentType {
            FOLKEREGISTERIDENT,
            NPID,
            ORGNR,
            SAMHANDLERNR,
        }
    }
}
