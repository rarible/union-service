package com.rarible.protocol.union.search.indexer.repository

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.converter.EsOrderConverter
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.EsAllOrderFilter
import com.rarible.protocol.union.core.model.EsOrder
import com.rarible.protocol.union.core.model.EsOrderBidOrdersByItem
import com.rarible.protocol.union.core.model.EsOrderSellOrders
import com.rarible.protocol.union.core.model.EsOrderSellOrdersByItem
import com.rarible.protocol.union.core.model.EsOrdersByMakers
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsOrderRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.BoolQueryBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.test.context.ContextConfiguration
import randomOrder

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsOrderRepositoryFt {

    private val logger by Logger()

    @Autowired
    protected lateinit var repository: EsOrderRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `should save and read`(): Unit = runBlocking {

        val order = randomOrder()
        val esOrder = EsOrderConverter.convert(order)

        val id = repository.save(esOrder).orderId
        val found = repository.findById(id)
        assertThat(found?.orderId).isEqualTo(esOrder.orderId)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `EsAllOrderFilter should be able to search up to 1000 items`(): Unit = runBlocking {
        // given
        val orders = List(1000) { randomOrder() }.map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000
        val actual = repository.findByFilter(
            EsAllOrderFilter(
                blockchains = BlockchainDto.values().asList(),
                size = 1000,
                continuation = null,
                sort = OrderSortDto.LAST_UPDATE_DESC,
                status = OrderStatusDto.values().asList()
            )
        )

        // then
        assertThat(actual).hasSize(1000)
    }

    @Test
    fun `EsOrderSellOrdersByItem filter`(): Unit = runBlocking {
        // given
        val orders = List(100) { randomOrder() }.map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000

        val exampleOrder = orders.first { it.make.isNft }
        val exampleFilter = EsOrderSellOrdersByItem(
            itemId = exampleOrder.make.address,
            platform = null,
            maker = null,
            origin = null,
            size = 1000,
            continuation = null,
            sort = OrderSortDto.LAST_UPDATE_DESC,
            status = OrderStatusDto.values().asList()
        )
        val filters = listOf(
            exampleFilter,
            exampleFilter.copy(platform = exampleOrder.platform),
            exampleFilter.copy(platform = exampleOrder.platform, maker = exampleOrder.maker)
        )

        //then
        filters.forEach { filter ->
            assertThat(
                repository.findByFilter(filter).first().orderId
            ).isEqualTo(exampleOrder.orderId)
        }

    }

    @Test
    fun `EsOrderBidOrdersByItem filter`(): Unit = runBlocking {
        // given
        val orders = List(100) {
            val o = randomOrder()
            o.copy(take = o.make, make = o.take)
        }.map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000

        val exampleOrder = orders.first()
        val exampleFilter = EsOrderBidOrdersByItem(
            itemId = exampleOrder.take.address,
            platform = null,
            maker = null,
            origin = null,
            size = 1000,
            continuation = null,
            sort = OrderSortDto.LAST_UPDATE_DESC,
            status = OrderStatusDto.values().asList()
        )
        val filters = listOf(
            exampleFilter,
            exampleFilter.copy(platform = exampleOrder.platform),
            exampleFilter.copy(platform = exampleOrder.platform, maker = listOf(exampleOrder.maker))
        )

        //then
        filters.forEach { filter ->
            val esOrders = repository.findByFilter(filter)
            assertThat(esOrders).isNotEmpty
            assertThat(
                esOrders.first().orderId
            ).isEqualTo(exampleOrder.orderId)
        }
    }

    @Test
    fun `EsOrdersByMakers filter`(): Unit = runBlocking {
        // given
        val orders = List(100) {
            val o = randomOrder()
            o.copy(take = o.make, make = o.take)
        }.map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000

        val o1 = orders.first()
        val o2 = orders.last()
        val exampleFilter = EsOrdersByMakers(
            platform = null,
            maker = listOf(o1.maker, o2.maker),
            origin = null,
            size = 1000,
            continuation = null,
            sort = OrderSortDto.LAST_UPDATE_DESC,
            status = OrderStatusDto.values().asList(),
            type = EsOrder.Type.SELL
        )

        //then
        val esOrders = repository.findByFilter(exampleFilter)
        assertThat(esOrders).hasSize(2)
    }

    @Test
    fun `EsOrderSellOrders filter`(): Unit = runBlocking {
        // given
        val orders = List(100) {
            val o = randomOrder()
            o.copy(take = o.make, make = o.take)
        }.map { EsOrderConverter.convert(it) }
        repository.saveAll(orders)

        // when
        val query = NativeSearchQuery(BoolQueryBuilder())
        query.maxResults = 1000

        val o1 = orders.first()
        val o2 = orders.last()
        val countOrders = orders.count { it.blockchain == o1.blockchain || it.blockchain == o2.blockchain }

        val exampleFilter = EsOrderSellOrders(
            blockchains = listOf(o1.blockchain, o2.blockchain),
            platform = null,
            origin = null,
            size = 1000,
            continuation = null,
            sort = OrderSortDto.LAST_UPDATE_DESC,
        )

        //then
        val esOrders = repository.findByFilter(exampleFilter)
        assertThat(esOrders).hasSize(countOrders)
    }
}
