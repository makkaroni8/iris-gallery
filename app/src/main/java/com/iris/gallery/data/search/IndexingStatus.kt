package com.iris.gallery.data.search

data class IndexingStatus(
    val isIndexing: Boolean = false,
    val indexedCount: Int = 0,
    val totalCount: Int = 0,
)
