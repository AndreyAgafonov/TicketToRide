package ticketToRide

import io.ktor.http.ContentType
import kotlinx.css.*
import kotlinx.html.*

private const val GoogleMapsKey = "AIzaSyCdpAiP1sFvTVh7uPsCKoFuKsE1BYsY-Q0"

fun HTML.indexHtml() {
    head {
        title("Ticket to Ride!")
        styleBlock {
            rule("#loading") {
                width = 100.pct
                height = 100.pct
                textAlign = TextAlign.center
                verticalAlign = VerticalAlign.middle
            }
            rule("html, body") {
                height = 100.pct
                margin = "0"
                padding = "0"
            }
        }
    }
    body {
        div {
            id = "loading"
            +"Loading..."
        }
        script(src = "https://maps.googleapis.com/maps/api/js?key=${GoogleMapsKey}") {}
        script(src = "/ticket-to-ride.js") {}
    }
}

fun FlowOrMetaDataContent.styleBlock(builder: CSSBuilder.() -> Unit) {
    style(type = ContentType.Text.CSS.toString()) {
        unsafe {
            raw(CSSBuilder().apply(builder).toString())
        }
    }
}