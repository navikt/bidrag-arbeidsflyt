FROM navikt/java:16
LABEL maintainer="Team Bidrag" \
      email="bidrag@nav.no"

COPY ./target/bidrag-arbeidsflyt-*.jar app.jar

EXPOSE 8080
