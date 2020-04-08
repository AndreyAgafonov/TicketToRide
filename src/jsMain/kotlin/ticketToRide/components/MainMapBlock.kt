package ticketToRide.components

import google.maps.*
import google.map.react.*
import kotlinext.js.*
import org.w3c.dom.*
import react.*
import ticketToRide.*
import kotlin.js.Promise
import kotlin.math.*

data class MainMapBlockProps(val gameMap: GameMap = GameMap): RProps
data class RouteLine(val from: String, val to: String, val color: Color, val segments: Int, val polyline: Polyline)

class MainMapBlock : RComponent<MainMapBlockProps, RState>() {
    private var routes: List<RouteLine> = emptyList()

    override fun RBuilder.render() {
        googleMap {
            attrs {
                center = GameMap.mapCenter.toGoogleMapCoords()
                zoom = GameMap.mapZoom
                googleMapLoader = { Promise.resolve(js("google.maps") as Any) }
                onGoogleApiLoaded = { maps -> drawRoutes(maps.map) }
                yesIWantToUseGoogleMapApiInternals = true
            }
            GameMap.cities.map {
                marker {
                    key = it.name
                    lat = it.latLng.lat
                    lng = it.latLng.lng
                }
            }
        }
    }

    private fun drawRoutes(map: Map<Element>) {
        val cityByName = GameMap.cities.associateBy { it.name }
        routes = GameMap.cities
            .flatMap { city ->
                city.routes.map { route ->
                    val dest =
                        cityByName[route.destination] ?: error("City ${route.destination} not present in game map")
                    val polyline = Polyline(jsObject {
                        this.map = map
                        path = arrayOf(city.latLng, dest.latLng)
                        strokeOpacity = 1
                        strokeColor = route.color.rgb
                        strokeWeight = 8
                        icons = arrayOf(jsObject {
                            icon = jsObject {
                                path = "M 1,0 -1,0"
                                strokeOpacity = 1
                                strokeColor = "#000000"
                                scale = 4
                            }
                            offset = "0px"
                            repeat = "${ceil(100f / route.segments)}%"
                        })
                    })
                    RouteLine(city.name, dest.name, route.color, route.segments, polyline)
                }
            }
    }
}

fun RBuilder.googleMap(block: RElementBuilder<Props>.() -> Unit): ReactElement {
    return child(GoogleMapReact::class, block)
}

fun LatLong.toGoogleMapCoords() = jsObject<Coords> {
    lat = this@toGoogleMapCoords.lat
    lng = this@toGoogleMapCoords.lng
}