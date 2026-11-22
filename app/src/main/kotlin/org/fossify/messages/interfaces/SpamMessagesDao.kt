package org.fossify.messages.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.fossify.messages.models.SpamMessage

@Dao
interface SpamMessagesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(spamMessage: SpamMessage): Long

    // newest first
    @Query("SELECT * FROM spam_messages ORDER BY date DESC, id DESC")
    fun getAll(): List<SpamMessage>

    @Query("SELECT * FROM spam_messages WHERE id = :id")
    fun getById(id: Long): SpamMessage?

    @Query("SELECT COUNT(*) FROM spam_messages")
    fun getCount(): Int

    @Query("DELETE FROM spam_messages WHERE id = :id")
    fun delete(id: Long)

    @Query("DELETE FROM spam_messages")
    fun deleteAll()
}
