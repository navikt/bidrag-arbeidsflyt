package no.nav.bidrag.arbeidsflyt.dto

import no.nav.bidrag.arbeidsflyt.hendelse.JournalpostHendelse
import org.springframework.http.HttpEntity

data class OppgaveSokRequest(val journalpostId: String, val fagomrade: String, val journalpostHendelse: JournalpostHendelse) {
    fun hentEnhetsnummer() = journalpostHendelse.hentEnhetsnummer()
    fun hentNyJournalforendeEnhet() = journalpostHendelse.journalforendeEnhet?.nyttEnhetsnummer
        ?: throw IllegalStateException("Fant ikke ny journalførende enhet")
}

data class OppgaveSokResponse(var antallTreffTotalt: Int = 0, var oppgaver: List<OppgaveData> = emptyList())

data class OppgaveData(
    var id: Long? = null,
    var tildeltEnhetsnr: String? = null,
    var endretAvEnhetsnr: String? = null,
    var opprettetAvEnhetsnr: String? = null,
    var journalpostId: String? = null,
    var journalpostkilde: String? = null,
    var behandlesAvApplikasjon: String? = null,
    var saksreferanse: String? = null,
    var bnr: String? = null,
    var samhandlernr: String? = null,
    var aktoerId: String? = null,
    var orgnr: String? = null,
    var tilordnetRessurs: String? = null,
    var beskrivelse: String? = null,
    var temagruppe: String? = null,
    var tema: String? = null,
    var behandlingstema: String? = null,
    var oppgavetype: String? = null,
    var behandlingstype: String? = null,
    var versjon: Int? = null,
    var mappeId: String? = null,
    var fristFerdigstillelse: String? = null,
    var aktivDato: String? = null,
    var opprettetTidspunkt: String? = null,
    var opprettetAv: String? = null,
    var endretAv: String? = null,
    var ferdigstiltTidspunkt: String? = null,
    var endretTidspunkt: String? = null,
    var prioritet: String? = null,
    var status: String? = null,
    var metadata: Map<String, String>? = null
) {
    internal fun asJson() = """
        {
          "id": $id,
          "tildeltEnhetsnr": "$tildeltEnhetsnr",
          "endretAvEnhetsnr": "$endretAvEnhetsnr",
          "opprettetAvEnhetsnr": "$opprettetAvEnhetsnr",
          "journalpostId": "$journalpostId",
          "journalpostkilde": "$journalpostkilde",
          "behandlesAvApplikasjon": "$behandlesAvApplikasjon",
          "saksreferanse": "$saksreferanse",
          "bnr": "$bnr",
          "samhandlernr": "$samhandlernr",
          "aktoerId": "$aktoerId",
          "orgnr": "$orgnr",
          "tilordnetRessurs": "$tilordnetRessurs",
          "beskrivelse": "$beskrivelse",
          "temagruppe": "$temagruppe",
          "tema": "$tema",
          "behandlingstema": "$behandlingstema",
          "oppgavetype": "$oppgavetype",
          "behandlingstype": "$behandlingstype",
          "versjon": $versjon,
          "mappeId": $mappeId,
          "fristFerdigstillelse": "$fristFerdigstillelse",
          "aktivDato": "$aktivDato",
          "opprettetTidspunkt": "$opprettetTidspunkt",
          "opprettetAv": "$opprettetAv",
          "endretAv": "$endretAv",
          "ferdigstiltTidspunkt": "$ferdigstiltTidspunkt",
          "endretTidspunkt": "$endretTidspunkt",
          "prioritet": "$prioritet",
          "status": "$status",
          "metadata": ${hentMetadata()}
        }
        """.trimIndent()

    private fun hentMetadata(): String {
        if (metadata == null || metadata!!.isEmpty()) {
            return "{}"
        }

        val keyValues = StringBuilder()

        metadata?.let { it.forEach { (key, value) -> keyValues.append(""""$key":"$value",""") } }

        keyValues.deleteCharAt(keyValues.length - 1) // fjerner siste komma

        return "{$keyValues}"
    }
}

sealed class EndreOppgaveRequest {
    abstract protected fun hentOppgaveId(): Long?

    abstract fun hentRequestType(): String
    fun leggOppgaveIdPa(contextUrl: String) = "$contextUrl/${hentOppgaveId()}".replace("//", "/")
    fun somHttpEntity() = HttpEntity<Any>(this)
}

data class OverforOppgaveRequest(
    private val oppgaveData: OppgaveData,
    private val nyttEnhetsnummer: String
) : EndreOppgaveRequest() {
    init {
        oppgaveData.tildeltEnhetsnr = nyttEnhetsnummer
    }

    override fun toString() = oppgaveData.asJson()
    override fun hentOppgaveId() = oppgaveData.id
    override fun hentRequestType() = "Overfører oppgave med id ${oppgaveData.id} til nytt enhetsnummer ${nyttEnhetsnummer}"
}

data class FerdigstillOppgaveRequest(
    private val oppgaveData: OppgaveData,
    private val tema: String,
    private val tildeltEnhetsnr: String
) : EndreOppgaveRequest() {
    init {
        oppgaveData.tildeltEnhetsnr = tildeltEnhetsnr
        oppgaveData.status = "FERDIGSTILLT"
        oppgaveData.tema = tema
    }

    override fun toString() = oppgaveData.asJson()
    override fun hentOppgaveId() = oppgaveData.id
    override fun hentRequestType() = "Ferdigstiller oppgave med id ${oppgaveData.id}"
}
