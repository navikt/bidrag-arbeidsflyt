package no.nav.bidrag.arbeidsflyt.persistence.entity

import no.nav.bidrag.arbeidsflyt.model.Fagomrade
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class Journalpost (
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0,

    @Column(name = "journalpost_id")
    var journalpostId: String,

    @Column(name = "status")
    var status: String,

    @Column(name = "enhet")
    var enhet: String,

    @Column(name = "tema")
    var tema: String,

    @Column(name = "gjelder_id")
    var gjelderId: String?
) {
    internal val erStatusMottatt get() = status == "M"
    internal val erBidragFagomrade get() = tema == Fagomrade.BIDRAG || tema == Fagomrade.FARSKAP
}