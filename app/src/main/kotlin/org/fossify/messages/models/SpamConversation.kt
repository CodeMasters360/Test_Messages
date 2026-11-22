package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Marks a conversation (thread) as being in the Spam folder.
 *
 * Kept in a dedicated table (instead of a column on `conversations`) because the
 * `conversations` rows are regularly replaced with fresh data coming from the telephony
 * provider, which knows nothing about spam.
 */
@Entity(
    tableName = "spam_conversations",
    indices = [(Index(value = ["thread_id"], unique = true))]
)
data class SpamConversation(
    @PrimaryKey @ColumnInfo(name = "thread_id") val threadId: Long,
    @ColumnInfo(name = "marked_ts") val markedTS: Long
)
