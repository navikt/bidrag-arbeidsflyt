package no.nav.bidrag.arbeidsflyt

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

const val LIVE = "live"
const val TEST = "test"

@SpringBootApplication
class BidragArbeidsflyt

fun main(args: Array<String>) {
	println(">>>>>>   args.isEmpty(): ${args.isEmpty()} -> ${args.joinToString { "," }}   <<<<<<")
    val app = SpringApplication(BidragArbeidsflyt::class.java)
    app.setAdditionalProfiles(if (args.isEmpty()) LIVE else args[0])
    app.run(*args)
}
