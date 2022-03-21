package no.nav.bidrag.arbeidsflyt

import org.springframework.boot.SpringApplication
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class, ManagementWebSecurityAutoConfiguration::class])
@EnableAspectJAutoProxy
@ActiveProfiles(value = [PROFILE_KAFKA_TEST, "local"])
@Profile("local")
class BidragArbeidsflytLocal

fun main(args: Array<String>) {
    val app = SpringApplication(BidragArbeidsflytLocal::class.java)
    app.run(*args)
}
