package com.rarible.protocol.union.core.elasticsearch

interface EsRepository {
    suspend fun refresh()
    fun init()
}