package com.example

fun main() {
    AgentClient.new().use { client ->
        startWebhookServer(client).use {
            cliLoop(client)
        }
    }
}
