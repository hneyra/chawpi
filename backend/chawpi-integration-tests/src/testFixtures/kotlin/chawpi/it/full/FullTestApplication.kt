package chawpi.it.full

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration

// the app every full-app suite boots: every starter on the classpath, auto-configuration and nothing
// else, the way a real app gets chawpi. no component scan, so no library class is picked up twice.
@SpringBootConfiguration
@EnableAutoConfiguration
class FullTestApplication
