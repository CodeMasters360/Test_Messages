package org.fossify.messages.adapters

import android.view.Menu
import android.widget.Toast
import org.fossify.commons.dialogs.ConfirmationDialog
<<<<<<< HEAD
=======
import org.fossify.commons.extensions.deleteBlockedNumber
import org.fossify.commons.extensions.notificationManager
>>>>>>> 9c2f92fd66cf484dd365f7c933cdd2e1c76d5e89
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.R
import org.fossify.messages.activities.SimpleActivity
<<<<<<< HEAD
import org.fossify.messages.extensions.restoreSpamMessage
import org.fossify.messages.extensions.spamMessagesDB
=======
import org.fossify.messages.extensions.deleteConversation
import org.fossify.messages.extensions.unmarkConversationAsSpam
>>>>>>> 9c2f92fd66cf484dd365f7c933cdd2e1c76d5e89
import org.fossify.messages.helpers.SpamFilter
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.models.Conversation

<<<<<<< HEAD
/**
 * Shows the Spam folder. Every item is a single spam message, not a conversation: the
 * `threadId` of the [Conversation] items is the id of the spam message.
 */
=======
>>>>>>> 9c2f92fd66cf484dd365f7c933cdd2e1c76d5e89
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
<<<<<<< HEAD
            R.id.cab_not_spam -> markMessagesAsNotSpam()
=======
            R.id.cab_not_spam -> markConversationsAsNotSpam()
>>>>>>> 9c2f92fd66cf484dd365f7c933cdd2e1c76d5e89
            R.id.cab_select_all -> selectAll()
        }
    }

    private fun askConfirmDelete() {
<<<<<<< HEAD
        ConfirmationDialog(activity) {
            ensureBackgroundThread {
                deleteMessages()
=======
        val itemsCnt = selectedKeys.size
        val items = resources.getQuantityString(R.plurals.delete_conversations, itemsCnt, itemsCnt)

        val baseString = org.fossify.commons.R.string.deletion_confirmation
        val question = String.format(resources.getString(baseString), items)

        ConfirmationDialog(activity, question) {
            ensureBackgroundThread {
                deleteConversations()
>>>>>>> 9c2f92fd66cf484dd365f7c933cdd2e1c76d5e89
            }
        }
    }

<<<<<<< HEAD
    private fun getSelectedMessages(): ArrayList<Conversation> {
        return currentList.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<Conversation>
    }

    private fun deleteMessages() {
=======
    private fun deleteConversations() {
>>>>>>> 9c2f92fd66cf484dd365f7c933cdd2e1c76d5e89
        if (selectedKeys.isEmpty()) {
            return
        }

<<<<<<< HEAD
        val messagesToRemove = getSelectedMessages()
        messagesToRemove.forEach {
            activity.spamMessagesDB.delete(it.threadId)
        }

        removeMessagesFromList(messagesToRemove)
    }

    private fun markMessagesAsNotSpam() {
=======
        val conversationsToRemove = currentList.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<Conversation>
        conversationsToRemove.forEach {
            activity.deleteConversation(it.threadId)
            activity.notificationManager.cancel(it.threadId.hashCode())
        }

        removeConversationsFromList(conversationsToRemove)
    }

    private fun markConversationsAsNotSpam() {
>>>>>>> 9c2f92fd66cf484dd365f7c933cdd2e1c76d5e89
        if (selectedKeys.isEmpty()) {
            return
        }

        ensureBackgroundThread {
<<<<<<< HEAD
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
=======
            val conversationsToRestore = currentList.filter { selectedKeys.contains(it.hashCode()) } as ArrayList<Conversation>
            var stillBlocked = false
            conversationsToRestore.forEach {
                // conversations of blocked senders are hidden from the main list, so try to
                // remove the sender from the blocked numbers as well
                runCatching { activity.deleteBlockedNumber(it.phoneNumber) }
                if (runCatching { SpamFilter.isSenderBlocked(activity, it.phoneNumber) }.getOrDefault(false)) {
                    stillBlocked = true
                }
                activity.unmarkConversationAsSpam(it.threadId)
            }

            if (stillBlocked) {
                activity.toast(R.string.not_spam_still_blocked, Toast.LENGTH_LONG)
            }

            removeConversationsFromList(conversationsToRestore)
        }
    }

    private fun removeConversationsFromList(removedConversations: List<Conversation>) {
        val newList = try {
            currentList.toMutableList().apply { removeAll(removedConversations) }
>>>>>>> 9c2f92fd66cf484dd365f7c933cdd2e1c76d5e89
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
