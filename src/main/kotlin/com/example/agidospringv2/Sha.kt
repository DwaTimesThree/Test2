package com.example.agidospringv2

import com.google.common.hash.Hashing
import java.nio.charset.StandardCharsets
import java.util.*

class Sha {
   fun calculateSH256(secret: String): String {
        val sha256hex = Hashing.sha256()
                .hashString(secret, StandardCharsets.UTF_8)
                .asBytes()

        return Base64.getEncoder().encodeToString(sha256hex)
    }
}