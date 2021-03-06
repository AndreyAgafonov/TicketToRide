package ticketToRide.components

import react.*
import ticketToRide.GameStateView
import ticketToRide.Locale
import ticketToRide.playerState.PlayerState

interface ComponentBaseProps : RProps {
    var locale: Locale
    var playerState: PlayerState
    var gameState: GameStateView
    var onAction: (PlayerState) -> Unit
}

abstract class ComponentBase<P, S> : RComponent<P, S> where P : ComponentBaseProps, S : RState {

    constructor() : super()

    private val gameState get() = props.gameState
    open val playerState get() = props.playerState

    val players get() = gameState.players
    val me get() = gameState.me
    val lastRound get() = gameState.lastRound
    val turn get() = gameState.turn
    val myTurn get() = gameState.myTurn
    val myCards get() = gameState.myCards
    val myTickets get() = gameState.myTicketsOnHand
    val openCards get() = gameState.openCards
    val canPickCards get() = myTurn && playerState !is PlayerState.ChoosingTickets

    protected fun act(block: PlayerState.() -> PlayerState) =
        props.onAction(playerState.block())
}

inline fun <reified T : ComponentBase<P, *>, P : ComponentBaseProps> RBuilder.componentBase(
    props: ComponentBaseProps,
    crossinline builder: P.() -> Unit = {}
) =
    child(T::class) {
        attrs {
            this.locale = props.locale
            this.gameState = props.gameState
            this.playerState = props.playerState
            this.onAction = props.onAction
            builder()
        }
    }