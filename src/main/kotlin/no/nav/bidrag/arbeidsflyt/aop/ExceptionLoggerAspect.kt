package no.nav.bidrag.arbeidsflyt.aop

import no.nav.bidrag.commons.ExceptionLogger
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class ExceptionLoggerAspect(private val exceptionLogger: ExceptionLogger) {
    @AfterThrowing(pointcut = "within (no.nav.bidrag.arbeidsflyt.service..*)", throwing = "exception")
    fun logException(joinPoint: JoinPoint, exception: Exception?) {
        exceptionLogger.logException(exception, "${joinPoint.sourceLocation.withinType}")
    }
}
