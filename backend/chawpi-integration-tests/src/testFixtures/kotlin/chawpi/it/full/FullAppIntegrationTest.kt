package chawpi.it.full

import chawpi.test.ChawpiIntegrationTest
import org.springframework.test.context.TestPropertySource
import java.lang.annotation.Inherited

// the full app's test properties, in one place: FullAppBootTest cannot extend FullAppIntegrationTest
// (it reuses the slice checks) but must boot the same context. no background drain: the tests drive
// the automation runner by hand, as the original's did. geoserver stays out of the suite; a real
// publish is checked by hand.
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@TestPropertySource(
    properties = [
        "chawpi.automation.poll-interval=0s",
        "chawpi.gis.geoserver.enabled=false",
        "chawpi.gis.geoserver.url=http://geoserver.invalid:8081/geoserver"
    ]
)
annotation class FullAppProperties

// base of every ported api test
@FullAppProperties
abstract class FullAppIntegrationTest : ChawpiIntegrationTest()
