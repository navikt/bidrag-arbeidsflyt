package no.nav.bidrag.arbeidsflyt

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Profile

@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ServletWebSecurityAutoConfiguration::class,
    ],
)
@EnableAspectJAutoProxy
@Profile("local")
class BidragArbeidsflytLocal

fun main(args: Array<String>) {
    val app = SpringApplication(BidragArbeidsflytLocal::class.java)
    app.setAdditionalProfiles(PROFILE_KAFKA_TEST, PROFILE_NAIS, "live", "local", "lokal-nais-secrets", "local-kafka")
    app.run(*args)
}
