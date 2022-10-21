package com.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.Javalin
import kong.unirest.HttpRequestSummary
import kong.unirest.HttpResponse
import kong.unirest.Interceptor
import kong.unirest.MimeTypes
import kong.unirest.Unirest
import kong.unirest.UnirestInstance
import kong.unirest.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("main")
private val mapper: ObjectMapper = jacksonObjectMapper()

private var lastConnectionId: String? = null

fun main() {
    val client = createAgentClient()
    val server = startWebhookServer(client)

    cliLoop(client)

    server.stop()
    client.close()
}

private fun startWebhookServer(client: UnirestInstance): Javalin = Javalin.create()
    .post("/webhooks/topic/{topic}") { ctx ->
        val topic = ctx.pathParam("topic")
        val event: ObjectNode = mapper.readValue(ctx.body())
        logger.info("Event $topic: $event")
        when (topic) {
            "basicmessages" -> logger.info("Received message: ${event["content"]}")
            "issue_credential_v2_0" -> {
                val credExId = event["cred_ex_id"].asText()
                when (event["state"].asText()) {
                    "offer-received" -> client
                        .post("/issue-credential-2.0/records/${credExId}/send-request")
                        .asEmpty()
                }
            }

            "present_proof_v2_0" -> {
                when (event["state"].asText()) {
                    "request-received" -> {
                        val presExId = event["pres_ex_id"].asText()
                        val credentialsByReferent = credentialsByReferent(client, presExId)

                        val presentRequest = event["by_format"]["pres_request"]["indy"]

                        val predicates = presentRequest["requested_predicates"].fieldNames()
                            .asSequence()
                            .filter { it in credentialsByReferent }
                            .map {
                                it to mapOf(
                                    "cred_id" to credentialsByReferent[it]!!
                                        .getJSONObject("cred_info")
                                        .get("referent")
                                )
                            }
                            .toMap()

                        val revealed = presentRequest["requested_attributes"].fieldNames()
                            .asSequence()
                            .filter { it in credentialsByReferent }
                            .map {
                                it to mapOf(
                                    "cred_id" to credentialsByReferent[it]!!
                                        .getJSONObject("cred_info")
                                        .get("referent"),
                                    "revealed" to true,
                                )
                            }
                            .toMap()

                        val selfAttested = presentRequest["requested_attributes"].fieldNames()
                            .asSequence()
                            .filter { it !in credentialsByReferent }
                            .associateWith { "my self-attested value" }

                        val request = mapOf(
                            "indy" to mapOf(
                                "requested_predicates" to predicates,
                                "requested_attributes" to revealed,
                                "self_attested_attributes" to selfAttested,
                            )
                        )

                        logger.info("Sending presentation: $request")

                        client
                            .post("/present-proof-2.0/records/${presExId}/send-presentation")
                            .contentType(MimeTypes.JSON)
                            .body(request)
                            .asEmpty()
                    }
                }
            }
        }
    }
    .start(ServerConfig.port)

private fun createAgentClient(): UnirestInstance {
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

private fun cliLoop(client: UnirestInstance) {
    while (true) {
        print(
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

private fun sendMessage(client: UnirestInstance) {
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

private fun receiveInvitation(client: UnirestInstance) {
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

private fun credentialsByReferent(
    client: UnirestInstance,
    presExId: String,
): Map<String, JSONObject> {
    val credentials = client
        .get("/present-proof-2.0/records/$presExId/credentials")
        .asJson().body.array

    return credentials.asSequence()
        .map { it as JSONObject }
        .sortedBy {
            it
                .getJSONObject("cred_info")
                .getJSONObject("attrs")
                .getInt("timestamp")
        }
        .flatMap { row ->
            row.getJSONArray("presentation_referents").asSequence()
                .map { it as String }
                .distinct()
                .map { it to row }
        }
        .toMap()
}

