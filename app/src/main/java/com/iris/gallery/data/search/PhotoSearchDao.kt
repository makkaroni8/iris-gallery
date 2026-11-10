package com.iris.gallery.data.search

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

data class MediaDateModified(
    @ColumnInfo(name = "media_store_id") val mediaStoreId: Long,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
)

@Dao
interface PhotoSearchDao {

    @Query(
        "SELECT photo_index.media_store_id FROM photo_index " +
            "JOIN photo_index_fts ON photo_index.rowid = photo_index_fts.rowid " +
            "WHERE photo_index_fts MATCH :query || '*'",
    )
    suspend fun searchMatchingIds(query: String): List<Long>

    @Query(
        "SELECT photo_index.media_store_id FROM photo_index " +
            "JOIN photo_index_fts ON photo_index.rowid = photo_index_fts.rowid " +
            "WHERE photo_index_fts MATCH :ftsQuery",
    )
    suspend fun searchMatchingIdsRaw(ftsQuery: String): List<Long>

    @Query(
        "SELECT media_store_id FROM photo_index " +
            "WHERE ocr_text LIKE '%' || :query || '%' " +
            "OR labels_de LIKE '%' || :query || '%' " +
            "OR labels_en LIKE '%' || :query || '%'",
    )
    suspend fun searchByLike(query: String): List<Long>

    @Query(
        "SELECT photo_index.* FROM photo_index " +
            "JOIN photo_index_fts ON photo_index.rowid = photo_index_fts.rowid " +
            "WHERE photo_index_fts MATCH :query || '*'",
    )
    suspend fun searchPhotos(query: String): List<PhotoIndexEntity>

    @Query("SELECT media_store_id FROM photo_index")
    suspend fun getAllIndexedIds(): List<Long>

    @Query("SELECT media_store_id, date_modified FROM photo_index")
    suspend fun getAllDateModified(): List<MediaDateModified>

    @Query("SELECT COUNT(*) FROM photo_index")
    suspend fun getIndexedCount(): Int

    @Query("SELECT * FROM photo_index WHERE media_store_id = :mediaStoreId LIMIT 1")
    suspend fun getIndexForPhoto(mediaStoreId: Long): PhotoIndexEntity?

    @Query("DELETE FROM photo_index WHERE media_store_id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("DELETE FROM photo_index")
    suspend fun clearAll()

    @Upsert
    suspend fun upsertAll(entities: List<PhotoIndexEntity>)
}
