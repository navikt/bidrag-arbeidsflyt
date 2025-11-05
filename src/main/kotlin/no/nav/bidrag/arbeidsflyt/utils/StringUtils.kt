package no.nav.bidrag.arbeidsflyt.utils

import no.nav.bidrag.transport.dokument.JournalpostHendelse

val JournalpostHendelse.enhetKonvertert get() = if (enhet == "2101") "4865" else enhet

fun String.numericOnly(): String = this.replace(("[^\\d]").toRegex(), "")
