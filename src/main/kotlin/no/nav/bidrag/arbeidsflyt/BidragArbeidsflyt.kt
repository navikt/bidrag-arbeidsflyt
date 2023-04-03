package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.commons.web.config.RestOperationsAzure
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import

const val PROFILE_NAIS = "nais"
const val PROFILE_KAFKA_TEST = "kafka_test"
val SECURE_LOGGER = LoggerFactory.getLogger("secureLogger")

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
@EnableAspectJAutoProxy
@Import(RestOperationsAzure::class)
class BidragArbeidsflyt

fun main(args: Array<String>) {
    val app = SpringApplication(BidragArbeidsflyt::class.java)
    app.setAdditionalProfiles(if (args.isEmpty()) PROFILE_NAIS else args[0])
    app.run(*args)
}
