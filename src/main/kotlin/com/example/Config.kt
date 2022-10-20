package com.example

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

private val conf: Config = ConfigFactory.load()

object ServerConfig {
    val port: Int = conf.getInt("server.port")
}

object AgentConfig {
    val url: String = conf.getString("agent.url")
    val apiKey: String = conf.getString("agent.api-key")
}
