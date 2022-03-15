package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.arbeidsflyt.utils.DateUtils
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val PARAM_JOURNALPOST_ID = "journalpostId={id}"
private const val PARAMS_100_APNE_OPPGAVER = "tema=BID&statuskategori=AAPEN&sorteringsrekkefolge=ASC&sorteringsfelt=FRIST&limit=100"
private const val PARAMS_JOURNALPOST_ID_MED_OG_UTEN_PREFIKS = "$PARAM_JOURNALPOST_ID&journalpostId={prefix}-{id}"

data class OppgaveSokRequest(val journalpostId: String) {

    fun hentParametre(): String {
        if (harJournalpostIdPrefiks()) {
            val prefix = hentPrefiks()
            val idWithoutPrefix = hentJournalpostIdUtenPrefiks()

            return "$PARAMS_100_APNE_OPPGAVER&${
                PARAMS_JOURNALPOST_ID_MED_OG_UTEN_PREFIKS
                    .replace("{prefix}", prefix)
                    .replace("{id}", idWithoutPrefix)
            }"
        }

        return "$PARAMS_100_APNE_OPPGAVER&${
            PARAMS_JOURNALPOST_ID_MED_OG_UTEN_PREFIKS
                .replace("{prefix}", "BID")
                .replace("{id}", journalpostId)
        }"
    }

    private fun harJournalpostIdPrefiks() = journalpostId.contains("-")
    private fun hentJournalpostIdUtenPrefiks() = if (harJournalpostIdPrefiks()) journalpostId.split('-')[1] else journalpostId
    private fun hentPrefiks() = journalpostId.split('-')[0]
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
    fun somOppgaveForHendelse() = OppgaveDataForHendelse(
        id = id ?: -1,
        versjon = versjon ?: -1,
        aktorId = aktoerId,
        oppgavetype = oppgavetype,
        tema = tema,
        tildeltEnhetsnr = tildeltEnhetsnr
    )

    override fun toString() = "{id=$id,journalpostId=$journalpostId,tema=$tema,aktoerId=$aktoerId,oppgavetype=$oppgavetype...}"
}

@Suppress("unused") // used by jackson...
data class OpprettOppgaveRequest(
    var journalpostId: String,
    var aktoerId: String? = null,
    var tema: String? = "BID",
    var tildeltEnhetsnr: String? = "4833",
    var bnr: String? = null,
    var beskrivelse: String? = null
) {
    var oppgavetype: String = "JFR"
    var prioritet: String = Prioritet.HOY.name
    var aktivDato: String = LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))
    var fristFerdigstillelse: String = DateUtils.finnNesteArbeidsdag().format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))
    var opprettetAvEnhetsnr: String = "9999"
    fun somHttpEntity(): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return HttpEntity<OpprettOppgaveRequest>(this, headers)
    }
}

/**
 * Påkrevde data for en oppgave som skal patches i oppgave api
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed class PatchOppgaveRequest {
    var id: Long = -1
    var versjon: Int = -1
    open var aktoerId: String? = null
    open var endretAvEnhetsnr: String? = null
    open var oppgavetype: String? = null
    open var prioritet: String? = null
    open var status: String? = null
    open var tema: String? = null
    open var tildeltEnhetsnr: String? = null
    open var tilordnetRessurs: String? = null

    fun leggOppgaveIdPa(contextUrl: String) = "$contextUrl/${id}".replace("//", "/")
    fun somHttpEntity(): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return HttpEntity<PatchOppgaveRequest>(this, headers)
    }

    protected fun leggTilObligatoriskeVerdier(oppgaveDataForHendelse: OppgaveDataForHendelse) {
        id = oppgaveDataForHendelse.id
        versjon = oppgaveDataForHendelse.versjon
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PatchOppgaveRequest

        if (id != other.id) return false
        if (aktoerId != other.aktoerId) return false
        if (endretAvEnhetsnr != other.endretAvEnhetsnr) return false
        if (oppgavetype != other.oppgavetype) return false
        if (prioritet != other.prioritet) return false
        if (status != other.status) return false
        if (tema != other.tema) return false
        if (tildeltEnhetsnr != other.tildeltEnhetsnr) return false
        if (versjon != other.versjon) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + aktoerId.hashCode()
        result = 31 * result + endretAvEnhetsnr.hashCode()
        result = 31 * result + (oppgavetype?.hashCode() ?: 0)
        result = 31 * result + prioritet.hashCode()
        result = 31 * result + (status?.hashCode() ?: 0)
        result = 31 * result + tema.hashCode()
        result = 31 * result + (tildeltEnhetsnr?.hashCode() ?: 0)
        result = 31 * result + versjon

        return result
    }

    override fun toString() = "${javaClass.simpleName}: id=$id,version=$versjon${fieldsWithValues()}"

    private fun fieldsWithValues(): String {
        return StringBuilder("")
            .append(fieldToString("aktorId", aktoerId))
            .append(fieldToString("endretAvEnhetsnr", endretAvEnhetsnr))
            .append(fieldToString("oppgavetype", oppgavetype))
            .append(fieldToString("prioritet", prioritet))
            .append(fieldToString("status", status))
            .append(fieldToString("tema", tema))
            .append(fieldToString("tildeltEnhetsnr", tildeltEnhetsnr))
            .toString()
    }

    private fun fieldToString(fieldName: String, value: String?) = if (value != null) ",$fieldName=$value" else ""
}

class UpdateOppgaveAfterOpprettRequest(var journalpostId: String) : PatchOppgaveRequest() {
    constructor(oppgaveDataForHendelse: OppgaveDataForHendelse, journalpostIdMedPrefix: String) : this(journalpostIdMedPrefix) {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
    }
}

class OverforOppgaveRequest(override var tildeltEnhetsnr: String?) : PatchOppgaveRequest() {
    override var tilordnetRessurs: String? = ""
    constructor(oppgaveDataForHendelse: OppgaveDataForHendelse, nyttEnhetsnummer: String) : this(nyttEnhetsnummer) {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
    }
}

class OppdaterOppgaveRequest(override var aktoerId: String?) : PatchOppgaveRequest() {
    constructor(oppgaveDataForHendelse: OppgaveDataForHendelse, aktoerId: String?) : this(aktoerId) {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
    }
}

class FerdigstillOppgaveRequest(override var status: String?) : PatchOppgaveRequest() {

    constructor(oppgaveDataForHendelse: OppgaveDataForHendelse) : this(status = "FERDIGSTILT") {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
    }
}

enum class Prioritet {
    HOY //, NORM, LAV
}

enum class OppgaveStatus {
    FERDIGSTILT,
    AAPNET,
    OPPRETTET,
    FEILREGISTRERT,
    UNDER_BEHANDLING
}