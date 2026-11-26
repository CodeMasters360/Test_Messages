package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single received message that was detected as spam (blocked number / sender name,
 * blocked keyword, unknown number).
 *
 * Spam messages are kept in their own table, they are not part of any conversation and are
 * never written to the telephony provider, so they don't show up in the inbox, in the
 * conversations list or in notifications.
 */
@Entity(tableName = "spam_messages")
data class SpamMessage(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "sender_name") val senderName: String,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "subscription_id") val subscriptionId: Int,
    @ColumnInfo(name = "is_mms") val isMMS: Boolean
) {
    /** The name of the sender when it's known, the phone number / sender ID otherwise. */
    fun getSenderTitle(): String = senderName.ifBlank { address }

    /**
     * Every spam message is shown as an item of its own in the Spam folder. The id of the
     * message is used as the `threadId` of the item.
     */
    fun toConversation() = Conversation(
        threadId = id,
        snippet = body,
        date = (date / 1000).toInt(),
        read = true,
        title = getSenderTitle(),
        photoUri = "",
        isGroupConversation = false,
        phoneNumber = address
    )
}
