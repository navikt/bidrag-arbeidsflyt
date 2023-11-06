package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.arbeidsflyt.model.ENHET_FAGPOST
import no.nav.bidrag.arbeidsflyt.model.isBidJournalpostId
import no.nav.bidrag.arbeidsflyt.model.journalpostMedBareBIDPrefix
import no.nav.bidrag.arbeidsflyt.model.tilFagområdeBeskrivelse
import no.nav.bidrag.commons.util.VirkedagerProvider
import no.nav.bidrag.transport.dokument.JournalpostHendelse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val NORSK_TIDSSTEMPEL_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
private val NORSK_DATO_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private const val PARAMETER_JOURNALPOST_ID = "journalpostId"
private const val PARAMETER_OPPGAVE_TYPE = "oppgavetype"
private const val PARAMETER_SAKSREFERANSE = "saksreferanse"
private const val PARAMETER_TEMA = "tema"
private const val PARAMETER_JOURNALPOSTID = "journalpostId"

fun formatterDatoForOppgave(date: LocalDate): String {
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
            leggTilParameter(PARAMETER_JOURNALPOSTID, "$prefix-$idWithoutPrefix:$idWithoutPrefix")
        } else {
            leggTilParameter(PARAMETER_JOURNALPOSTID, "BID-$journalpostId")
            leggTilParameter(PARAMETER_JOURNALPOSTID, "BID-$journalpostId:$journalpostId")
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

    private fun hentJournalpostIdUtenPrefiks(journalpostId: String) =
        if (harJournalpostIdPrefiks(journalpostId)) journalpostId.split('-')[1] else journalpostId

    private fun hentPrefiks(journalpostId: String) = journalpostId.split('-')[0]

    private fun leggTilParameter(
        navn: String?,
        verdi: Any?,
    ): OppgaveSokRequest {
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
    val id: Long,
    val tildeltEnhetsnr: String? = null,
    val endretAvEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val journalpostkilde: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val saksreferanse: String? = null,
    val bnr: String? = null,
    val samhandlernr: String? = null,
    val aktoerId: String? = null,
    val orgnr: String? = null,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String? = null,
    val temagruppe: String? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val versjon: Int = -1,
    val mappeId: String? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val aktivDato: String? = null,
    val opprettetTidspunkt: String? = null,
    val opprettetAv: String? = null,
    val endretAv: String? = null,
    val ferdigstiltTidspunkt: String? = null,
    val endretTidspunkt: String? = null,
    val prioritet: String? = null,
    val status: OppgaveStatus? = null,
    val metadata: Map<String, String>? = null,
) {
    override fun toString() =
        "{id=$id,journalpostId=$journalpostId,tema=$tema,oppgavetype=$oppgavetype,status=$status,tildeltEnhetsnr=$tildeltEnhetsnr,opprettetTidspunkt=$opprettetTidspunkt...}"

    fun erTemaBIDEllerFAR(): Boolean = tema == "BID" || tema == "FAR"

    fun erAapenJournalforingsoppgave(): Boolean = erStatusKategoriAapen && erJournalforingOppgave

    fun erAapenVurderDokumentOppgave(): Boolean = erStatusKategoriAapen && erVurderDokumentOppgave

    fun erReturoppgave(): Boolean = erStatusKategoriAapen && erReturOppgave

    private val erStatusKategoriAapen get() =
        listOf(
            OppgaveStatus.AAPNET,
            OppgaveStatus.OPPRETTET,
            OppgaveStatus.UNDER_BEHANDLING,
        ).contains(status)
    val erJournalforingOppgave get() = oppgavetype == OppgaveType.JFR.name
    private val erVurderDokumentOppgave get() = oppgavetype == OppgaveType.VUR.name
    private val erReturOppgave get() = oppgavetype == OppgaveType.RETUR.name
    internal val hasJournalpostId get() = !journalpostId.isNullOrEmpty()

    private fun harJournalpostIdPrefix() = hasJournalpostId && journalpostId!!.contains("-")

    fun erAvsluttetJournalforingsoppgave(): Boolean = erStatusKategoriAvsluttet && erJournalforingOppgave

    internal val erStatusKategoriAvsluttet get() = listOf(OppgaveStatus.FERDIGSTILT, OppgaveStatus.FEILREGISTRERT).contains(status)
    val tilhorerFagpost get() = tildeltEnhetsnr == ENHET_FAGPOST
    internal val hentIdent get() = aktoerId

    internal val journalpostIdMedPrefix get() =
        if (journalpostId.isNullOrEmpty() || harJournalpostIdPrefix()) {
            journalpostId
        } else if (isBidJournalpostId(journalpostId)) {
            "BID-$journalpostId"
        } else {
            "JOARK-$journalpostId"
        }
}

@Suppress("unused") // used by jackson...
sealed class OpprettOppgaveRequest(
    var beskrivelse: String,
    var oppgavetype: OppgaveType = OppgaveType.JFR,
    var opprettetAvEnhetsnr: String = "9999",
    var prioritet: String = Prioritet.HOY.name,
    var tema: String = "BID",
    var aktivDato: String = formatterDatoForOppgave(LocalDate.now()),
    var fristFerdigstillelse: String = formatterDatoForOppgave(VirkedagerProvider.nesteVirkedag()),
    open var tildeltEnhetsnr: String? = null,
    open val saksreferanse: String? = null,
    open val journalpostId: String? = null,
    open val tilordnetRessurs: String? = null,
    open val aktoerId: String? = null,
    var bnr: String? = null,
) {
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
    private var sporingsdata: no.nav.bidrag.transport.dokument.Sporingsdata,
) : OpprettOppgaveRequest(
        beskrivelse =
            lagBeskrivelseHeader(saksbehandlersInfo) +
                "${lagDokumentOppgaveTittel("Behandle dokument", tittel, dokumentDato ?: LocalDate.now())}\r\n" +
                "\u00B7 ${lagDokumenterVedlagtBeskrivelse(_journalpostId)}\r\n\r\n",
        oppgavetype = OppgaveType.BEH_SAK,
        opprettetAvEnhetsnr = sporingsdata.enhetsnummer ?: "9999",
        tildeltEnhetsnr = sporingsdata.enhetsnummer,
        tilordnetRessurs = if (sporingsdata.brukerident.isNullOrEmpty() || sporingsdata.brukerident!!.length > 7) null else sporingsdata.brukerident,
    ) {
    override fun toString(): String {
        return super.toString()
    }
}

@Suppress("unused") // used by jackson...
data class OpprettJournalforingsOppgaveRequest(override var journalpostId: String, override var aktoerId: String?) : OpprettOppgaveRequest(
    beskrivelse = "Innkommet brev som skal journalføres og eventuelt saksbehandles. (Denne oppgaven er opprettet automatisk)",
    oppgavetype = OppgaveType.JFR,
) {
    constructor(oppgaveHendelse: OppgaveData, tildeltEnhetsnr: String) : this(oppgaveHendelse.journalpostId!!, oppgaveHendelse.aktoerId) {
        this.bnr = oppgaveHendelse.bnr
        this.tema = oppgaveHendelse.tema ?: this.tema
        this.fristFerdigstillelse =
            if (oppgaveHendelse.fristFerdigstillelse != null) formatterDatoForOppgave(oppgaveHendelse.fristFerdigstillelse) else this.fristFerdigstillelse
        this.tildeltEnhetsnr = tildeltEnhetsnr
    }

    constructor(journalpostHendelse: JournalpostHendelse, tildeltEnhetsnr: String) : this(
        journalpostHendelse.journalpostMedBareBIDPrefix,
        journalpostHendelse.aktorId,
    ) {
        this.tema = "BID" // Kan ikke opprette JFR med tema FAR
        this.tildeltEnhetsnr = tildeltEnhetsnr
    }

    constructor(oppgaveData: OppgaveData) : this(oppgaveData.journalpostId!!, oppgaveData.aktoerId) {
        this.bnr = oppgaveData.bnr
        this.tema = oppgaveData.tema ?: this.tema
        this.tildeltEnhetsnr = oppgaveData.tildeltEnhetsnr
    }

    constructor(journalpostId: String, aktoerId: String? = null, tema: String = "BID", tildeltEnhetsnr: String = "4833") : this(
        journalpostId,
        aktoerId,
    ) {
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
    open var beskrivelse: String? = null,
) {
    fun leggOppgaveIdPa(contextUrl: String) = "$contextUrl/$id".replace("//", "/")

    open fun somHttpEntity(): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return HttpEntity<PatchOppgaveRequest>(this, headers)
    }

    protected fun leggTilObligatoriskeVerdier(oppgaveDataForHendelse: OppgaveData) {
        id = oppgaveDataForHendelse.id
        versjon = oppgaveDataForHendelse.versjon
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

    private fun fieldToString(
        fieldName: String,
        value: String?,
    ) = if (value != null) ",$fieldName=$value" else ""
}

class OppdaterOppgave() : PatchOppgaveRequest() {
    private var _hasChanged: Boolean = false
    private var saksbehandlerInfo: String = "Automatisk jobb"
    private var oppgaveDataForHendelse: OppgaveData? = null

    constructor(oppgaveDataForHendelse: OppgaveData, saksbehandlersInfo: String? = null) : this() {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
        this.oppgaveDataForHendelse = oppgaveDataForHendelse
        this.saksbehandlerInfo = saksbehandlersInfo ?: this.saksbehandlerInfo
        this.endretAvEnhetsnr = "9999"
    }

    fun ferdigstill(): OppdaterOppgave {
        status = "FERDIGSTILT"
        _hasChanged = true
        return this
    }

    fun endreOppgavetype(nyOppgavetype: OppgaveType): OppdaterOppgave {
        oppgavetype = nyOppgavetype.name
        tilordnetRessurs = ""
        _hasChanged = true
        return this
    }

    fun overforTilEnhet(nyTildeltEnhetsnr: String): OppdaterOppgave {
        tildeltEnhetsnr = nyTildeltEnhetsnr
        tilordnetRessurs = ""
        _hasChanged = true
        return this
    }

    fun hasChanged(): Boolean {
        return _hasChanged
    }

    private fun oppdaterBeskrivelse() {
        var nyBeskrivelse = ""
        if (erOppgavetypeEndret) {
            nyBeskrivelse += "\u00B7 Oppgavetype endret fra ${OppgaveType.descriptionFrom(eksisterendeOppgavetype)} til ${
                OppgaveType.descriptionFrom(
                    oppgavetype,
                )
            }\r\n"
        }

        if (erEnhetEndret) {
            nyBeskrivelse += "\u00B7 Oppgave overført fra enhet $eksisterendeTildeltEnhet til $tildeltEnhetsnr\r\n"
        }

        if (erTilordnetRessursEndretFraValgtTilIkkeValgt) {
            nyBeskrivelse += "\u00B7 Saksbehandler endret fra $eksisterendeTilordnetRessurs til ikke valgt\r\n"
        }

        if (nyBeskrivelse.isNotEmpty()) {
            this.beskrivelse = "--- ${LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)} ${this.saksbehandlerInfo} ---\r\n" +
                "${nyBeskrivelse}\r\n\r\n" +
                eksisterendeBeskrivelse
        }
    }

    override fun somHttpEntity(): HttpEntity<*> {
        this.oppdaterBeskrivelse()
        return super.somHttpEntity()
    }

    private val eksisterendeBeskrivelse get() = oppgaveDataForHendelse?.beskrivelse ?: ""
    private val eksisterendeTilordnetRessurs get() = oppgaveDataForHendelse?.tilordnetRessurs
    private val eksisterendeTildeltEnhet get() = oppgaveDataForHendelse?.tildeltEnhetsnr
    private val eksisterendeOppgavetype get() = oppgaveDataForHendelse?.oppgavetype
    private val erOppgavetypeEndret get() = oppgavetype != null && (eksisterendeOppgavetype) != oppgavetype
    private val erTilordnetRessursEndretFraValgtTilIkkeValgt get() = eksisterendeTilordnetRessurs?.isNotEmpty() == true && tilordnetRessurs?.isEmpty() == true
    private val erEnhetEndret get() = tildeltEnhetsnr != null && (eksisterendeTildeltEnhet) != tildeltEnhetsnr
}

class UpdateOppgaveAfterOpprettRequest(var journalpostId: String) : PatchOppgaveRequest() {
    constructor(oppgaveDataForHendelse: OppgaveData, journalpostIdMedPrefix: String) : this(journalpostIdMedPrefix) {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
    }
}

class EndreForNyttDokumentRequest() : PatchOppgaveRequest() {
    constructor(oppgaveDataForHendelse: OppgaveData, journalpostHendelse: JournalpostHendelse) : this() {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
        this.beskrivelse = "--- ${LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)} ${journalpostHendelse.hentSaksbehandlerInfo()} ---\r\n" +
            "\u00B7 ${lagDokumentOppgaveTittel("Nytt dokument", journalpostHendelse.tittel ?: "", journalpostHendelse.dokumentDato!!)}\r\n" +
            "\u00B7 ${lagDokumenterVedlagtBeskrivelse(journalpostHendelse.journalpostId)}\r\n\r\n" +
            "${oppgaveDataForHendelse.beskrivelse}"
    }
}

class EndreMellomBidragFagomrader() : PatchOppgaveRequest() {
    constructor(
        oppgaveDataForHendelse: OppgaveData,
        saksbehandlersInfo: String,
        fagomradeGammelt: String? = null,
        fagomradeNy: String,
        overførTilFellesbenk: Boolean = false,
    ) : this() {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
        val dateFormatted = LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)
        this.beskrivelse = "--- $dateFormatted $saksbehandlersInfo ---\r\n"
        if (fagomradeGammelt.isNullOrEmpty()) {
            this.beskrivelse += "${"Fagområde endret til ${tilFagområdeBeskrivelse(fagomradeNy)}"}\r\n\r\n"
        } else if (fagomradeGammelt != fagomradeNy) {
            this.beskrivelse += "${"Fagområde endret til ${tilFagområdeBeskrivelse(fagomradeNy)} fra ${tilFagområdeBeskrivelse(fagomradeGammelt)}"}\r\n\r\n"
        }
        if (overførTilFellesbenk && !oppgaveDataForHendelse.tilordnetRessurs.isNullOrEmpty()) {
            this.tilordnetRessurs = ""
            this.beskrivelse += "${"Saksbehandler endret fra $saksbehandlersInfo til ikke valgt"}\r\n\r\n"
        }
        this.beskrivelse += oppgaveDataForHendelse.beskrivelse ?: ""
    }
}

class OverforOppgaveRequest(override var tildeltEnhetsnr: String?) : PatchOppgaveRequest() {
    override var tilordnetRessurs: String? = ""

    constructor(oppgaveDataForHendelse: OppgaveData, nyttEnhetsnummer: String, saksbehandlersInfo: String) : this(nyttEnhetsnummer) {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
        val dateFormatted = LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)
        this.beskrivelse = "--- $dateFormatted $saksbehandlersInfo ---\r\n" +
            "${"· Oppgave overført fra enhet ${oppgaveDataForHendelse.tildeltEnhetsnr} til $nyttEnhetsnummer"}\r\n\r\n" +
            "${"· Saksbehandler endret fra $saksbehandlersInfo til ikke valgt"}\r\n\r\n" +
            (oppgaveDataForHendelse.beskrivelse ?: "")
    }
}

class OppdaterOppgaveRequest(override var aktoerId: String?) : PatchOppgaveRequest() {
    constructor(oppgaveDataForHendelse: OppgaveData, aktoerId: String?) : this(aktoerId) {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
    }
}

class EndreTemaOppgaveRequest(override var tema: String?, override var tildeltEnhetsnr: String?) : PatchOppgaveRequest() {
    @JsonInclude(JsonInclude.Include.ALWAYS)
    override var tilordnetRessurs: String? = null

    constructor(
        oppgaveDataForHendelse: OppgaveData,
        tema: String?,
        tildeltEnhetsnr: String?,
        saksbehandlersInfo: String,
    ) : this(tema = tema, tildeltEnhetsnr = tildeltEnhetsnr) {
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
    constructor(oppgaveDataForHendelse: OppgaveData) : this(status = "FERDIGSTILT") {
        leggTilObligatoriskeVerdier(oppgaveDataForHendelse)
    }
}

enum class Prioritet {
    HOY, // , NORM, LAV
}

enum class OppgaveIdentType {
    AKTOERID,
    ORGNR,
    SAMHANDLERNR,
    BNR,
}

enum class OppgaveStatus {
    FERDIGSTILT,
    AAPNET,
    OPPRETTET,
    FEILREGISTRERT,
    UNDER_BEHANDLING,
}

enum class Oppgavestatuskategori {
    AAPEN,
    AVSLUTTET,
}

enum class OppgaveType(val description: String) {
    BEH_SAK("Behandle sak"),
    VUR("Vurder dokument"),
    JFR("Journalføring"),
    RETUR("Retur"),
    VURD_HENV("Vurder henvendelse"),
    ;

    companion object {
        fun descriptionFrom(value: String?): String {
            return OppgaveType.values().asList().find { it.name == value }?.description ?: value ?: "Ukjent"
        }
    }
}

internal fun lagDokumentOppgaveTittel(
    oppgaveNavn: String,
    dokumentbeskrivelse: String,
    dokumentdato: LocalDate,
) = "$oppgaveNavn ($dokumentbeskrivelse) mottatt ${dokumentdato.format(NORSK_DATO_FORMAT)}"

internal fun lagDokumenterVedlagtBeskrivelse(journalpostId: String) = "Dokumenter vedlagt: $journalpostId"

internal fun lagBeskrivelseHeader(saksbehandlersInfo: String): String {
    val dateFormatted = LocalDateTime.now().format(NORSK_TIDSSTEMPEL_FORMAT)
    return "--- $dateFormatted $saksbehandlersInfo ---\r\n"
}

private fun fieldToString(
    fieldName: String,
    value: String?,
) = if (value != null) "$fieldName=$value, " else ""
