package org.fossify.messages.dialogs

import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.formatDate
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.activities.SimpleActivity
import org.fossify.messages.extensions.restoreSpamMessage
import org.fossify.messages.extensions.spamMessagesDB
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.models.SpamMessage

/**
 * Shows a spam message in full, with the option to move it back to the inbox.
 */
class SpamMessageDialog(val activity: SimpleActivity, val spamMessage: SpamMessage) {
    init {
        val details = buildString {
            if (spamMessage.senderName.isNotBlank() && spamMessage.senderName != spamMessage.address) {
                appendLine(spamMessage.address)
            }
            appendLine((spamMessage.date / 1000).toInt().formatDate(activity))
            appendLine()
            append(spamMessage.body)
        }

        ConfirmationDialog(
            activity = activity,
            message = details,
            positive = R.string.not_spam,
            negative = org.fossify.commons.R.string.close,
            dialogTitle = spamMessage.getSenderTitle()
        ) {
            ensureBackgroundThread {
                activity.restoreSpamMessage(spamMessage)
                activity.spamMessagesDB.delete(spamMessage.id)
                refreshConversations()
            }
        }
    }
}
