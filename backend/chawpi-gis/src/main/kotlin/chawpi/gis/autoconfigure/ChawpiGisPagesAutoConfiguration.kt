package chawpi.gis.autoconfigure

import chawpi.gis.MapPageComponent
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

// the MAP component, only when chawpi-pages is on the classpath (named as a string, so nothing
// loads a pages class otherwise) and gis itself is switched on. pages collects providers lazily,
// so no order against its auto-config is needed.
@AutoConfiguration
@ConditionalOnClass(name = ["chawpi.pages.PageComponentProvider"])
@ConditionalOnProperty(prefix = "chawpi.gis", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ChawpiGisPagesAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun mapPageComponent(): MapPageComponent = MapPageComponent()
}
