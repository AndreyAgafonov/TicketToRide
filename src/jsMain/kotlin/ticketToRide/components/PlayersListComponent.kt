package ticketToRide.components

import com.ccfraser.muirwik.components.*
import kotlinx.css.*
import kotlinx.html.onMouseOut
import react.*
import react.dom.*
import styled.*
import ticketToRide.PlayerView

interface PlayersListProps : RProps {
    var players: List<PlayerView>
    var turn: Int
}

class PlayersList : RComponent<PlayersListProps, RState>() {
    override fun RBuilder.render() {
        for ((ix, player) in props.players.withIndex()) {
            mTooltip(if (player.away) "Disconnected" else "", TooltipPlacement.rightStart) {
                mPaper {
                    attrs {
                        elevation = 2
                    }
                    css {
                        +ComponentStyles.playerCard
                        color = if (player.away) Color.black.withAlpha(0.4) else Color.black
                        backgroundColor = Color(player.color.rgb).withAlpha(0.4)
                        if (ix == props.turn) {
                            borderColor = Color.red
                            borderStyle = BorderStyle.solid
                            borderWidth = 4.px
                        }
                    }
                    mTypography(variant = MTypographyVariant.h6) {
                        +player.name.value
                    }
                    styledDiv {
                        css { +ComponentStyles.playerCardIcons }
                        playerCardIcon("/icons/railway-car.png", player.carsLeft)
                        playerCardIcon("/icons/station.png", player.stationsLeft)
                        playerCardIcon("/icons/cards-deck.png", player.cardsOnHand)
                        playerCardIcon("/icons/ticket.png", player.ticketsOnHand)
                    }
                }
            }
        }
    }

    object ComponentStyles : StyleSheet("PlayersList", isStatic = true) {
        val playerCard by css {
            minHeight = 60.px
            display = Display.flex
            alignItems = Align.flexStart
            flexDirection = FlexDirection.column
            justifyContent = JustifyContent.spaceBetween
            borderRadius = 4.px
            margin = 4.px.toString()
            paddingTop = 4.px
            paddingBottom = 4.px
            paddingLeft = 12.px
            paddingRight = 12.px
        }
        val playerCardIcons by css {
            display = Display.flex
            flexDirection = FlexDirection.row
            alignItems = Align.center
            width = 100.pct
            justifyContent = JustifyContent.spaceEvenly
        }
    }
}

fun RBuilder.playerCardIcon(iconUrl: String, number: Int, style: RuleSet = {}) {
    styledImg {
        css {
            style()
        }
        attrs {
            src = iconUrl
            width = 24.px.toString()
        }
    }
    mTypography(variant = MTypographyVariant.body1) {
        +number.toString()
    }
}

fun RBuilder.playersList(players: List<PlayerView>, turn: Int) = child(PlayersList::class) {
    attrs {
        this.players = players
        this.turn = turn
    }
}