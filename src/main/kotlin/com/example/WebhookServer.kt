package com.example

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.Javalin
import kong.unirest.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger("webhook")
private val mapper: ObjectMapper = jacksonObjectMapper()

fun startWebhookServer(client: AgentClient): Javalin = Javalin.create()
    .post("/webhooks/topic/{topic}") { ctx ->
        val topic = ctx.pathParam("topic")
        val event: ObjectNode = mapper.readValue(ctx.body())
        logger.info("Event $topic: $event")
        when (topic) {
            "basicmessages" -> logger.info("Received message: ${event["content"]}")
            "issue_credential_v2_0" -> handleIssueCredentials(client, event)
            "present_proof_v2_0" -> handlePresentProof(client, event)
        }
    }
    .start(ServerConfig.port)

private fun handleIssueCredentials(client: AgentClient, event: ObjectNode) {
    val credExId = event["cred_ex_id"].asText()
    when (event["state"].asText()) {
        "offer-received" -> client.requestCredential(credExId)
    }
}

private fun handlePresentProof(client: AgentClient, event: ObjectNode) {
    when (event["state"].asText()) {
        "request-received" -> {
            val presExId = event["pres_ex_id"].asText()
            val credentialsByReferent = credentialsByReferent(client, presExId)

            val presentRequest = event["by_format"]["pres_request"]["indy"]

            client.sendPresentation(
                presExId,
                mapOf(
                    "indy" to mapOf(
                        "requested_predicates" to predicates(presentRequest, credentialsByReferent),
                        "requested_attributes" to revealed(presentRequest, credentialsByReferent),
                        "self_attested_attributes" to selfAttested(presentRequest, credentialsByReferent),
                    )
                ),
            )
        }
    }
}

private fun credentialsByReferent(
    client: AgentClient,
    presExId: String,
): Map<String, JSONObject> =
    client.fetchCredentials(presExId).asSequence()
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

private fun predicates(
    presentRequest: JsonNode,
    credentialsByReferent: Map<String, JSONObject>,
): Map<String, Map<String, String>> =
    presentRequest["requested_predicates"].fieldNames()
        .asSequence()
        .filter { it in credentialsByReferent }
        .map {
            it to mapOf(
                "cred_id" to credentialsByReferent[it]!!
                    .getJSONObject("cred_info")
                    .getString("referent")
            )
        }
        .toMap()

private fun revealed(
    presentRequest: JsonNode,
    credentialsByReferent: Map<String, JSONObject>,
): Map<String, Map<String, Any>> =
    presentRequest["requested_attributes"].fieldNames()
        .asSequence()
        .filter { it in credentialsByReferent }
        .map {
            it to mapOf(
                "cred_id" to credentialsByReferent[it]!!
                    .getJSONObject("cred_info")
                    .getString("referent"),
                "revealed" to true,
            )
        }
        .toMap()

private fun selfAttested(
    presentRequest: JsonNode,
    credentialsByReferent: Map<String, JSONObject>,
): Map<String, String> =
    presentRequest["requested_attributes"].fieldNames()
        .asSequence()
        .filter { it !in credentialsByReferent }
        .associateWith { "my self-attested value" }
