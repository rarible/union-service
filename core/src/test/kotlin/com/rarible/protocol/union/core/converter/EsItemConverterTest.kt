package com.rarible.protocol.union.core.converter

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBoolean
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.converter.EsItemConverter.toEsItem
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.ext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import randomOrder
import java.math.BigDecimal
import java.math.BigInteger

class EsItemConverterTest {

    @TestFactory
    fun `should convert`() =
        listOf(
            ItemDto(
                id = ItemIdDto(BlockchainDto.ETHEREUM, randomAddress().toString(), BigInteger.ONE),
                creators = listOf(
                    CreatorDto(
                        account = UnionAddress(
                            BlockchainGroupDto.ETHEREUM,
                            randomAddress().toString()
                        ),
                        value = randomInt(),
                    ),
                    CreatorDto(
                        account = UnionAddress(
                            BlockchainGroupDto.ETHEREUM,
                            randomAddress().toString()
                        ),
                        value = randomInt(),
                    )
                ),
                blockchain = BlockchainDto.ETHEREUM,
                collection = CollectionIdDto(BlockchainDto.ETHEREUM, randomAddress().toString()),
                lazySupply = BigInteger.ONE,
                pending = emptyList(),
                mintedAt = nowMillis(),
                lastUpdatedAt = nowMillis(),
                supply = BigInteger.ONE,
                deleted = randomBoolean(),
                auctions = emptyList(),
                sellers = 1,
                meta = MetaDto(
                    name = randomString(),
                    description = randomString(),
                    attributes = listOf(
                        MetaAttributeDto(randomString(), randomString()),
                        MetaAttributeDto(randomString(), randomString())
                    ),
                    content = emptyList(),
                    restrictions = emptyList()
                ),
                bestSellOrder = randomOrder(
                    take = AssetDto(
                        EthErc20AssetTypeDto(ContractAddress(BlockchainDto.ETHEREUM, randomString())),
                        BigDecimal("10")
                    )
                ),
                bestBidOrder = randomOrder(
                    make = AssetDto(
                        EthErc20AssetTypeDto(ContractAddress(BlockchainDto.ETHEREUM, randomString())),
                        BigDecimal("5")
                    )
                ),
            ),
            ItemDto(
                id = ItemIdDto(BlockchainDto.FLOW, randomAddress().toString(), BigInteger.ONE),
                creators = listOf(
                    CreatorDto(
                        account = UnionAddress(
                            BlockchainGroupDto.FLOW,
                            randomAddress().toString()
                        ),
                        value = randomInt(),
                    ),
                    CreatorDto(
                        account = UnionAddress(
                            BlockchainGroupDto.FLOW,
                            randomAddress().toString()
                        ),
                        value = randomInt(),
                    )
                ),
                blockchain = BlockchainDto.FLOW,
                collection = CollectionIdDto(BlockchainDto.FLOW, randomAddress().toString()),
                lazySupply = BigInteger.ONE,
                pending = emptyList(),
                mintedAt = nowMillis(),
                lastUpdatedAt = nowMillis(),
                supply = BigInteger.ONE,
                deleted = randomBoolean(),
                auctions = emptyList(),
                sellers = 1
            )
        ).map { unionItem ->
            dynamicTest("should convert") {
                val esItem = unionItem.toEsItem()

                assertThat(esItem.itemId).isEqualTo(unionItem.id.fullId())
                assertThat(esItem.blockchain).isEqualTo(unionItem.blockchain)
                assertThat(esItem.collection).isEqualTo(unionItem.collection!!.fullId())
                assertThat(esItem.deleted).isEqualTo(unionItem.deleted)
                assertThat(esItem.mintedAt).isEqualTo(unionItem.mintedAt)
                assertThat(esItem.lastUpdatedAt).isEqualTo(unionItem.lastUpdatedAt)
                assertThat(esItem.creators[0]).isEqualTo(unionItem.creators[0].account.fullId())
                assertThat(esItem.creators[1]).isEqualTo(unionItem.creators[1].account.fullId())

                unionItem.meta?.let {
                    assertThat(esItem.name).isEqualTo(it.name)
                    assertThat(esItem.description).isEqualTo(it.description)

                    assertThat(esItem.traits[0].key).isEqualTo(it.attributes[0].key)
                    assertThat(esItem.traits[0].value).isEqualTo(it.attributes[0].value)
                    assertThat(esItem.traits[1].key).isEqualTo(it.attributes[1].key)
                    assertThat(esItem.traits[1].value).isEqualTo(it.attributes[1].value)
                }

                unionItem.bestSellOrder?.let {
                    assertThat(esItem.bestSellAmount).isEqualTo(10.0)
                    assertThat(esItem.bestSellCurrency).isEqualTo(it.take.type.ext.currencyAddress())
                    assertThat(esItem.bestSellMarketplace).isEqualTo(it.platform.name)
                }

                unionItem.bestBidOrder?.let {
                    assertThat(esItem.bestBidAmount).isEqualTo(5.0)
                    assertThat(esItem.bestBidCurrency).isEqualTo(it.make.type.ext.currencyAddress())
                    assertThat(esItem.bestBidMarketplace).isEqualTo(it.platform.name)
                }
            }
        }
}
