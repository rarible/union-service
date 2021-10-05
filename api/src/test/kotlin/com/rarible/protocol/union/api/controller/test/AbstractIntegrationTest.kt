package com.rarible.protocol.union.api.controller.test

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.nftorder.api.test.mock.EthItemControllerApiMock
import com.rarible.protocol.nftorder.api.test.mock.EthOrderControllerApiMock
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.api.controller.test.mock.eth.EthOwnershipControllerApiMock
import com.rarible.protocol.union.api.controller.test.mock.flow.FlowItemControllerApiMock
import com.rarible.protocol.union.api.controller.test.mock.flow.FlowOrderControllerApiMock
import com.rarible.protocol.union.api.controller.test.mock.flow.FlowOwnershipControllerApiMock
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestTemplate
import java.net.URI

@FlowPreview
abstract class AbstractIntegrationTest {

    @Autowired
    @Qualifier("testLocalhostUri")
    protected lateinit var baseUri: URI

    @Autowired
    protected lateinit var testRestTemplate: RestTemplate

    @Autowired
    protected lateinit var testItemEventProducer: RaribleKafkaProducer<ItemEventDto>

    @Autowired
    protected lateinit var testOwnershipEventProducer: RaribleKafkaProducer<OwnershipEventDto>

    //--------------------- ETHEREUM ---------------------//
    @Autowired
    @Qualifier("ethereum.item.api")
    lateinit var testEthereumItemApi: NftItemControllerApi

    @Autowired
    @Qualifier("ethereum.ownership.api")
    lateinit var testEthereumOwnershipApi: NftOwnershipControllerApi

    @Autowired
    @Qualifier("ethereum.collection.api")
    lateinit var testEthereumCollectionApi: NftCollectionControllerApi

    @Autowired
    @Qualifier("ethereum.order.api")
    lateinit var testEthereumOrderApi: com.rarible.protocol.order.api.client.OrderControllerApi

    @Autowired
    @Qualifier("ethereum.signature.api")
    lateinit var testEthereumSignatureApi: com.rarible.protocol.order.api.client.OrderSignatureControllerApi

    @Autowired
    @Qualifier("ethereum.activity.api.item")
    lateinit var testEthereumActivityItemApi: NftActivityControllerApi

    @Autowired
    @Qualifier("ethereum.activity.api.order")
    lateinit var testEthereumActivityOrderApi: OrderActivityControllerApi

    lateinit var ethereumItemControllerApiMock: EthItemControllerApiMock
    lateinit var ethereumOwnershipControllerApiMock: EthOwnershipControllerApiMock
    lateinit var ethereumOrderControllerApiMock: EthOrderControllerApiMock

    //--------------------- POLYGON ---------------------//    
    @Autowired
    @Qualifier("polygon.item.api")
    lateinit var testPolygonItemApi: NftItemControllerApi

    @Autowired
    @Qualifier("polygon.ownership.api")
    lateinit var testPolygonOwnershipApi: NftOwnershipControllerApi

    @Autowired
    @Qualifier("polygon.collection.api")
    lateinit var testPolygonCollectionApi: NftCollectionControllerApi

    @Autowired
    @Qualifier("polygon.order.api")
    lateinit var testPolygonOrderApi: com.rarible.protocol.order.api.client.OrderControllerApi

    @Autowired
    @Qualifier("polygon.signature.api")
    lateinit var testPolygonSignatureApi: com.rarible.protocol.order.api.client.OrderSignatureControllerApi

    @Autowired
    @Qualifier("polygon.activity.api.item")
    lateinit var testPolygonActivityItemApi: NftActivityControllerApi

    @Autowired
    @Qualifier("polygon.activity.api.order")
    lateinit var testPolygonActivityOrderApi: OrderActivityControllerApi

    //--------------------- FLOW ---------------------//    
    @Autowired
    lateinit var testFlowItemApi: FlowNftItemControllerApi

    @Autowired
    lateinit var testFlowOwnershipApi: FlowNftOwnershipControllerApi

    @Autowired
    lateinit var testFlowCollectionApi: FlowNftCollectionControllerApi

    @Autowired
    lateinit var testFlowOrderApi: FlowOrderControllerApi

    @Autowired
    lateinit var testFlowActivityApi: FlowNftOrderActivityControllerApi

    lateinit var flowItemControllerApiMock: FlowItemControllerApiMock
    lateinit var flowOwnershipControllerApiMock: FlowOwnershipControllerApiMock
    lateinit var flowOrderControllerApiMock: FlowOrderControllerApiMock


    @BeforeEach
    fun beforeEach() {
        clearMocks(
            testEthereumItemApi,
            testEthereumOwnershipApi,
            testEthereumOrderApi,

            testFlowItemApi,
            testFlowOwnershipApi,
            testFlowOrderApi,

            testItemEventProducer,
            testOwnershipEventProducer
        )
        ethereumItemControllerApiMock = EthItemControllerApiMock(testEthereumItemApi)
        ethereumOwnershipControllerApiMock = EthOwnershipControllerApiMock(testEthereumOwnershipApi)
        ethereumOrderControllerApiMock = EthOrderControllerApiMock(testEthereumOrderApi)

        flowItemControllerApiMock = FlowItemControllerApiMock(testFlowItemApi)
        flowOwnershipControllerApiMock = FlowOwnershipControllerApiMock(testFlowOwnershipApi)
        flowOrderControllerApiMock = FlowOrderControllerApiMock(testFlowOrderApi)

        coEvery {
            testItemEventProducer.send(any() as KafkaMessage<ItemEventDto>)
        } returns KafkaSendResult.Success("")

        coEvery {
            testOwnershipEventProducer.send(any() as KafkaMessage<OwnershipEventDto>)
        } returns KafkaSendResult.Success("")
    }

}