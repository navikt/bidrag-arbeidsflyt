package no.nav.bidrag.arbeidsflyt

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

const val LIVE = "live"

@SpringBootApplication
@EnableAspectJAutoProxy
class BidragArbeidsflyt

fun main(args: Array<String>) {
    val app = SpringApplication(BidragArbeidsflyt::class.java)
    app.setAdditionalProfiles(if (args.isEmpty()) LIVE else args[0])
    app.run(*args)
}
