package no.nav.bidrag.arbeidsflyt

import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
@EnableAspectJAutoProxy
class BidragArbeidsflytLocal

fun main(args: Array<String>) {
    val app = SpringApplication(BidragArbeidsflytTest::class.java)
    app.setAdditionalProfiles(PROFILE_KAFKA_TEST, PROFILE_NAIS, "live", "local")
    app.run(*args)
}
