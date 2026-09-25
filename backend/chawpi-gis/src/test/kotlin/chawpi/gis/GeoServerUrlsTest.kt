package chawpi.gis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val BASE = "http://localhost:8081/geoserver"

class GeoServerUrlsTest {
    @Test
    fun `rest urls follow the workspace datastore featuretype hierarchy`() {
        assertThat(GeoServerUrls.workspaces(BASE)).isEqualTo("$BASE/rest/workspaces")
        assertThat(GeoServerUrls.workspace(BASE, "chawpi")).isEqualTo("$BASE/rest/workspaces/chawpi")
        assertThat(GeoServerUrls.dataStores(BASE, "chawpi")).isEqualTo("$BASE/rest/workspaces/chawpi/datastores")
        assertThat(GeoServerUrls.dataStore(BASE, "chawpi", "chawpi-postgis"))
            .isEqualTo("$BASE/rest/workspaces/chawpi/datastores/chawpi-postgis")
        assertThat(GeoServerUrls.featureTypes(BASE, "chawpi", "chawpi-postgis"))
            .isEqualTo("$BASE/rest/workspaces/chawpi/datastores/chawpi-postgis/featuretypes")
        assertThat(GeoServerUrls.featureType(BASE, "chawpi", "chawpi-postgis", "predio__00000000"))
            .isEqualTo("$BASE/rest/workspaces/chawpi/datastores/chawpi-postgis/featuretypes/predio__00000000")
    }

    @Test
    fun `service urls hang off the workspace, except wmts which is global`() {
        assertThat(GeoServerUrls.wms(BASE, "chawpi")).isEqualTo("$BASE/chawpi/wms")
        assertThat(GeoServerUrls.wfs(BASE, "chawpi")).isEqualTo("$BASE/chawpi/wfs")
        assertThat(GeoServerUrls.wmts(BASE)).isEqualTo("$BASE/gwc/service/wmts")
    }

    @Test
    fun `layer urls qualify the layer with the workspace`() {
        assertThat(GeoServerUrls.wmsLayer(BASE, "chawpi", "predio__00000000"))
            .isEqualTo("$BASE/chawpi/wms?service=WMS&version=1.3.0&request=GetMap&layers=chawpi:predio__00000000")
        assertThat(GeoServerUrls.wfsLayer(BASE, "chawpi", "predio__00000000"))
            .isEqualTo("$BASE/chawpi/wfs?service=WFS&version=2.0.0&request=GetFeature&typeNames=chawpi:predio__00000000")
    }

    @Test
    fun `a configured trailing slash does not double up`() {
        val properties = GeoServerProperties(url = "http://geoserver:8080/geoserver/")
        assertThat(properties.baseUrl).isEqualTo("http://geoserver:8080/geoserver")
        assertThat(GeoServerUrls.workspaces(properties.baseUrl)).isEqualTo("http://geoserver:8080/geoserver/rest/workspaces")
    }
}
