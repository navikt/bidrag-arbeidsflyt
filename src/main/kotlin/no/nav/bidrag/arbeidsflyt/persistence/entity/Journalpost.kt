package no.nav.bidrag.arbeidsflyt.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import no.nav.bidrag.dokument.dto.JournalpostStatus

@Entity
data class Journalpost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Int = 0,

    @Column(name = "journalpost_id")
    val journalpostId: String,

    @Column(name = "status")
    val status: String,

    @Column(name = "enhet")
    val enhet: String,

    @Column(name = "tema")
    val tema: String
) {
    internal val erStatusMottatt get() = status == "M" || status == JournalpostStatus.MOTTATT.name
    internal val erBidragFagomrade get() = tema == Fagomrade.BIDRAG || tema == Fagomrade.FARSKAP
}
