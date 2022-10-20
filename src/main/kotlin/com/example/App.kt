package com.example

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.javalin.Javalin

object AppConfig {
    private val conf: Config = ConfigFactory.load()

    val port: Int = conf.getInt("server.port")
}

fun main() {
    val server = Javalin.create()
        .post("/webhooks/topic/{topic}/") { ctx ->
            ctx.result("Handling ${ctx.pathParam("topic")}...")
        }
        .start(AppConfig.port)

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