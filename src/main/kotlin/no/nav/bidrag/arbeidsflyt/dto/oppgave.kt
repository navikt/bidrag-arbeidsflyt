package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.arbeidsflyt.model.JournalpostHendelse
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.arbeidsflyt.persistence.entity.Journalpost
import no.nav.bidrag.arbeidsflyt.utils.DateUtils
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val PARAM_JOURNALPOST_ID = "journalpostId={id}"
private const val PARAMS_100_APNE_OPPGAVER = "tema=BID&statuskategori=AAPEN&sorteringsrekkefolge=ASC&sorteringsfelt=FRIST&limit=100"
private const val PARAMS_JOURNALPOST_ID_MED_OG_UTEN_PREFIKS = "$PARAM_JOURNALPOST_ID&journalpostId={prefix}-{id}&journalpostId={prefix}-{id}:{id}"
private val NORSK_TIDSSTEMPEL_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

fun formatterDatoForOppgave(date: LocalDate): String{
    return date.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))
}

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
    val statuskategori: Oppgavestatuskategori? = null,
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

    override fun toString() = "{id=$id,journalpostId=$journalpostId,tema=$tema,oppgavetype=$oppgavetype,status=$status,tildeltEnhetsnr=$tildeltEnhetsnr,opprettetTidspunkt=$opprettetTidspunkt...}"
}

@Suppress("unused") // used by jackson...
data class OpprettJournalforingsOppgaveRequest(var journalpostId: String) {
    // Default verdier
    var beskrivelse: String = "Innkommet brev som skal journalføres og eventuelt saksbehandles. (Denne oppgaven er opprettet automatisk)"
    var oppgavetype: String = "JFR"
    var opprettetAvEnhetsnr: String = "9999"
    var prioritet: String = Prioritet.HOY.name
    var tildeltEnhetsnr: String? = "4833"
    var tema: String? = "BID"
    var aktivDato: String = formatterDatoForOppgave(LocalDate.now())
    var fristFerdigstillelse: String = formatterDatoForOppgave(DateUtils.finnNesteArbeidsdag())

    var aktoerId: String? = null
    var bnr: String? = null
    constructor(oppgaveHendelse: OppgaveHendelse, tildeltEnhetsnr: String): this(oppgaveHendelse.journalpostId!!){
        this.aktoerId = oppgaveHendelse.hentAktoerId
        this.bnr = oppgaveHendelse.hentBnr
        this.tema = oppgaveHendelse.tema ?: this.tema
        this.fristFerdigstillelse = if(oppgaveHendelse.fristFerdigstillelse!=null) formatterDatoForOppgave(oppgaveHendelse.fristFerdigstillelse) else this.fristFerdigstillelse
        this.tildeltEnhetsnr = tildeltEnhetsnr
    }

    constructor(journalpostHendelse: JournalpostHendelse, tildeltEnhetsnr: String): this(journalpostHendelse.journalpostMedBareBIDprefix){
        this.aktoerId = journalpostHendelse.aktorId
        this.tema = "BID" // Kan ikke opprette JFR med tema FAR
        this.tildeltEnhetsnr = tildeltEnhetsnr
    }

    constructor(oppgaveData: OppgaveData): this(oppgaveData.journalpostId!!){
        this.aktoerId = oppgaveData.aktoerId
        this.bnr = oppgaveData.bnr
        this.tema = oppgaveData.tema ?: this.tema
        this.tildeltEnhetsnr = oppgaveData.tildeltEnhetsnr
    }

    constructor(journalpostId: String, aktoerId: String? = null, tema: String? = "BID", tildeltEnhetsnr: String? = "4833"): this(journalpostId){
        this.aktoerId = aktoerId
        this.tema = tema
        this.tildeltEnhetsnr = tildeltEnhetsnr
    }

    fun somHttpEntity(): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return HttpEntity<OpprettJournalforingsOppgaveRequest>(this, headers)
    }

    override fun toString() = "${javaClass.simpleName}: ${fieldsWithValues()}"

    private fun fieldsWithValues(): String {
        return StringBuilder()
            .append(fieldToString("journalpostId", journalpostId))
            .append(fieldToString("aktorId", aktoerId))
            .append(fieldToString("bnr", bnr))
            .append(fieldToString("opprettetAvEnhetsnr", opprettetAvEnhetsnr))
            .append(fieldToString("fristFerdigstillelse", fristFerdigstillelse))
            .append(fieldToString("aktivDato", aktivDato))
            .append(fieldToString("oppgavetype", oppgavetype))
            .append(fieldToString("prioritet", prioritet))
            .append(fieldToString("tema", tema))
            .append(fieldToString("tildeltEnhetsnr", tildeltEnhetsnr))
            .append(fieldToString("beskrivelse", beskrivelse))
            .toString()
    }

    private fun fieldToString(fieldName: String, value: String?) = if (value != null) "$fieldName=$value," else ""
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
    open var beskrivelse: String? = null

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

    protected fun leggTilObligatoriskeVerdier(oppgaveHendelse: OppgaveHendelse) {
        id = oppgaveHendelse.id
        versjon = oppgaveHendelse.versjon
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
            .append(fieldToString("tilordnetRessurs", tilordnetRessurs))
            .append(fieldToString("beskrivelse", beskrivelse))
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
    constructor(oppgaveDataForHendelse: OppgaveDataForHendelse, nyttEnhetsnummer: String, saksbehandlersInfo: String) : this(nyttEnhetsnummer) {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
        val dateFormatted = LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)
        this.beskrivelse = "--- $dateFormatted $saksbehandlersInfo ---\r\n" +
                "${"· Oppgave overført fra enhet ${oppgaveDataForHendelse.tildeltEnhetsnr} til $nyttEnhetsnummer"}\r\n\r\n" +
                "${"· Saksbehandler endret fra $saksbehandlersInfo til ikke valgt"}\r\n\r\n" +
                (oppgaveDataForHendelse.beskrivelse ?: "")
    }

    constructor(oppgaveHendelse: OppgaveHendelse, nyttEnhetsnummer: String, saksbehandlersInfo: String) : this(nyttEnhetsnummer) {
        leggTilObligatoriskeVerdier(oppgaveHendelse)
        val dateFormatted = LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)
        this.beskrivelse = "--- $dateFormatted $saksbehandlersInfo ---\r\n" +
                "${"· Oppgave overført fra enhet ${oppgaveHendelse.tildeltEnhetsnr} til $nyttEnhetsnummer"}\r\n\r\n" +
                "${"· Saksbehandler endret fra ${oppgaveHendelse.tilordnetRessurs} til ikke valgt"}\r\n\r\n" +
                (oppgaveHendelse.beskrivelse ?: "")
    }
}

class OppdaterOppgaveRequest(override var aktoerId: String?) : PatchOppgaveRequest() {
    constructor(oppgaveDataForHendelse: OppgaveDataForHendelse, aktoerId: String?) : this(aktoerId) {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
    }
}

class EndreTemaOppgaveRequest(override var tema: String?, override var tildeltEnhetsnr: String?) : PatchOppgaveRequest() {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    override var tilordnetRessurs: String? = null
    constructor(oppgaveDataForHendelse: OppgaveDataForHendelse, tema: String?, tildeltEnhetsnr: String?, saksbehandlersInfo: String) : this(tema = tema, tildeltEnhetsnr = tildeltEnhetsnr) {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
        val dateFormatted = LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)
        this.beskrivelse = "--- $dateFormatted $saksbehandlersInfo ---\r\n" +
                "${"Saksbehandler endret fra $saksbehandlersInfo til ikke valgt"}\r\n\r\n" +
                "${oppgaveDataForHendelse.beskrivelse}"
        this.beskrivelse = "--- $dateFormatted $saksbehandlersInfo ---\r\n" +
                "${"Oppgave overført fra tema ${oppgaveDataForHendelse.tema} til $tema"}\r\n\r\n" +
                "${this.beskrivelse}"
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

enum class OppgaveIdentType {
    AKTOERID,
    ORGNR,
    SAMHANDLERNR,
    BNR
}
enum class OppgaveStatus {
    FERDIGSTILT,
    AAPNET,
    OPPRETTET,
    FEILREGISTRERT,
    UNDER_BEHANDLING
}

enum class Oppgavestatuskategori {
    AAPEN, AVSLUTTET
}