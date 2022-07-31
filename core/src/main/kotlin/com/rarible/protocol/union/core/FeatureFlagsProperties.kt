package com.rarible.protocol.union.core

import org.elasticsearch.action.support.WriteRequest
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("common.feature-flags")
@ConstructorBinding
data class FeatureFlagsProperties(
    val enableNotificationOnCollectionOrders: Boolean = true,
    val enableRevertedActivityEventSending: Boolean = false,
    val enableOwnershipSourceEnrichment: Boolean = false,
    val enableItemLastSaleEnrichment: Boolean = true,
    val enableLegacyWrappedEventTopic: Boolean = true,
    val enableContentMetaCache: Boolean = true,
    val enableEmbeddedContentMigrationJob: Boolean = true,
    // activities
    var enableActivityQueriesToElasticSearch: Boolean = false,
    var enableImmutableXActivitiesQueries: Boolean = false,
    var enableActivityAscQueriesWithApiMerge: Boolean = true,
    var enableActivitySaveImmediateToElasticSearch: Boolean = false,
    // orders
    var enableOrderQueriesToElasticSearch: Boolean = false,
    var enableOrderSaveImmediateToElasticSearch: Boolean = false,
    var orderRefreshPolicy: WriteRequest.RefreshPolicy = WriteRequest.RefreshPolicy.NONE,
    // collections
    val enableCollectionQueriesToElastic: Boolean = false,
    var enableCollectionSaveImmediateToElasticSearch: Boolean = false,
    // ownerships
    var enableOwnershipQueriesToElasticSearch: Boolean = false,
    var enableOwnershipSaveImmediateToElasticSearch: Boolean = false,
    // items
    var enableItemQueriesToElasticSearch: Boolean = false,
    var enableItemSaveImmediateToElasticSearch: Boolean = false,
)
