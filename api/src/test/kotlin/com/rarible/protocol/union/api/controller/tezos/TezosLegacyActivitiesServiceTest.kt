package com.rarible.protocol.union.api.controller.tezos

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TezosMTAssetTypeDto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.integration.tezos.service.TezosActivityService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.stream.Stream


@Testcontainers
@FlowPreview
@IntegrationTest
class TezosLegacyActivitiesServiceTest : AbstractIntegrationTest() {

    companion object {

        val blockchain = BlockchainDto.TEZOS

        private fun date(str: String): Instant =
            LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
                .toInstant(ZoneOffset.UTC)

        @Container
        val container = PostgreSQLContainer<Nothing>("postgres:13.5").apply {
            withDatabaseName("postgres")
            withUsername("duke")
            withPassword("s3crEt")
            withInitScript("tezosInit.sql")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("integration.tezos.pg.port", container::getFirstMappedPort)
            registry.add("integration.tezos.pg.host", container::getHost)
            registry.add("integration.tezos.pg.user", container::getUsername)
            registry.add("integration.tezos.pg.password", container::getPassword)
            registry.add("integration.tezos.pg.database", container::getDatabaseName)
        }

        @JvmStatic
        fun activities() = Stream.of(
            Arguments.of(
                TypedActivityId(
                    id = "BKsJSiEfxzdSsuagvyLQfnZ7AQPbtjnmMkNoXmdnz8dadM45dE2_9",
                    type = ActivityTypeDto.MINT
                ),
                MintActivityDto(
                    id = ActivityIdDto(blockchain, "BKsJSiEfxzdSsuagvyLQfnZ7AQPbtjnmMkNoXmdnz8dadM45dE2_9"),
                    date = date("2022-04-28 19:14:59.000000"),
                    owner = UnionAddressConverter.convert(blockchain, "tz1QFyjHEp3kfeyEuMAAcuc4oYZjmrMjn9vx"),
                    contract = ContractAddressConverter.convert(
                        blockchain,
                        "KT1RJ6PbjHpwc3M5rw5s2Nbmefwbuwbdxton"
                    ),
                    tokenId = BigInteger("726245"),
                    itemId = ItemIdDto(
                        blockchain,
                        "KT1RJ6PbjHpwc3M5rw5s2Nbmefwbuwbdxton",
                        BigInteger("726245")
                    ),
                    value = 3.toBigInteger(),
                    transactionHash = "opNoeB9ub52E9yEcUv6nYhTYHJ8R9SsN21S7n9zpS8qcFckc21i",
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = "opNoeB9ub52E9yEcUv6nYhTYHJ8R9SsN21S7n9zpS8qcFckc21i",
                        blockHash = "",
                        blockNumber = 0,
                        logIndex = 0
                    ),
                    reverted = false
                )
            ),
            Arguments.of(
                TypedActivityId(
                    id = "BKmg87vCWK7uwRF4avDWj7KAmcuLCrVv4gMLCfg3sHdS82rsEfq_8",
                    type = ActivityTypeDto.TRANSFER
                ),
                TransferActivityDto(
                    id = ActivityIdDto(blockchain, "BKmg87vCWK7uwRF4avDWj7KAmcuLCrVv4gMLCfg3sHdS82rsEfq_8"),
                    date = date("2022-04-28 19:51:59.000000"),
                    from = UnionAddressConverter.convert(blockchain, "tz1LUDkKvKpoz2iXtih9SpiPwiVsBoxvtsTh"),
                    owner = UnionAddressConverter.convert(blockchain, "KT1BvWGFENd4CXW5F3u4n31xKfJhmBGipoqF"),
                    contract = ContractAddressConverter.convert(
                        blockchain,
                        "KT1MxDwChiDwd6WBVs24g1NjERUoK622ZEFp"
                    ),
                    tokenId = BigInteger("16625"),
                    itemId = ItemIdDto(
                        blockchain,
                        "KT1MxDwChiDwd6WBVs24g1NjERUoK622ZEFp",
                        BigInteger("16625")
                    ),
                    value = 1.toBigInteger(),
                    transactionHash = "oo22KG27pptFtKEGJ8JcwN44teZoRMTigRaFsKiKPnwvWcg8xB6",
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = "oo22KG27pptFtKEGJ8JcwN44teZoRMTigRaFsKiKPnwvWcg8xB6",
                        blockHash = "",
                        blockNumber = 0,
                        logIndex = 0
                    ),
                    reverted = false
                )
            ),
            Arguments.of(
                TypedActivityId(
                    id = "BLgNWpfjc5xWzvbY7neaE4DPFWGUZvJT77dUAHxb3YubYzMWQCA_4",
                    type = ActivityTypeDto.BURN
                ),
                BurnActivityDto(
                    id = ActivityIdDto(blockchain, "BLgNWpfjc5xWzvbY7neaE4DPFWGUZvJT77dUAHxb3YubYzMWQCA_4"),
                    date = date("2022-04-28 19:58:14.000000"),
                    owner = UnionAddressConverter.convert(blockchain, "tz1hFesk6GV6fT3vak68zz5JxdZ5kK81rvRB"),
                    contract = ContractAddressConverter.convert(
                        blockchain,
                        "KT18pVpRXKPY2c4U2yFEGSH3ZnhB2kL8kwXS"
                    ),
                    tokenId = BigInteger("61026"),
                    itemId = ItemIdDto(
                        blockchain,
                        "KT18pVpRXKPY2c4U2yFEGSH3ZnhB2kL8kwXS",
                        BigInteger("61026")
                    ),
                    value = 1.toBigInteger(),
                    transactionHash = "ooAYipzS3MU83RKQMXVhmwcC9jgtpPpENQqNGUw9CDMyQbo13wj",
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = "ooAYipzS3MU83RKQMXVhmwcC9jgtpPpENQqNGUw9CDMyQbo13wj",
                        blockHash = "",
                        blockNumber = 0,
                        logIndex = 0
                    ),
                    reverted = false
                )
            ),
            Arguments.of(
                TypedActivityId(
                    id = "d432fb5d00706c54a8fe7b1132d65a0ef2e3306bfe484d39ee7bd6ba3e4dd4f3d930750b1aecdda8854925917c0c99b1ee957f9d0fcb2e3c2652abd3c1737783dbc1d94d1d51ffb12e56dda15a9cb5f40c4be139c2e46b351c3bee9572b12c2b57bc354f5162a5add470ec0961c86e27dab5aa8b2825dd63bcde062a64fde6ff",
                    type = ActivityTypeDto.LIST
                ),
                OrderListActivityDto(
                    id = ActivityIdDto(
                        blockchain,
                        "d432fb5d00706c54a8fe7b1132d65a0ef2e3306bfe484d39ee7bd6ba3e4dd4f3d930750b1aecdda8854925917c0c99b1ee957f9d0fcb2e3c2652abd3c1737783dbc1d94d1d51ffb12e56dda15a9cb5f40c4be139c2e46b351c3bee9572b12c2b57bc354f5162a5add470ec0961c86e27dab5aa8b2825dd63bcde062a64fde6ff"
                    ),
                    date = date("2022-04-26 15:39:10.000000"),
                    price = BigDecimal("1"),
                    priceUsd = BigDecimal("1"),
                    source = OrderActivitySourceDto.RARIBLE,
                    hash = "c2e72a459d006fb63d4de1ce234748b9ae40be96541c4bd275e355589d952aa3",
                    maker = UnionAddressConverter.convert(blockchain, "tz1aSkwEot3L2kmUvcoxzjMomb9mvBNuzFK6"),
                    make = AssetDto(
                        type = TezosMTAssetTypeDto(
                            contract = ContractAddressConverter.convert(
                                blockchain,
                                "KT1REVBNMiz2QuQjgGM9UsoPDmu7Jcm9gX6y"
                            ),
                            tokenId = 3.toBigInteger()
                        ),
                        value = BigDecimal("111")
                    ),
                    take = AssetDto(
                        type = TezosXTZAssetTypeDto(),
                        value = BigDecimal("111")
                    ),
                    reverted = false
                )
            ),
            Arguments.of(
                TypedActivityId(
                    id = "BKpJX4yv2JsxezPcvgnavyjJZBZVbQ5hicMwQLEkxv9516Qz27N_46",
                    type = ActivityTypeDto.SELL
                ),
                OrderMatchSellDto(
                    id = ActivityIdDto(blockchain, "BKpJX4yv2JsxezPcvgnavyjJZBZVbQ5hicMwQLEkxv9516Qz27N_46"),
                    date = date("2022-04-28 16:58:44.000000"),
                    source = OrderActivitySourceDto.RARIBLE,
                    transactionHash = "",
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = "",
                        blockHash = "",
                        blockNumber = 0,
                        logIndex = 0
                    ),
                    nft = AssetDto(
                        type = TezosMTAssetTypeDto(
                            contract = ContractAddressConverter.convert(
                                blockchain,
                                "KT1WGYTJRzMUoJHcZ62jRAMGGqVWQrLSMBza"
                            ),
                            tokenId = 3.toBigInteger()
                        ),
                        value = BigDecimal("1")
                    ),
                    payment = AssetDto(
                        type = TezosXTZAssetTypeDto(),
                        value = BigDecimal("0.05")
                    ),
                    seller = UnionAddressConverter.convert(blockchain, "tz1UQMvLFm8xnf7CcSjJwWWa2ibbtrnrsAne"),
                    buyer = UnionAddressConverter.convert(blockchain, "tz1YuZGjEMmfBxGtXSxYgU8VDRH847wbt7nP"),
                    sellerOrderHash = "f1dad99bd88f47cecb6f6124c80f726f7b42a7ddb9e6ded3e68e40c20f49ff13",
                    buyerOrderHash = "8c3b89e350b767fa625165050abe157e6fcdd511981d238f6cbe8e47e43e603c",
                    price = BigDecimal("0.05"),
                    priceUsd = BigDecimal("0.05"),
                    amountUsd = BigDecimal("0.05"),
                    type = OrderMatchSellDto.Type.SELL,
                    reverted = false
                )
            ),
            Arguments.of(
                TypedActivityId(
                    id = "BMHowFJeRxfD5hUKKe2K64tbcNMS33CdgFoKTJXvWt7DumS4WJR_8",
                    type = ActivityTypeDto.CANCEL_LIST
                ),
                OrderCancelListActivityDto(
                    id = ActivityIdDto(blockchain, "BMHowFJeRxfD5hUKKe2K64tbcNMS33CdgFoKTJXvWt7DumS4WJR_8"),
                    date = date("2022-04-28 18:49:44.000000"),
                    maker = UnionAddressConverter.convert(blockchain, "tz1ea4AkZ44BDZS9SFpdEw5cTfYGYNnE22Bd"),
                    make = TezosMTAssetTypeDto(
                        contract = ContractAddressConverter.convert(blockchain, "KT18pVpRXKPY2c4U2yFEGSH3ZnhB2kL8kwXS"),
                        tokenId = 32980.toBigInteger(),
                    ),
                    take = TezosXTZAssetTypeDto(),
                    source = OrderActivitySourceDto.RARIBLE,
                    hash = "9c1cae8b763962949e25c687d16026f803d3d6bc9b3b730445d2b411982c355b",
                    transactionHash = "9c1cae8b763962949e25c687d16026f803d3d6bc9b3b730445d2b411982c355b",
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = "9c1cae8b763962949e25c687d16026f803d3d6bc9b3b730445d2b411982c355b",
                        blockHash = "",
                        blockNumber = 0,
                        logIndex = 0
                    ),
                    reverted = false
                )
            ),
            Arguments.of( // SNDBX-240
                TypedActivityId(
                    id = "BMdUs3hgMfEHLyXtJVRaMem6kq9JAaJmhVfV69zHVpnkygrLiNi_59",
                    type = ActivityTypeDto.SELL
                ),
                OrderMatchSellDto(
                    id = ActivityIdDto(blockchain, "BMdUs3hgMfEHLyXtJVRaMem6kq9JAaJmhVfV69zHVpnkygrLiNi_59"),
                    date = date("2022-04-08 21:34:44.000000"),
                    source = OrderActivitySourceDto.RARIBLE,
                    transactionHash = "",
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = "",
                        blockHash = "",
                        blockNumber = 0,
                        logIndex = 0
                    ),
                    nft = AssetDto(
                        type = TezosMTAssetTypeDto(
                            contract = ContractAddressConverter.convert(
                                blockchain,
                                "KT18c8n9jbpDRZStQRvZUC1GPBEDEcVgwGFC"
                            ),
                            tokenId = 25.toBigInteger()
                        ),
                        value = BigDecimal("1")
                    ),
                    payment = AssetDto(
                        type = TezosXTZAssetTypeDto(),
                        value = BigDecimal("5")
                    ),
                    seller = UnionAddressConverter.convert(blockchain, "tz1ZzkVVRaip6kEmUXeWdQf7aTcyzMg9fA3w"),
                    buyer = UnionAddressConverter.convert(blockchain, "tz1iLaTq7cFBgELhTzBZtRSP76AXKPCqbYGe"),
                    sellerOrderHash = "606d6c3322000cfffbed0a41c4c641241cc5a93d68f220bb7acb6f06a3c4810c",
                    buyerOrderHash = "688a049b1695af56a01e3d64d9632e32a13ca5d36dd2ed68b2e28370b38800f7",
                    price = BigDecimal("5"),
                    priceUsd = BigDecimal("5"),
                    amountUsd = BigDecimal("5"),
                    type = OrderMatchSellDto.Type.SELL,
                    reverted = false
                )
            ),
            Arguments.of(
                TypedActivityId(
                    id = "BLZqXaBVGh6WCEZNFTHgspfDbRxdQNGfZtkMXnhmGdipcqm7bxN_2",
                    type = ActivityTypeDto.SELL
                ),
                OrderMatchSellDto(
                    id = ActivityIdDto(blockchain, "BLZqXaBVGh6WCEZNFTHgspfDbRxdQNGfZtkMXnhmGdipcqm7bxN_2"),
                    date = date("2022-06-10 13:27:55.000000"),
                    source = OrderActivitySourceDto.RARIBLE,
                    transactionHash = "",
                    blockchainInfo = ActivityBlockchainInfoDto(
                        transactionHash = "",
                        blockHash = "",
                        blockNumber = 0,
                        logIndex = 0
                    ),
                    nft = AssetDto(
                        type = TezosMTAssetTypeDto(
                            contract = ContractAddressConverter.convert(
                                blockchain,
                                "KT1RuoaCbnZpMgdRpSoLfJUzSkGz1ZSiaYwj"
                            ),
                            tokenId = 320.toBigInteger()
                        ),
                        value = BigDecimal("1")
                    ),
                    payment = AssetDto(
                        type = TezosXTZAssetTypeDto(),
                        value = BigDecimal("1")
                    ),
                    seller = UnionAddressConverter.convert(blockchain, "tz1ePNX2ys72LrEiLoKRTHukjjWbUu2QaWG3"),
                    buyer = UnionAddressConverter.convert(blockchain, "tz1XzWGWhX8mKfqggyHt4NnoYwuNZ7qiLoNG"),
                    sellerOrderHash = "0e69a1b2d5f705aff75c46570b2ed37449be83136c88d2493381d1fa365e45df",
                    buyerOrderHash = "91fadb78e8df52f83695efc16d325696c829f982ce3e4206489fef4f9b0742d6",
                    price = BigDecimal("1"),
                    priceUsd = BigDecimal("1"),
                    amountUsd = BigDecimal("1"),
                    type = OrderMatchSellDto.Type.SELL,
                    reverted = false
                )
            ),
            Arguments.of(
                TypedActivityId(
                    id = "95af530f9099099b89c5e54ee4bdc4dc64b17a4d85d398d897fd53403f64984cee7690cca83bd7b50f1f05b9f47a9b54e7a60958ae59d1088239919b191b353e1cdd1e6cb1af6b52fcad7710498254566702ab8676c600e79e2027ff46237b94e50979490c324f7c44b88bc7a11185c5c9287e3b59678e1550fc270cc13f1697",
                    type = ActivityTypeDto.LIST
                ),
                OrderListActivityDto(
                    id = ActivityIdDto(
                        blockchain,
                        "95af530f9099099b89c5e54ee4bdc4dc64b17a4d85d398d897fd53403f64984cee7690cca83bd7b50f1f05b9f47a9b54e7a60958ae59d1088239919b191b353e1cdd1e6cb1af6b52fcad7710498254566702ab8676c600e79e2027ff46237b94e50979490c324f7c44b88bc7a11185c5c9287e3b59678e1550fc270cc13f1697"
                    ),
                    date = date("2022-06-24 07:50:21.000000"),
                    price = BigDecimal("0.5"),
                    priceUsd = BigDecimal("0.5"),
                    source = OrderActivitySourceDto.RARIBLE,
                    hash = "0f8b93117f4175a2039965a6281d94809c38f9d8f7ff1985c69a7b2c2e81197e",
                    maker = UnionAddressConverter.convert(blockchain, "tz1hHTdiDezgWNRWyY7RYSyZE11EobKQw583"),
                    make = AssetDto(
                        type = TezosMTAssetTypeDto(
                            contract = ContractAddressConverter.convert(
                                blockchain,
                                "KT18pVpRXKPY2c4U2yFEGSH3ZnhB2kL8kwXS"
                            ),
                            tokenId = 76378.toBigInteger()
                        ),
                        value = BigDecimal("10")
                    ),
                    take = AssetDto(
                        type = TezosXTZAssetTypeDto(),
                        value = BigDecimal("5")
                    ),
                    reverted = false
                )
            )
        )
    }

    @Autowired
    lateinit var tezosActivityService: TezosActivityService

    @ParameterizedTest
    @MethodSource("activities")
    fun `should get activity by id list`(typedActivityId: TypedActivityId, activityDto: ActivityDto) =
        runBlocking<Unit> {
            val activities = tezosActivityService.getActivitiesByIds(listOf(typedActivityId))

            assertThat(activities.first())
                .usingRecursiveComparison()
                .isEqualTo(activityDto)
        }
}
