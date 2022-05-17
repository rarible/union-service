package com.rarible.protocol.union.search.indexer.repository

import com.rarible.protocol.union.core.elasticsearch.EsMetadataRepository
import com.rarible.protocol.union.core.model.EsMetadata
import com.rarible.protocol.union.core.SearchConfiguration
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsMetadataRepositoryFt {

    @Autowired
    protected lateinit var repository: EsMetadataRepository

    @Test
    fun `should save and read`(): Unit = runBlocking {
        val metadata = EsMetadata(
            "activity_mapping", "{ 123 }",
        )

        repository.save(metadata)
        val found = repository.findById("activity_mapping")
        assertThat(found).isEqualTo(metadata)
    }
}
