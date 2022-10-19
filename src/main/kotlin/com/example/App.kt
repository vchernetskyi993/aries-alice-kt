package com.example

import io.javalin.Javalin

fun main() {
    val server = Javalin.create()
        .post("/webhooks/topic/{topic}/") { ctx ->
            ctx.result("Handling ${ctx.pathParam("topic")}...")
        }
        // TODO: use conf
        .start(8032)

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
            "4" -> println("Waiting for invitation...")
            "x", "X" -> break
            else -> println("Invalid input.")
        }
    }

    server.stop()
}