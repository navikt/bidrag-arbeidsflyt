package no.nav.bidrag.arbeidsflyt.model

import no.nav.bidrag.arbeidsflyt.dto.OppgaveData

data class OppgaveDataForHendelse(
    val id: Long,
    val versjon: Int,

    val bnr: String? = null,
    val aktorId: String? = null,
    val journalpostId: String? = null,
    val oppgavetype: String? = null,
    val tema: String? = null,
    val tildeltEnhetsnr: String? = null,
    val beskrivelse: String? = null,
    val tilordnetRessurs: String? = null
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
        tilordnetRessurs = oppgaveData.tilordnetRessurs
    )
}
