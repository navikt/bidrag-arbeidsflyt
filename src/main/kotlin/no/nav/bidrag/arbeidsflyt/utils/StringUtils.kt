package no.nav.bidrag.arbeidsflyt.utils

fun String.numericOnly(): String = this.replace(("[^\\d]").toRegex(), "")
