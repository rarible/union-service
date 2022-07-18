package com.rarible.protocol.union.integration.aptos.controller

import com.rarible.protocol.union.integration.aptos.service.AptosUpdateService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AptosUpdateController(
    private val updateService: AptosUpdateService
) {

    @PostMapping("/v0.1/aptos/update")
    suspend fun update(@RequestBody request: AptosUpdateRequest): ResponseEntity<String> {
        updateService.updateTokens(request.tokens)
        updateService.updateOwnerships(request.ownerships)
        updateService.updateCollections(request.collections)
        return ResponseEntity.ok("OK")
    }
}
