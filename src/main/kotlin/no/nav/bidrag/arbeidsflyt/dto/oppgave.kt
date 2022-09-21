package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.arbeidsflyt.model.OppgaveDataForHendelse
import no.nav.bidrag.arbeidsflyt.utils.DateUtils
import no.nav.bidrag.dokument.dto.JournalpostHendelse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import no.nav.bidrag.arbeidsflyt.model.*

private val NORSK_TIDSSTEMPEL_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
private val NORSK_DATO_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private const val PARAMETER_JOURNALPOST_ID = "journalpostId"
private const val PARAMETER_OPPGAVE_TYPE = "oppgavetype"
private const val PARAMETER_SAKSREFERANSE = "saksreferanse"
private const val PARAMETER_TEMA = "tema"
private const val PARAMETER_JOURNALPOSTID = "journalpostId"

fun formatterDatoForOppgave(date: LocalDate): String{
    return date.format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))
}

data class OppgaveSokRequest(private val parametre: StringBuilder = StringBuilder()) {

    fun brukBehandlingSomOppgaveType(): OppgaveSokRequest {
        return leggTilParameter(PARAMETER_OPPGAVE_TYPE, OppgaveType.BEH_SAK)
    }

    fun brukVurderDokumentSomOppgaveType(): OppgaveSokRequest {
        return leggTilParameter(PARAMETER_OPPGAVE_TYPE, OppgaveType.VUR)
    }

    fun brukJournalforingSomOppgaveType(): OppgaveSokRequest {
        return leggTilParameter(PARAMETER_OPPGAVE_TYPE, OppgaveType.JFR)
    }

    fun leggTilJournalpostId(journalpostId: String): OppgaveSokRequest {
        leggTilParameter(PARAMETER_JOURNALPOSTID, journalpostId)
        if (harJournalpostIdPrefiks(journalpostId)) {
            val prefix = hentPrefiks(journalpostId)
            val idWithoutPrefix = hentJournalpostIdUtenPrefiks(journalpostId)
            leggTilParameter(PARAMETER_JOURNALPOSTID, idWithoutPrefix)
            leggTilParameter(PARAMETER_JOURNALPOSTID, "${prefix}-${idWithoutPrefix}:${idWithoutPrefix}")
        } else {
            leggTilParameter(PARAMETER_JOURNALPOSTID, "BID-${journalpostId}")
            leggTilParameter(PARAMETER_JOURNALPOSTID, "BID-${journalpostId}:${journalpostId}")
        }

        return this
    }
    fun leggTilFagomrade(fagomrade: String): OppgaveSokRequest {
        return leggTilParameter(PARAMETER_TEMA, fagomrade)
    }

    fun leggTilSaksreferanse(saksnummer: String?): OppgaveSokRequest {
        leggTilParameter(PARAMETER_SAKSREFERANSE, saksnummer)
        return this
    }

    fun leggTilSaksreferanser(saksnummere: List<String>): OppgaveSokRequest {
        saksnummere.forEach { leggTilParameter(PARAMETER_SAKSREFERANSE, it) }
        return this
    }

    private fun harJournalpostIdPrefiks(journalpostId: String) = journalpostId.contains("-")
    private fun hentJournalpostIdUtenPrefiks(journalpostId: String) = if (harJournalpostIdPrefiks(journalpostId)) journalpostId.split('-')[1] else journalpostId
    private fun hentPrefiks(journalpostId: String) = journalpostId.split('-')[0]

    private fun leggTilParameter(navn: String?, verdi: Any?): OppgaveSokRequest {
        if (parametre.isEmpty()) {
            parametre.append('?')
        } else {
            parametre.append('&')
        }

        parametre.append(navn).append('=').append(verdi)

        return this
    }

    fun hentParametre(): String {
        return "$parametre&tema=BID&statuskategori=AAPEN&sorteringsrekkefolge=ASC&sorteringsfelt=FRIST&limit=100"
    }
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
sealed class OpprettOppgaveRequest(
    var beskrivelse: String,
    var oppgavetype: OppgaveType = OppgaveType.JFR,
    var opprettetAvEnhetsnr: String = "9999",
    var prioritet: String = Prioritet.HOY.name,
    var tema: String = "BID",
    var aktivDato: String = formatterDatoForOppgave(LocalDate.now()),
    var fristFerdigstillelse: String = formatterDatoForOppgave(DateUtils.finnNesteArbeidsdag()),
    open var tildeltEnhetsnr: String? = null,
    open val saksreferanse: String? = null,
    open val journalpostId: String? = null,
    open val tilordnetRessurs: String? = null,
    open val aktoerId: String? = null,
    var bnr: String? = null
){
    fun somHttpEntity(): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return HttpEntity<OpprettOppgaveRequest>(this, headers)
    }

    override fun toString() = "${javaClass.simpleName}(${fieldsWithValues()})"

    private fun fieldsWithValues(): String {
        return StringBuilder("")
            .append(fieldToString("beskrivelse", beskrivelse))
            .append(fieldToString("oppgavetype", oppgavetype?.name))
            .append(fieldToString("opprettetAvEnhetsnr", opprettetAvEnhetsnr))
            .append(fieldToString("prioritet", prioritet))
            .append(fieldToString("tema", tema))
            .append(fieldToString("aktivDato", aktivDato))
            .append(fieldToString("fristFerdigstillelse", fristFerdigstillelse))
            .append(fieldToString("tildeltEnhetsnr", tildeltEnhetsnr))
            .append(fieldToString("saksreferanse", saksreferanse))
            .append(fieldToString("journalpostId", journalpostId))
            .append(fieldToString("aktoerId", aktoerId))
            .append(fieldToString("bnr", bnr))
            .toString().removeSuffix(", ")
    }

}

data class OpprettBehandleDokumentOppgaveRequest(
    override var aktoerId: String?,
    private var _journalpostId: String,
    override var saksreferanse: String,
    private var tittel: String,
    private var dokumentDato: LocalDate?,
    private var saksbehandlersInfo: String,
    private var sporingsdata: no.nav.bidrag.dokument.dto.Sporingsdata
): OpprettOppgaveRequest(
    beskrivelse =
            lagBeskrivelseHeader(saksbehandlersInfo) +
            "\u00B7 ${lagDokumentOppgaveTittel("Behandle dokument", tittel, dokumentDato ?: LocalDate.now())}\r\n"+
            "\u00B7 ${lagDokumenterVedlagtBeskrivelse(_journalpostId)}\r\n\r\n",
    oppgavetype = OppgaveType.BEH_SAK,
    opprettetAvEnhetsnr = sporingsdata.enhetsnummer ?: "9999",
    tildeltEnhetsnr = sporingsdata.enhetsnummer,
    tilordnetRessurs = sporingsdata.brukerident
){
    override fun toString(): String {
        return super.toString()
    }
}
@Suppress("unused") // used by jackson...
data class OpprettJournalforingsOppgaveRequest(override var journalpostId: String, override var aktoerId: String?): OpprettOppgaveRequest(
    beskrivelse = "Innkommet brev som skal journalføres og eventuelt saksbehandles. (Denne oppgaven er opprettet automatisk)",
    oppgavetype = OppgaveType.JFR,
) {
    constructor(oppgaveHendelse: OppgaveHendelse, tildeltEnhetsnr: String): this(oppgaveHendelse.journalpostId!!, oppgaveHendelse.hentAktoerId){
        this.bnr = oppgaveHendelse.hentBnr
        this.tema = oppgaveHendelse.tema ?: this.tema
        this.fristFerdigstillelse = if(oppgaveHendelse.fristFerdigstillelse!=null) formatterDatoForOppgave(oppgaveHendelse.fristFerdigstillelse) else this.fristFerdigstillelse
        this.tildeltEnhetsnr = tildeltEnhetsnr
    }

    constructor(journalpostHendelse: JournalpostHendelse, tildeltEnhetsnr: String): this(journalpostHendelse.journalpostMedBareBIDPrefix, journalpostHendelse.aktorId){
        this.tema = "BID" // Kan ikke opprette JFR med tema FAR
        this.tildeltEnhetsnr = tildeltEnhetsnr
    }

    constructor(oppgaveData: OppgaveData): this(oppgaveData.journalpostId!!, oppgaveData.aktoerId){
        this.bnr = oppgaveData.bnr
        this.tema = oppgaveData.tema ?: this.tema
        this.tildeltEnhetsnr = oppgaveData.tildeltEnhetsnr
    }

    constructor(journalpostId: String, aktoerId: String? = null, tema: String = "BID", tildeltEnhetsnr: String = "4833"): this(journalpostId, aktoerId) {
        this.aktoerId = aktoerId
        this.tema = tema
        this.tildeltEnhetsnr = tildeltEnhetsnr
    }

    override fun toString(): String {
        return super.toString()
    }
}

/**
 * Påkrevde data for en oppgave som skal patches i oppgave api
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
open class PatchOppgaveRequest(
    var id: Long = -1,
    var versjon: Int = -1,
    open var aktoerId: String? = null,
    open var endretAvEnhetsnr: String? = null,
    open var oppgavetype: String? = null,
    open var prioritet: String? = null,
    open var status: String? = null,
    open var tema: String? = null,
    open var tildeltEnhetsnr: String? = null,
    open var tilordnetRessurs: String? = null,
    open var beskrivelse: String? = null
){

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

class EndreForNyttDokumentRequest(): PatchOppgaveRequest(){

    constructor(oppgaveDataForHendelse: OppgaveDataForHendelse, journalpostHendelse: JournalpostHendelse) : this() {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
        this.beskrivelse =  "--- ${LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)} ${journalpostHendelse.hentSaksbehandlerInfo()} ---\r\n" +
                "\u00B7 ${lagDokumentOppgaveTittel("Nytt dokument", journalpostHendelse.tittel?:"", journalpostHendelse.dokumentDato!!)}\r\n" +
                "\u00B7 ${lagDokumenterVedlagtBeskrivelse(journalpostHendelse.journalpostId)}\r\n\r\n" +
                "${oppgaveDataForHendelse.beskrivelse}"
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
                (if (!oppgaveHendelse.tilordnetRessurs.isNullOrEmpty()) "${"· Saksbehandler endret fra ${oppgaveHendelse.tilordnetRessurs} til ikke valgt"}\r\n\r\n" else "") +
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

enum class OppgaveType {
    BEH_SAK,
    VUR,
    JFR
}
internal fun lagDokumentOppgaveTittel(oppgaveNavn: String, dokumentbeskrivelse: String, dokumentdato: LocalDate) =
    "$oppgaveNavn ($dokumentbeskrivelse) mottatt ${dokumentdato.format(NORSK_DATO_FORMAT)}"
internal fun lagDokumenterVedlagtBeskrivelse(journalpostId: String) =
    "Dokumenter vedlagt: $journalpostId"
internal fun lagBeskrivelseHeader(saksbehandlersInfo: String): String {
    val dateFormatted = LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)
    return "--- $dateFormatted $saksbehandlersInfo ---\r\n"
}

private fun fieldToString(fieldName: String, value: String?) = if (value != null) "$fieldName=$value, " else ""
