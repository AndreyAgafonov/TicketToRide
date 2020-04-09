package ticketToRide.components.screens

import com.ccfraser.muirwik.components.*
import com.ccfraser.muirwik.components.button.*
import com.ccfraser.muirwik.components.dialog.*
import react.*
import react.dom.a
import react.dom.p
import styled.*
import ticketToRide.*
import kotlin.browser.window

external interface ShowGameIdScreenProps : RProps {
    var gameId: GameId
    var onClosed: () -> Unit
}

class ShowGameIdScreen : RComponent<ShowGameIdScreenProps, RState>() {
    override fun RBuilder.render() {
        val linkUrl = "${window.location.origin}?game=${props.gameId.value}"
        window.navigator.clipboard.writeText(linkUrl)
        mDialog {
            css {
                +WelcomeScreen.ComponentStyles.welcomeDialog
            }
            attrs {
                open = true
                maxWidth = "sm"
                fullWidth = true
            }
            mDialogContent {
                p {
                    +"Send this link to other players (it is already in your clipboard)"
                }
                p {
                    a {
                        attrs {
                            href = linkUrl
                            target = "_blank"
                        }
                        +linkUrl
                    }
                }
            }
            mDialogActions {
                mButton("OK", MColor.primary, MButtonVariant.contained, onClick = { props.onClosed() })
            }
        }
    }
}