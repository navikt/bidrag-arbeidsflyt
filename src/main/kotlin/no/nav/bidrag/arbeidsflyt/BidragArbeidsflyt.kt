package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.commons.service.AppContext
import no.nav.bidrag.commons.web.config.RestOperationsAzure
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
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
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@EnableAspectJAutoProxy
class BidragArbeidsflyt

fun main(args: Array<String>) {
    val app = SpringApplication(BidragArbeidsflyt::class.java)
    app.setAdditionalProfiles(if (args.isEmpty()) PROFILE_NAIS else args[0])
    app.run(*args)
}
