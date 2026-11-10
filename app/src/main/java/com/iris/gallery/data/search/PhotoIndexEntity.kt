package com.iris.gallery.data.search

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_index")
data class PhotoIndexEntity(
    @PrimaryKey
    @ColumnInfo(name = "media_store_id")
    val mediaStoreId: Long,
    @ColumnInfo(name = "content_uri")
    val contentUri: String,
    @ColumnInfo(name = "ocr_text")
    val ocrText: String,
    @ColumnInfo(name = "labels_de")
    val labelsDe: String,
    @ColumnInfo(name = "labels_en")
    val labelsEn: String,
    @ColumnInfo(name = "date_modified")
    val dateModified: Long,
)
