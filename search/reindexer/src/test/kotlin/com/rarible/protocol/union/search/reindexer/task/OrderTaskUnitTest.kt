package com.rarible.protocol.union.search.reindexer.task

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.union.api.client.OrderControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.OrdersDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.search.core.ElasticOrder
import com.rarible.protocol.union.search.core.converter.ElasticOrderConverter
import com.rarible.protocol.union.search.reindexer.config.SearchReindexerConfiguration
import com.rarible.protocol.union.search.reindexer.config.SearchReindexerProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import reactor.core.publisher.Mono
import java.math.BigInteger
import java.time.Instant

class OrderTaskUnitTest {

    val esOperations = mockk<ReactiveElasticsearchOperations> {
        every {
            save(any<Iterable<ElasticOrder>>())
        } answers { Mono.just(arg(0)) }
    }

    val converter = mockk<ElasticOrderConverter> {
        every {
            convert(any<OrderDto>())
        } returns ElasticOrder(
            orderId = randomOrderId().fullId(),
            lastUpdatedAt = Instant.now(),
            type = ElasticOrder.Type.SELL,
            blockchain = BlockchainDto.ETHEREUM,
            platform = PlatformDto.RARIBLE,
            maker = randomUnionAddress(),
            make = ElasticOrder.Asset(EthErc721AssetTypeDto(contract = randomContract(), tokenId = BigInteger.ZERO)),
            taker = randomUnionAddress(),
            take = ElasticOrder.Asset(EthEthereumAssetTypeDto()),
            start = null,
            end = null,
            origins = emptyList(),
            status =  OrderStatusDto.ACTIVE
        )
    }

    val orderClient = mockk<OrderControllerApi> {
        every {
            getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                null,
                OrderTask.PAGE_SIZE,
                OrderSortDto.LAST_UPDATE_ASC,
                emptyList()
            )
        } returns Mono.just(OrdersDto("ETHEREUM:cursor_1", listOf(mockk())))

        every {
            getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                "ETHEREUM:cursor_1",
                OrderTask.PAGE_SIZE,
                OrderSortDto.LAST_UPDATE_ASC,
                emptyList()
            )
        } returns Mono.just(OrdersDto(null, listOf(mockk())))
    }

    @Test
    fun `should launch first run of the task`(): Unit = runBlocking {
        val task = OrderTask(
            SearchReindexerConfiguration(SearchReindexerProperties()),
            orderClient,
            esOperations,
            converter
        )

        task.runLongTask(
            null,
            "ETHEREUM"
        ).toList()

        verify {
            orderClient.getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                null,
                OrderTask.PAGE_SIZE,
                OrderSortDto.LAST_UPDATE_ASC,
                emptyList()
            )

            converter.convert(any<OrderDto>())
            esOperations.save(any<Iterable<ElasticOrder>>())
        }
    }

    @Test
    fun `should launch next run of the task`(): Unit = runBlocking {
        val task = OrderTask(
            SearchReindexerConfiguration(SearchReindexerProperties()),
            orderClient,
            esOperations,
            converter
        )

        task.runLongTask(
            "ETHEREUM:cursor_1",
            "ETHEREUM"
        ).toList()

        verify {
            orderClient.getOrdersAll(
                listOf(BlockchainDto.ETHEREUM),
                "ETHEREUM:cursor_1",
                OrderTask.PAGE_SIZE,
                OrderSortDto.LAST_UPDATE_ASC,
                emptyList()
            )

            converter.convert(any<OrderDto>())
            esOperations.save(any<Iterable<ElasticOrder>>())
        }
    }

    companion object {
        private fun randomOrderId(): OrderIdDto {
            return OrderIdDto(
                blockchain = BlockchainDto.ETHEREUM,
                value = randomAddress().toString()
            )
        }
        private fun randomUnionAddress(): UnionAddress {
            return UnionAddress(
                blockchainGroup = BlockchainGroupDto.ETHEREUM,
                value = randomAddress().toString()
            )
        }
        private fun randomContract(): ContractAddress {
            return ContractAddress(
                blockchain = BlockchainDto.ETHEREUM,
                value = randomAddress().toString()
            )
        }
    }
}