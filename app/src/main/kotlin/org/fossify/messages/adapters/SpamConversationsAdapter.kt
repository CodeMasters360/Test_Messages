package org.fossify.messages.adapters

import android.view.Menu
import android.widget.Toast
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.R
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.extensions.restoreSpamMessage
import org.fossify.messages.extensions.spamMessagesDB
import org.fossify.messages.helpers.SpamFilter
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.models.Conversation

/**
 * Shows the Spam folder. Every item is a single spam message, not a conversation: the
 * `threadId` of the [Conversation] items is the id of the spam message.
 */
class SpamConversationsAdapter(
    activity: SimpleActivity, recyclerView: MyRecyclerView, onRefresh: () -> Unit, itemClick: (Any) -> Unit
) : BaseConversationsAdapter(activity, recyclerView, onRefresh, itemClick) {
    override fun getActionMenuId() = R.menu.cab_spam_conversations

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> askConfirmDelete()
            R.id.cab_not_spam -> markMessagesAsNotSpam()
            R.id.cab_select_all -> selectAll()
        }
    }

    private fun askConfirmDelete() {
        ConfirmationDialog(activity) {
            ensureBackgroundThread {
                deleteMessages()
            }
        }
    }

    private fun getSelectedMessages(): ArrayList<Conversation> {
        return currentList.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<Conversation>
    }

    private fun deleteMessages() {
        if (selectedKeys.isEmpty()) {
            return
        }

        val messagesToRemove = getSelectedMessages()
        messagesToRemove.forEach {
            activity.spamMessagesDB.delete(it.threadId)
        }

        removeMessagesFromList(messagesToRemove)
    }

    private fun markMessagesAsNotSpam() {
        if (selectedKeys.isEmpty()) {
            return
        }

        ensureBackgroundThread {
            val messagesToRestore = getSelectedMessages()
            var stillSpam = false
            messagesToRestore.forEach {
                val spamMessage = activity.spamMessagesDB.getById(it.threadId) ?: return@forEach
                activity.restoreSpamMessage(spamMessage)
                activity.spamMessagesDB.delete(spamMessage.id)
                if (SpamFilter.isSpam(activity, spamMessage.address, spamMessage.body)) {
                    stillSpam = true
                }
            }

            if (stillSpam) {
                activity.toast(R.string.not_spam_still_blocked, Toast.LENGTH_LONG)
            }

            // the restored messages have to show up in the conversations list
            refreshConversations()
            removeMessagesFromList(messagesToRestore)
        }
    }

    private fun removeMessagesFromList(removedMessages: List<Conversation>) {
        val newList = try {
            currentList.toMutableList().apply { removeAll(removedMessages) }
        } catch (ignored: Exception) {
            currentList.toMutableList()
        }

        activity.runOnUiThread {
            if (newList.none { selectedKeys.contains(it.hashCode()) }) {
                refreshConversations()
                finishActMode()
            } else {
                submitList(newList)
                if (newList.isEmpty()) {
                    refreshConversations()
                }
            }
        }
    }
}
