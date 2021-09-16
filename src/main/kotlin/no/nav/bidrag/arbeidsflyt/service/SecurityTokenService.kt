package no.nav.bidrag.arbeidsflyt.service

import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.stereotype.Service

@Service
class SecurityTokenService(@Lazy val authorizedClientManager: OAuth2AuthorizedClientManager) {

    private val ANONYMOUS_AUTHENTICATION: Authentication = AnonymousAuthenticationToken(
        "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
    )

    fun generateBearerToken(clientRegistrationId: String): ClientHttpRequestInterceptor? {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray?, execution: ClientHttpRequestExecution ->
            val accessToken = authorizedClientManager
                .authorize(
                    OAuth2AuthorizeRequest
                        .withClientRegistrationId(clientRegistrationId)
                        .principal(ANONYMOUS_AUTHENTICATION)
                        .build()
                )!!.accessToken

            request.headers.setBearerAuth(accessToken.tokenValue)
            execution.execute(request, body!!)
        }
    }
}