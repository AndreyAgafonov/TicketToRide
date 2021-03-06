package ticketToRide

import io.ktor.application.*
import io.ktor.features.CachingHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.html.respondHtml
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.net.InetAddress

class PlayerConnection(
    val gameId: GameId,
    val name: PlayerName,
    private val ws: WebSocketSession
) {
    suspend fun send(resp: Response) = ws.send(json.stringify(Response.serializer(), resp))
    suspend fun ping() = ws.send(Response.Pong)
}

private val games = mutableMapOf<GameId, Game>()
private val rootScope = CoroutineScope(Dispatchers.Default + Job())
private val json = Json(JsonConfiguration.Default.copy(allowStructuredMapKeys = true))
private val logger = KotlinLogging.logger("Server")

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    val googleApiKey = environment.config.property("google-api-key").getString()
    val host = environment.config.property("ktor.deployment.host").getString()
    val redis = environment.config.propertyOrNull("redis.host")?.let {
        RedisCredentials(
            it.getString(),
            environment.config.property("redis.port").getString().toInt(),
            environment.config.propertyOrNull("redis.password")?.getString()
        )
    }
    val isLoopbackAddress = InetAddress.getByName(host).isLoopbackAddress

    install(WebSockets)
    install(ContentNegotiation) { json(json) }
    install(CachingHeaders) {
        options { outgoingContent ->
            outgoingContent.contentType?.withoutParameters()?.let {
                if (it == ContentType.Application.JavaScript || it.contentType == ContentType.Image.Any.contentType)
                    CachingOptions(
                        CacheControl.MaxAge(
                            maxAgeSeconds = 24 * 60 * 60 * 30,
                            visibility = CacheControl.Visibility.Public,
                            mustRevalidate = true
                        )
                    )
                else null
            }
        }
    }
    routing {
        static {
            resource("ticket-to-ride.js")
            resource("favicon.ico")
        }
        static("icons") { resources("icons") }
        static("cards") { resources("cards") }
        static("images") { resources("images") }

        get("/") {
            call.respondHtml { indexHtml(googleApiKey, isLoopbackAddress) }
        }
        get("/game/{gameId}") {
            call.respondHtml { indexHtml(googleApiKey, isLoopbackAddress) }
        }

        route("/internal") {
            get("/games") {
                call.respond(games.entries.associate { it.key.value to it.value.playerNames })
            }
            get("/game/{id}") {
                call.parameters["id"]?.let { id ->
                    games[GameId(id)]?.let { game ->
                        call.respond(game.getState())
                    }
                }
            }
        }

        webSocket("ws") {
            var connection: PlayerConnection? = null
            incoming.consumeAsFlow()
                .mapNotNull { (it as? Frame.Text)?.readText() }
                .filter {
                    if (it == Request.Ping) send(Response.Pong)
                    it != Request.Ping
                }
                .mapNotNull { req ->
                    kotlin.runCatching { json.parse(Request.serializer(), req) }.also {
                        logger.info { "Received $req from ${connection?.name?.value}" }
                        it.exceptionOrNull()?.let { e ->
                            logger.warn(e) { "Failed to deserialize request: $req" }
                            connection?.let { conn -> games[conn.gameId]?.leavePlayer(conn) }
                            close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Bad request"))
                        }
                    }.getOrNull()
                }
                .collect { req ->
                    when (req) {

                        is StartGameRequest -> {
                            val game = Game(req.carsCount, redis) { games.remove(it.id) }
                            games[game.id] = game
                            connection = PlayerConnection(game.id, req.playerName, this).also { conn ->
                                rootScope.launch { game.start(conn) }
                            }
                        }

                        is JoinGameRequest -> {
                            val conn = PlayerConnection(req.gameId, req.playerName, this)
                            games[req.gameId]?.let {
                                if (it.joinPlayer(req, conn)) connection = conn
                                else conn.send(Response.ErrorMessage("Name is taken"))
                            } ?: redis?.loadFromRedis(conn.gameId)?.let { gameState ->
                                val game = Game(gameState.initialCarsCount, redis) { games.remove(conn.gameId) }
                                games[conn.gameId] = game
                                rootScope.launch { game.restore(conn, gameState) }
                                connection = conn
                            } ?: conn.send(Response.ErrorMessage("No such game"))
                        }

                        is LeaveGameRequest -> {
                            connection?.let { games[it.gameId]?.leavePlayer(it) }
                            close(CloseReason(CloseReason.Codes.NORMAL, "Exit game"))
                        }

                        is ChatMessageRequest ->
                            connection?.let { conn ->
                                games[conn.gameId]?.let {
                                    it.sendToAll { Response.ChatMessage(conn.name, req.message) }
                                }
                            }

                        is GameRequest ->
                            connection?.let { games[it.gameId]?.process(req, it) }
                    }
                }
        }
    }
}