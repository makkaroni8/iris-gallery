package com.iris.gallery.data.search

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

@Fts4(contentEntity = PhotoIndexEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "photo_index_fts")
data class PhotoIndexFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    @ColumnInfo(name = "ocr_text")
    val ocrText: String,
    @ColumnInfo(name = "labels_de")
    val labelsDe: String,
    @ColumnInfo(name = "labels_en")
    val labelsEn: String,
)
