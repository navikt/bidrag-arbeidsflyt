package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData
import no.nav.bidrag.arbeidsflyt.dto.OppgaveHendelse
import no.nav.bidrag.arbeidsflyt.dto.OppgaveStatus
import no.nav.bidrag.arbeidsflyt.dto.OppgaveType
import no.nav.bidrag.arbeidsflyt.dto.Oppgavestatuskategori

data class OppgaveDataForHendelse(
    val id: Long,
    val versjon: Int,

    val ident: String? = null,
    val bnr: String? = null,
    val aktorId: String? = null,
    val journalpostId: String? = null,
    val behandlingstype: String? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val tema: String? = null,
    val tildeltEnhetsnr: String? = null,
    val beskrivelse: String? = null,
    val tilordnetRessurs: String? = null,
    val saksreferanse: String? = null,
    val statuskategori: Oppgavestatuskategori? = null,
    val status: OppgaveStatus? = null
) {
    constructor(oppgaveData: OppgaveData) : this(
        id = oppgaveData.id ?: -1,
        versjon = oppgaveData.versjon ?: -1,
        tema = oppgaveData.tema,
        bnr = oppgaveData.bnr,
        aktorId = oppgaveData.aktoerId,
        oppgavetype = oppgaveData.oppgavetype,
        tildeltEnhetsnr = oppgaveData.tildeltEnhetsnr,
        journalpostId = oppgaveData.journalpostId,
        beskrivelse = oppgaveData.beskrivelse,
        tilordnetRessurs = oppgaveData.tilordnetRessurs,
        saksreferanse = oppgaveData.saksreferanse,
        status = oppgaveData.status,
        statuskategori = oppgaveData.statuskategori,
        ident = oppgaveData.aktoerId
    )

    constructor(oppgaveHendelse: OppgaveHendelse) : this(
        id = oppgaveHendelse.id,
        versjon = oppgaveHendelse.versjon,
        tema = oppgaveHendelse.tema,
        bnr = oppgaveHendelse.hentBnr,
        aktorId = oppgaveHendelse.hentAktoerId,
        oppgavetype = oppgaveHendelse.oppgavetype,
        tildeltEnhetsnr = oppgaveHendelse.tildeltEnhetsnr,
        journalpostId = oppgaveHendelse.journalpostId,
        beskrivelse = oppgaveHendelse.beskrivelse,
        tilordnetRessurs = oppgaveHendelse.tilordnetRessurs,
        saksreferanse = oppgaveHendelse.saksreferanse,
        statuskategori = oppgaveHendelse.statuskategori,
        status = oppgaveHendelse.status,
        ident = oppgaveHendelse.hentIdent
    )

    fun erTemaBIDEllerFAR(): Boolean = tema == "BID" || tema == "FAR"
    fun erStatusFerdigstilt(): Boolean = status == OppgaveStatus.FERDIGSTILT
    fun erAapenJournalforingsoppgave(): Boolean = erStatusKategoriAapen && erJournalforingOppgave
    fun erAapenVurderDokumentOppgave(): Boolean = erStatusKategoriAapen && erVurderDokumentOppgave
    fun erAvsluttetJournalforingsoppgave(): Boolean = erStatusKategoriAvsluttet && erJournalforingOppgave

    internal val erStatusKategoriAapen get() = statuskategori == Oppgavestatuskategori.AAPEN
    internal val erStatusKategoriAvsluttet get() = statuskategori == Oppgavestatuskategori.AVSLUTTET
    internal val erJournalforingOppgave get() = oppgavetype == OppgaveType.JFR.name
    internal val erVurderDokumentOppgave get() = oppgavetype == OppgaveType.VUR.name
    internal val erBehandleDokumentOppgave get() = oppgavetype == OppgaveType.BEH_SAK.name
    internal val hasJournalpostId get() = !journalpostId.isNullOrEmpty()
    internal val journalpostIdUtenPrefix get() = if (harJournalpostIdPrefix() && hasJournalpostId) journalpostId!!.split('-')[1] else journalpostId
    internal fun harJournalpostIdPrefix() = hasJournalpostId && journalpostId!!.contains("-")
//    internal val journalpostIdMedPrefix get() = if (journalpostId == null) null else if(harJournalpostIdPrefix()) journalpostId else "JOARK-$journalpostId"

    internal val journalpostIdMedPrefix get() = if (journalpostId.isNullOrEmpty() || harJournalpostIdPrefix()) journalpostId else if (isBidJournalpostId(journalpostId)) "BID-$journalpostId" else "JOARK-$journalpostId"
}
