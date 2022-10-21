package com.example

private var lastConnectionId: String? = null

fun cliLoop(client: AgentClient) {
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

private fun sendMessage(client: AgentClient) {
    if (lastConnectionId == null) {
        print("To send messages you need to connect to other agent first.")
        return
    }
    print("Enter message: ")
    val message = readLine()
    client.sendMessage(lastConnectionId!!, message)
}

private fun receiveInvitation(client: AgentClient) {
    print("Input invitation: ")
    val invitation = readLine()
    lastConnectionId = client.receiveInvitation(invitation)
        .getString("connection_id")
}
