package no.nav.bidrag.arbeidsflyt.model

class OpprettOppgaveFeiletFunksjoneltException(message: String, throwable: Throwable): RuntimeException(message, throwable)
class EndreOppgaveFeiletFunksjoneltException(message: String, throwable: Throwable): RuntimeException(message, throwable)

class HentArbeidsfordelingFeiletFunksjoneltException(message: String, throwable: Throwable): RuntimeException(message, throwable)
class HentArbeidsfordelingFeiletTekniskException(message: String, throwable: Throwable): RuntimeException(message, throwable)
class HentPersonFeiletFunksjoneltException(message: String, throwable: Throwable): RuntimeException(message, throwable)
class KunneIkkeProsessereKafkaMelding(message: String): RuntimeException(message)