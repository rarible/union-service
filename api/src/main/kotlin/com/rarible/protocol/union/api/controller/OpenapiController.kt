package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.UnionOpenapiReader
import org.springframework.core.io.InputStreamResource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/v0.1"])
class OpenapiController() {

    @GetMapping(
        value = ["/openapi.yaml"],
        produces = ["text/yaml"]
    )
    fun openapiYaml(): InputStreamResource {
        return InputStreamResource(UnionOpenapiReader.getOpenapi())
    }

    @GetMapping(
        value = ["/doc"],
        produces = ["text/html"]
    )
    fun doc(): InputStreamResource {
        val file = OpenapiController::class.java.getResourceAsStream("/redoc.html")
        return InputStreamResource(file)
    }
}
