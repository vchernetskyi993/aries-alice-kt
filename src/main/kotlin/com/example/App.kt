package com.example

import io.javalin.Javalin
import kong.unirest.HttpRequestSummary
import kong.unirest.HttpResponse
import kong.unirest.Interceptor
import kong.unirest.MimeTypes
import kong.unirest.Unirest
import kong.unirest.UnirestInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("main")

private var lastConnectionId: String? = null

fun main() {
    val server = startWebhookServer()
    val client = createAgentClient()

    cliLoop(client)

    server.stop()
    client.close()
}

fun startWebhookServer(): Javalin = Javalin.create()
    .post("/webhooks/topic/{topic}") { ctx ->
        val topic = ctx.pathParam("topic")
        val event = ctx.body()
        logger.info("Event $topic: $event")
        // TODO: receive message
        // TODO: handle issued credential
        // TODO: handle proof request
    }
    .start(ServerConfig.port)

fun createAgentClient(): UnirestInstance {
    val instance = Unirest.spawnInstance()
    instance.config()
        .defaultBaseUrl(AgentConfig.url)
        .setDefaultHeader("X-API-KEY", AgentConfig.apiKey)
        .interceptor(object : Interceptor {
            override fun onResponse(
                response: HttpResponse<*>?,
                request: HttpRequestSummary?,
                config: kong.unirest.Config?
            ) {
                response?.ifFailure {
                    logger.error("Error response: ${it.status} - ${it.body}")
                }
            }
        })
    return instance
}

fun cliLoop(client: UnirestInstance) {
    while (true) {
        println(
            """
            
            (3) Send Message
            (4) Input New Invitation
            (X) Exit?
            [3/4/X]
        """.trimIndent()
        )
        when (readLine()) {
            "3" -> sendMessage(client)
            "4" -> receiveInvitation(client)
            "x", "X" -> break
            else -> println("Invalid input.")
        }
    }
}

fun sendMessage(client: UnirestInstance) {
    if (lastConnectionId == null) {
        print("To send messages you need to connect to other agent first.")
        return
    }
    print("Enter message: ")
    val message = readLine()
    client.post("/connections/${lastConnectionId}/send-message")
        .body(mapOf("content" to message))
        .asEmpty()
}

fun receiveInvitation(client: UnirestInstance) {
    print("Input invitation: ")
    val invitation = readLine()
    lastConnectionId = client
        .post("/out-of-band/receive-invitation")
        .contentType(MimeTypes.JSON)
        .body(invitation)
        .asJson()
        .body
        .`object`
        .getString("connection_id")
}

