package no.nav.bidrag.arbeidsflyt.model

import org.springframework.http.HttpStatus

abstract class BidragHttpStatusException(message: String, throwable: Throwable? = null) : RuntimeException(message, throwable) {
    abstract val status: HttpStatus
}

open class FunksjonellFeilException(message: String, throwable: Throwable? = null) : BidragHttpStatusException(message, throwable) {
    override val status: HttpStatus get() = HttpStatus.BAD_REQUEST
}

open class TekniskFeilException(message: String, throwable: Throwable) : BidragHttpStatusException(message, throwable) {
    override val status: HttpStatus get() = HttpStatus.INTERNAL_SERVER_ERROR
}

class OpprettOppgaveFeiletFunksjoneltException(message: String, throwable: Throwable): FunksjonellFeilException(message, throwable)