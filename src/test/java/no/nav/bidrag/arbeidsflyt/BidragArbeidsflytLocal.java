package no.nav.bidrag.arbeidsflyt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BidragArbeidsflytLocal extends BidragArbeidsflyt {

    public static void main(String... args) {
        SpringApplication.run(BidragArbeidsflyt.class, args);
    }
}

