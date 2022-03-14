package no.nav.bidrag.arbeidsflyt.persistence.entity

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

    @Column(name = "journalpostid")
    var journalpostId: String,

    @Column(name = "status")
    var status: String?
) {
    fun erStatusMottatt() = status == "M"
}