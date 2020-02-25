package com.bot.chat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class ServerApplication {

    fun action(args: Array<String>) = runApplication<ServerApplication>(*args)
}

fun main(args: Array<String>) {
    ServerApplication().action(args);
}
