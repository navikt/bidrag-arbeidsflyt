package no.nav.bidrag.arbeidsflyt.logging

import com.fasterxml.jackson.core.JsonStreamContext
import net.logstash.logback.mask.ValueMasker
import java.util.regex.Matcher
import java.util.regex.Pattern


class SensitiveLogMasker : ValueMasker {

    companion object {
        private val FNR_PATTERN: Pattern = "(?<![0-9])[0-9]{11,14}(?![0-9])".toPattern()
    }

    override fun mask(p0: JsonStreamContext?, p1: Any?): Any? {
        return (if (p1 is CharSequence) {
            maskLogMessage(p1)
        } else {
            p1
        })
    }

    fun maskLogMessage(logMessage: CharSequence?): String {
        val sb = StringBuilder(logMessage)
        maskFnr(sb)
        return sb.toString()
    }

    private fun maskFnr(sb: StringBuilder) {
        val matcher: Matcher = FNR_PATTERN.matcher(sb)
        while (matcher.find()) {
            mask(sb, matcher.start(), 8)
            matcher.start()
        }
    }

    private fun mask(sb: StringBuilder, start: Int, len: Int) {
        val end = start + len - 1
        for (i in start until end) {
            sb.setCharAt(i, '*')
        }
    }


}