package no.nav.bidrag.arbeidsflyt.service

import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

@Component
class AppContext : ApplicationContextAware {
    @Throws(BeansException::class)
    override fun setApplicationContext(argApplicationContext: ApplicationContext) {
        applicationContext = argApplicationContext
    }

    companion object {
        var applicationContext: ApplicationContext? = null
            private set

        fun <T> getBean(clazz: Class<T>): T = applicationContext!!.getBean(clazz)
    }
}
