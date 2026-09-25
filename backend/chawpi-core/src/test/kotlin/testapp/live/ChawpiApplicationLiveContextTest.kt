package testapp.live

import chawpi.core.autoconfigure.ChawpiApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.stereotype.Component

// an app class in a package of its own, the way an app uses the annotation. auto-configuration is
// switched off (no database here): this pins what the annotation itself scans and binds.
@ChawpiApplication
class LiveApp

@ConfigurationProperties("live")
data class LiveProperties(
    val greeting: String = "hello"
)

@Component
class LiveService

class ChawpiApplicationLiveContextTest {
    private val runner =
        ApplicationContextRunner()
            .withUserConfiguration(LiveApp::class.java)
            .withPropertyValues("spring.boot.enableautoconfiguration=false", "live.greeting=hola")

    @Test
    fun `the app's own components and properties are picked up`() {
        runner.run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(LiveService::class.java)
            assertThat(context.getBean(LiveProperties::class.java).greeting).isEqualTo("hola")
        }
    }

    @Test
    fun `the scan starts at the app's package and never reaches chawpi`() {
        runner.run { context ->
            val fromLibrary =
                context.beanDefinitionNames.filter { name ->
                    context.getType(name)?.packageName?.startsWith("chawpi.") == true
                }
            assertThat(fromLibrary).isEmpty()
        }
    }
}
