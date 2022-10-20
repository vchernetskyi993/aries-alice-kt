package com.example

import io.javalin.Javalin
import kong.unirest.HttpRequestSummary
import kong.unirest.HttpResponse
import kong.unirest.Interceptor
import kong.unirest.MimeTypes
import kong.unirest.Unirest
import kong.unirest.UnirestInstance

fun main() {
    val server = startWebhookServer()
    val client = createAgentClient()

    cliLoop(client)

    server.stop()
    client.close()
}

fun startWebhookServer(): Javalin = Javalin.create()
    .post("/webhooks/topic/{topic}") { ctx ->
        println("Handling ${ctx.pathParam("topic")}...")
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
                    println("Error response: ${it.status} - ${it.body}")
                }
            }
        })
    return instance
}

fun receiveInvitation(client: UnirestInstance) {
    print("Input invitation: ")
    val invitation = readLine()
    client
        .post("/out-of-band/receive-invitation")
        .contentType(MimeTypes.JSON)
        .body(invitation)
        .asEmpty()
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
            "3" -> println("Sending message...")
            "4" -> receiveInvitation(client)
            "x", "X" -> break
            else -> println("Invalid input.")
        }
    }
}
