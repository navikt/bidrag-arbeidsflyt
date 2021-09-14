package no.nav.bidrag.arbeidsflyt

import no.nav.bidrag.arbeidsflyt.model.MiljoVariabler
import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.commons.web.CorrelationIdFilter
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.oauth2.client.*
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository


@Configuration
class SecurityConfiguration: WebSecurityConfigurerAdapter() {

    private val ANONYMOUS_AUTHENTICATION: Authentication = AnonymousAuthenticationToken(
            "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf()
                .disable()
                .authorizeRequests()
                .antMatchers("/actuator/**")
                .permitAll()
                .anyRequest()
                .fullyAuthenticated()
    }

    @Bean
    fun authorizedClientManager(
            clientRegistrationRepository: ClientRegistrationRepository?,
            authorizedClientService: OAuth2AuthorizedClientService?): OAuth2AuthorizedClientManager? {
        val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build()
        val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService)
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
        return authorizedClientManager
    }

    @Bean
    @Scope("prototype")
    fun restTemplate(authorizedClientManager: OAuth2AuthorizedClientManager): HttpHeaderRestTemplate? {
        val httpHeaderRestTemplate = HttpHeaderRestTemplate();
        httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER) { CorrelationId.fetchCorrelationIdForThread() }
        httpHeaderRestTemplate.interceptors.add(bearerToken("bidrag-arbeidsflyt", authorizedClientManager))
        return httpHeaderRestTemplate;
    }

    private fun bearerToken(clientRegistrationId: String,
                                 authorizedClientManager: OAuth2AuthorizedClientManager): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            val accessToken = authorizedClientManager
                    .authorize(OAuth2AuthorizeRequest
                            .withClientRegistrationId(clientRegistrationId)
                            .principal(ANONYMOUS_AUTHENTICATION)
                            .build())!!.accessToken
            request.headers.setBearerAuth(accessToken.tokenValue)
            execution.execute(request, body!!)
        }
    }
}