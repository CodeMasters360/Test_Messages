package org.fossify.messages.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.fossify.messages.models.Conversation
import org.fossify.messages.models.ConversationWithSnippetOverride
import org.fossify.messages.models.SpamConversation

@Dao
interface ConversationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(conversation: Conversation): Long

    @Query("SELECT (SELECT body FROM messages LEFT OUTER JOIN recycle_bin_messages ON messages.id = recycle_bin_messages.id WHERE recycle_bin_messages.id IS NULL AND messages.thread_id = conversations.thread_id ORDER BY messages.date DESC LIMIT 1) as new_snippet, * FROM conversations WHERE archived = 0 AND conversations.thread_id NOT IN (SELECT spam_conversations.thread_id FROM spam_conversations)")
    fun getNonArchivedWithLatestSnippet(): List<ConversationWithSnippetOverride>

    fun getNonArchived(): List<Conversation> {
        return getNonArchivedWithLatestSnippet().map { it.toConversation() }
    }

    @Query("SELECT (SELECT body FROM messages LEFT OUTER JOIN recycle_bin_messages ON messages.id = recycle_bin_messages.id WHERE recycle_bin_messages.id IS NULL AND messages.thread_id = conversations.thread_id ORDER BY messages.date DESC LIMIT 1) as new_snippet, * FROM conversations WHERE archived = 1 AND conversations.thread_id NOT IN (SELECT spam_conversations.thread_id FROM spam_conversations)")
    fun getAllArchivedWithLatestSnippet(): List<ConversationWithSnippetOverride>

    fun getAllArchived(): List<Conversation> {
        return getAllArchivedWithLatestSnippet().map { it.toConversation() }
    }

    @Query("SELECT (SELECT body FROM messages LEFT OUTER JOIN recycle_bin_messages ON messages.id = recycle_bin_messages.id WHERE recycle_bin_messages.id IS NOT NULL AND messages.thread_id = conversations.thread_id ORDER BY messages.date DESC LIMIT 1) as new_snippet, * FROM conversations WHERE (SELECT COUNT(*) FROM messages LEFT OUTER JOIN recycle_bin_messages ON messages.id = recycle_bin_messages.id WHERE recycle_bin_messages.id IS NOT NULL AND messages.thread_id = conversations.thread_id) > 0")
    fun getAllWithMessagesInRecycleBinWithLatestSnippet(): List<ConversationWithSnippetOverride>

    fun getAllWithMessagesInRecycleBin(): List<Conversation> {
        return getAllWithMessagesInRecycleBinWithLatestSnippet().map { it.toConversation() }
    }

    @Query("SELECT * FROM conversations WHERE thread_id = :threadId")
    fun getConversationWithThreadId(threadId: Long): Conversation?

    @Query("SELECT * FROM conversations WHERE read = 0")
    fun getUnreadConversations(): List<Conversation>

    @Query("SELECT * FROM conversations WHERE title LIKE :text AND conversations.thread_id NOT IN (SELECT spam_conversations.thread_id FROM spam_conversations)")
    fun getConversationsWithText(text: String): List<Conversation>

    @Query("UPDATE conversations SET read = 1 WHERE thread_id = :threadId")
    fun markRead(threadId: Long)

    @Query("UPDATE conversations SET read = 0 WHERE thread_id = :threadId")
    fun markUnread(threadId: Long)

    @Query("UPDATE conversations SET archived = 1 WHERE thread_id = :threadId")
    fun moveToArchive(threadId: Long)

    @Query("UPDATE conversations SET archived = 0 WHERE thread_id = :threadId")
    fun unarchive(threadId: Long)

    @Transaction
    fun deleteThreadId(threadId: Long) {
        deleteConversationRow(threadId)
        deleteSpamEntry(threadId)
    }

    @Query("DELETE FROM conversations WHERE thread_id = :threadId")
    fun deleteConversationRow(threadId: Long)

    // ---- Spam folder ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSpamEntry(spamConversation: SpamConversation)

    @Query("DELETE FROM spam_conversations WHERE thread_id = :threadId")
    fun deleteSpamEntry(threadId: Long)

    @Query("SELECT thread_id FROM spam_conversations")
    fun getAllSpamThreadIds(): List<Long>

    @Query("SELECT COUNT(*) FROM spam_conversations WHERE thread_id = :threadId")
    fun getSpamEntryCount(threadId: Long): Int

    fun isThreadSpam(threadId: Long): Boolean {
        return getSpamEntryCount(threadId) > 0
    }

    @Query("SELECT (SELECT body FROM messages LEFT OUTER JOIN recycle_bin_messages ON messages.id = recycle_bin_messages.id WHERE recycle_bin_messages.id IS NULL AND messages.thread_id = conversations.thread_id ORDER BY messages.date DESC LIMIT 1) as new_snippet, * FROM conversations WHERE conversations.thread_id IN (SELECT spam_conversations.thread_id FROM spam_conversations)")
    fun getAllSpamWithLatestSnippet(): List<ConversationWithSnippetOverride>

    fun getAllSpam(): List<Conversation> {
        return getAllSpamWithLatestSnippet().map { it.toConversation() }
    }
}
