package no.nav.bidrag.arbeidsflyt

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

@Configuration
@Profile(PROFILE_LIVE)
class FlywayConfiguration @Autowired constructor(dataSource: DataSource?) {

    init {
        Thread.sleep(30000)
        Flyway.configure().dataSource(dataSource).load().migrate()
    }
}