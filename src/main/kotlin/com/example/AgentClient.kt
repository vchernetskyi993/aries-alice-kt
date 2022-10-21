package com.example

import kong.unirest.HttpRequestSummary
import kong.unirest.HttpResponse
import kong.unirest.Interceptor
import kong.unirest.MimeTypes
import kong.unirest.Unirest
import kong.unirest.UnirestInstance
import kong.unirest.json.JSONArray
import kong.unirest.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class AgentClient(private val client: UnirestInstance) : AutoCloseable {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger("agent")

        fun new(): AgentClient {
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
            return AgentClient(instance)
        }
    }

    fun receiveInvitation(invitation: String?): JSONObject =
        client
            .post("/out-of-band/receive-invitation")
            .contentType(MimeTypes.JSON)
            .body(invitation)
            .asJson()
            .body
            .`object`

    fun sendMessage(connectionId: String, message: String?) {
        client
            .post("/connections/$connectionId/send-message")
            .body(mapOf("content" to message))
            .asEmpty()
    }

    fun requestCredential(credExId: String) {
        client
            .post("/issue-credential-2.0/records/$credExId/send-request")
            .asEmpty()
    }

    fun fetchCredentials(presExId: String): JSONArray =
        client
            .get("/present-proof-2.0/records/$presExId/credentials")
            .asJson().body.array

    fun sendPresentation(presExId: String, request: Map<String, *>) {
        logger.info("Sending presentation: $request")

        client
            .post("/present-proof-2.0/records/${presExId}/send-presentation")
            .contentType(MimeTypes.JSON)
            .body(request)
            .asEmpty()
    }

    override fun close() {
        client.close()
    }
}
