package com.rarible.protocol.nftorder.api.test

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.cloud.bootstrap.enabled=false"]
)
@ActiveProfiles("test")
@Import(value = [ApiTestConfiguration::class])
annotation class IntegrationTest