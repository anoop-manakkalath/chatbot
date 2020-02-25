package com.bot.chat

import com.bot.chat.entity.Answer
import org.springframework.util.MimeTypeUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/v1")
class Endpoint {

    @GetMapping("/getAnswer")
    fun getAnswer(@RequestParam question: String): Answer {
        var answer: Pair<String, Boolean> = openNLPChatBot.answer(question)
        return Answer(answer.first, answer.second);
    }
}