package org.fossify.messages.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.models.PhoneNumber
import org.fossify.commons.models.SimpleContact
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.getConversations
import org.fossify.messages.extensions.getNameFromAddress
import org.fossify.messages.extensions.getNotificationBitmap
import org.fossify.messages.extensions.getThreadId
import org.fossify.messages.extensions.insertNewSMS
import org.fossify.messages.extensions.insertOrUpdateConversation
import org.fossify.messages.extensions.isConversationSpam
import org.fossify.messages.extensions.markConversationAsSpam
import org.fossify.messages.extensions.messagesDB
import org.fossify.messages.extensions.shouldUnarchive
import org.fossify.messages.extensions.showReceivedMessageNotification
<<<<<<< HEAD
import org.fossify.messages.extensions.saveSpamMessage
=======
import org.fossify.messages.extensions.unmarkConversationAsSpam
>>>>>>> 9c2f92fd66cf484dd365f7c933cdd2e1c76d5e89
import org.fossify.messages.extensions.updateConversationArchivedStatus
import org.fossify.messages.helpers.SpamFilter
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
import org.fossify.messages.models.Message

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext

        ensureBackgroundThread {
            try {
                val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (parts.isEmpty()) return@ensureBackgroundThread

                // this is how it has always worked, but need to revisit this.
                val address = parts.last().originatingAddress.orEmpty()
                if (address.isBlank()) return@ensureBackgroundThread
                val subject = parts.last().pseudoSubject.orEmpty()
                val status = parts.last().status
                val body = buildString { parts.forEach { append(it.messageBody.orEmpty()) } }

<<<<<<< HEAD
                // Only received messages are checked, never the ones the user sends.
                // A message detected as spam (blocked number / sender name, blocked keyword,
                // unknown number) is no longer discarded: just that message is kept in the
                // Spam folder, with its sender, silently (no conversation, no notification).
                if (SpamFilter.isSpam(appContext, address, body)) {
                    if (appContext.config.moveBlockedToSpam) {
                        appContext.saveSpamMessage(
                            address = address,
                            body = body,
                            date = System.currentTimeMillis(),
                            subscriptionId = intent.getIntExtra("subscription", -1)
                        )
                        refreshConversations()
                    }
=======
                // Messages detected as spam (blocked number / sender name, blocked keyword,
                // unknown number) are no longer discarded: they are saved silently and moved
                // to the Spam folder, unless the user turned that option off.
                val isSpam = SpamFilter.isSpam(appContext, address, body)
                if (isSpam && !appContext.config.moveBlockedToSpam) {
>>>>>>> 9c2f92fd66cf484dd365f7c933cdd2e1c76d5e89
                    return@ensureBackgroundThread
                }

                val date = System.currentTimeMillis()
                val threadId = appContext.getThreadId(address)
                val subscriptionId = intent.getIntExtra("subscription", -1)

                handleMessageSync(
                    context = appContext,
                    address = address,
                    subject = subject,
                    body = body,
                    date = date,
                    threadId = threadId,
                    subscriptionId = subscriptionId,
                    status = status,
                    isSpam = isSpam
                )
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleMessageSync(
        context: Context,
        address: String,
        subject: String,
        body: String,
        date: Long,
        read: Int = 0,
        threadId: Long,
        type: Int = Telephony.Sms.MESSAGE_TYPE_INBOX,
        subscriptionId: Int,
        status: Int,
        isSpam: Boolean = false
    ) {
        val photoUri = SimpleContactsHelper(context).getPhotoUriFromPhoneNumber(address)
        // no notification is shown for spam, so there is no need to load the bitmap
        val bitmap = if (isSpam) null else context.getNotificationBitmap(photoUri)
        // spam is stored as already read, so it doesn't add to unread counters / badges
        val readState = if (isSpam) 1 else read

        if (isSpam) {
            // flag the thread first, so it never shows up in the main conversations list
            context.markConversationAsSpam(threadId)
        }

        val newMessageId = context.insertNewSMS(
            address = address,
            subject = subject,
            body = body,
            date = date,
            read = readState,
            threadId = threadId,
            type = type,
            subscriptionId = subscriptionId
        )

        context.getConversations(threadId).firstOrNull()?.let { conv ->
            runCatching { context.insertOrUpdateConversation(conv) }
        }

        val senderName = context.getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true).use {
            context.getNameFromAddress(address, it)
        }

        val participant = SimpleContact(
            rawId = 0,
            contactId = 0,
            name = senderName,
            photoUri = photoUri,
            phoneNumbers = arrayListOf(PhoneNumber(value = address, type = 0, label = "", normalizedNumber = address)),
            birthdays = ArrayList(),
            anniversaries = ArrayList()
        )

        val message = Message(
            id = newMessageId,
            body = body,
            type = type,
            status = status,
            participants = arrayListOf(participant),
            date = (date / 1000).toInt(),
            read = readState == 1,
            threadId = threadId,
            isMMS = false,
            attachment = null,
            senderPhoneNumber = address,
            senderName = senderName,
            senderPhotoUri = photoUri,
            subscriptionId = subscriptionId
        )

        context.messagesDB.insertOrUpdate(message)

        if (isSpam) {
            refreshMessages()
            refreshConversations()
            return
        }

        // a regular message in a conversation that was moved to Spam brings it back
        if (context.isConversationSpam(threadId)) {
            context.unmarkConversationAsSpam(threadId)
        }

        if (context.shouldUnarchive()) {
            context.updateConversationArchivedStatus(threadId, false)
        }

        refreshMessages()
        refreshConversations()
        context.showReceivedMessageNotification(
            messageId = newMessageId,
            isMms = false,
            address = address,
            senderName = senderName,
            body = body,
            threadId = threadId,
            bitmap = bitmap
        )
    }
}
