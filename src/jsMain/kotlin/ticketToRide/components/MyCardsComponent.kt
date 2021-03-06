package ticketToRide.components

import com.ccfraser.muirwik.components.MTypographyVariant
import com.ccfraser.muirwik.components.mTypography
import kotlinx.css.*
import react.RBuilder
import react.RState
import styled.css
import styled.styledDiv
import ticketToRide.Locale
import ticketToRide.LocalizedStrings

class MyCardsComponent : ComponentBase<ComponentBaseProps, RState>() {
    override fun RBuilder.render() {
        mTypography(str.header, variant = MTypographyVariant.h6) {
            css { paddingLeft = 10.px }
        }
        styledDiv {
            css {
                display = Display.flex
                flexDirection = FlexDirection.row
                flexWrap = FlexWrap.wrap
            }
            for ((card, count) in myCards.groupingBy { it }.eachCount()) {
                styledDiv {
                    css {
                        display = Display.flex
                        flexDirection = FlexDirection.row
                        flexWrap = FlexWrap.nowrap
                        alignItems = Align.center
                        margin = 12.px.toString()
                    }
                    myCard(card, props.locale)
                    styledDiv {
                        css {
                            put("font-size", "large")
                        }
                        +count.toString()
                    }
                }
            }
        }
    }

    private inner class Strings : LocalizedStrings({ props.locale }) {

        val header by loc(
            Locale.En to "My cards",
            Locale.Ru to "Мои карты"
        )
    }

    private val str = Strings()
}

fun RBuilder.myCards(props: ComponentBaseProps) = componentBase<MyCardsComponent, ComponentBaseProps>(props)