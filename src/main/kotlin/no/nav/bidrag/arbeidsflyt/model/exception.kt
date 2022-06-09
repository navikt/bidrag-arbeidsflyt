package no.nav.bidrag.arbeidsflyt.model

class OpprettOppgaveFeiletFunksjoneltException(message: String, throwable: Throwable): RuntimeException(message, throwable)
class EndreOppgaveFeiletFunksjoneltException(message: String, throwable: Throwable): RuntimeException(message, throwable)

class HentGeografiskEnhetFeiletFunksjoneltException(message: String, throwable: Throwable): RuntimeException(message, throwable)
class HentGeografiskEnhetFeiletTekniskException(message: String, throwable: Throwable): RuntimeException(message, throwable)
class HentPersonFeiletFunksjoneltException(message: String, throwable: Throwable): RuntimeException(message, throwable)
class KunneIkkeProsessereKafkaMelding(message: String): RuntimeException(message)