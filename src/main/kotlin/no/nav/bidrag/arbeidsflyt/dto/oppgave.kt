package no.nav.bidrag.arbeidsflyt.dto

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.bidrag.arbeidsflyt.model.DetaljVerdi.FAGOMRADE_BIDRAG
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class OppgaveSokRequest(val journalpostId: String, val fagomrade: String)
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
)


data class OpprettOppgaveRequest(var journalpostId: String, var aktoerId: String? = null, var tema: String? = "BID") {
    var oppgavetype: String = "JFR"
    var prioritet: String = Prioritet.HOY.name
    var aktivDato: String = LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd"))
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
    var aktoerId: String? = null
    var endretAvEnhetsnr: String? = null
    var oppgavetype: String? = null
    var prioritet: String? = null
    open var status: String? = null
    open var tema: String? = null
    open var tildeltEnhetsnr: String? = null

    fun leggOppgaveIdPa(contextUrl: String) = "$contextUrl/${id}".replace("//", "/")
    fun somHttpEntity(): HttpEntity<*> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        return HttpEntity<PatchOppgaveRequest>(this, headers)
    }

    protected fun leggTilVerdierSomIkkeErOverlasta(oppgaveData: OppgaveData) {
        id = oppgaveData.id ?: -1
        versjon = oppgaveData.versjon ?: -1

        if (er(UpdateOppgaveAfterOpprettRequest::class.java)){
            return
        }

        // FIXME: Tor Egil: Er dette nødvendig? Det gjøres en PATCH kall til oppgave og da er det bare versjon og id som er obligatorisk å sende med.
        aktoerId = oppgaveData.aktoerId
        endretAvEnhetsnr = oppgaveData.endretAvEnhetsnr
        oppgavetype = oppgaveData.oppgavetype
        prioritet = oppgaveData.prioritet ?: Prioritet.HOY.name

        if (erIkke(FerdigstillOppgaveRequest::class.java)) {
            status = oppgaveData.status
            tema = oppgaveData.tema ?: FAGOMRADE_BIDRAG
        }

        if (erIkke(FerdigstillOppgaveRequest::class.java, OverforOppgaveRequest::class.java)) {
            tildeltEnhetsnr = oppgaveData.tildeltEnhetsnr
        }
    }

    private fun er(javaklasse: Class<*>, vararg andreKlasser: Class<*>): Boolean {
        return this.javaClass == javaklasse && erHellerIkke(andreKlasser)
    }

    private fun erIkke(javaklasse: Class<*>, vararg andreKlasser: Class<*>): Boolean {
        return this.javaClass != javaklasse && erHellerIkke(andreKlasser)
    }

    private fun erHellerIkke(andreKlasser: Array<out Class<*>>): Boolean {
        if (andreKlasser.isEmpty()) {
            return true
        }

        for (klasse in andreKlasser) {
            if (klasse == this.javaClass) return false
        }

        return true
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
}

data class UpdateOppgaveAfterOpprettRequest(var journalpostId: String) : PatchOppgaveRequest() {
    constructor(oppgaveData: OppgaveData, journalpostIdMedPrefix: String) : this(journalpostIdMedPrefix) {
        leggTilVerdierSomIkkeErOverlasta(oppgaveData)
    }
}

data class OverforOppgaveRequest(override var tildeltEnhetsnr: String?) : PatchOppgaveRequest() {

    constructor(oppgaveData: OppgaveData, nyttEnhetsnummer: String) : this(nyttEnhetsnummer) {
        leggTilVerdierSomIkkeErOverlasta(oppgaveData)
    }

    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode() = super.hashCode()
}

data class FerdigstillOppgaveRequest(
    override var tema: String?,
    override var status: String?,
    override var tildeltEnhetsnr: String?
) : PatchOppgaveRequest() {

    constructor(
        oppgaveData: OppgaveData,
        tema: String,
        tildeltEnhetsnr: String?
    ) : this(status = "FERDIGSTILLT", tema = tema, tildeltEnhetsnr = tildeltEnhetsnr) {
        leggTilVerdierSomIkkeErOverlasta(oppgaveData)
    }

    override fun equals(other: Any?) = super.equals(other)
    override fun hashCode() = super.hashCode()
}

enum class Prioritet {
    HOY //, NORM, LAV
}
