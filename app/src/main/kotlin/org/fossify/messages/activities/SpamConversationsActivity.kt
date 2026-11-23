package org.fossify.messages.activities

import android.annotation.SuppressLint
import android.os.Bundle
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.areSystemAnimationsEnabled
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.formatDate
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.adapters.SpamConversationsAdapter
import org.fossify.messages.databinding.ActivitySpamConversationsBinding
import org.fossify.messages.extensions.removeAllSpamMessages
import org.fossify.messages.extensions.restoreSpamMessage
import org.fossify.messages.extensions.spamMessagesDB
import org.fossify.messages.models.Conversation
import org.fossify.messages.models.Events
import org.fossify.messages.models.SpamMessage
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class SpamConversationsActivity : SimpleActivity() {
    private var bus: EventBus? = null
    private val binding by viewBinding(ActivitySpamConversationsBinding::inflate)

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.conversationsList))
        setupMaterialScrollListener(
            scrollingView = binding.conversationsList,
            topAppBar = binding.spamAppbar
        )

        loadSpamMessages()
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.spamAppbar, NavigationIcon.Arrow)
        loadSpamMessages()
    }

    override fun onDestroy() {
        super.onDestroy()
        bus?.unregister(this)
    }

    private fun setupOptionsMenu() {
        binding.spamToolbar.inflateMenu(R.menu.spam_menu)
        binding.spamToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.empty_spam -> removeAll()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun updateOptionsMenu(conversations: ArrayList<Conversation>) {
        binding.spamToolbar.menu.apply {
            findItem(R.id.empty_spam).isVisible = conversations.isNotEmpty()
        }
    }

    private fun loadSpamMessages() {
        ensureBackgroundThread {
            val conversations = try {
                ArrayList(spamMessagesDB.getAll().map { it.toConversation() })
            } catch (e: Exception) {
                ArrayList()
            }

            runOnUiThread {
                setupConversations(conversations)
            }
        }

        bus = EventBus.getDefault()
        try {
            bus!!.register(this)
        } catch (ignored: Exception) {
        }
    }

    private fun removeAll() {
        ConfirmationDialog(
            activity = this,
            message = "",
            messageId = R.string.empty_spam_confirmation,
            positive = org.fossify.commons.R.string.yes,
            negative = org.fossify.commons.R.string.no
        ) {
            removeAllSpamMessages {
                loadSpamMessages()
            }
        }
    }

    private fun getOrCreateConversationsAdapter(): SpamConversationsAdapter {
        var currAdapter = binding.conversationsList.adapter
        if (currAdapter == null) {
            hideKeyboard()
            currAdapter = SpamConversationsAdapter(
                activity = this,
                recyclerView = binding.conversationsList,
                onRefresh = { notifyDatasetChanged() },
                itemClick = { handleSpamMessageClick(it) }
            )

            binding.conversationsList.adapter = currAdapter
            if (areSystemAnimationsEnabled) {
                binding.conversationsList.scheduleLayoutAnimation()
            }
        }
        return currAdapter as SpamConversationsAdapter
    }

    private fun setupConversations(conversations: ArrayList<Conversation>) {
        // already sorted from the newest to the oldest message by the database query
        showOrHidePlaceholder(conversations.isEmpty())
        updateOptionsMenu(conversations)

        try {
            getOrCreateConversationsAdapter().apply {
                updateConversations(conversations)
            }
        } catch (ignored: Exception) {
        }
    }

    private fun showOrHidePlaceholder(show: Boolean) {
        binding.conversationsFastscroller.beGoneIf(show)
        binding.noConversationsPlaceholder.beVisibleIf(show)
        binding.noConversationsPlaceholder.setTextColor(getProperTextColor())
        binding.noConversationsPlaceholder.text = getString(R.string.no_spam_conversations)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyDatasetChanged() {
        getOrCreateConversationsAdapter().notifyDataSetChanged()
    }

    // every spam message is shown as an item of its own, the id of the message is used as threadId
    private fun SpamMessage.toConversation() = Conversation(
        threadId = id,
        snippet = body,
        date = (date / 1000).toInt(),
        read = true,
        title = getSenderTitle(),
        photoUri = "",
        isGroupConversation = false,
        phoneNumber = address
    )

    private fun handleSpamMessageClick(any: Any) {
        val conversation = any as Conversation
        ensureBackgroundThread {
            val spamMessage = spamMessagesDB.getById(conversation.threadId) ?: return@ensureBackgroundThread
            runOnUiThread {
                showSpamMessage(spamMessage)
            }
        }
    }

    private fun showSpamMessage(spamMessage: SpamMessage) {
        val details = buildString {
            if (spamMessage.senderName.isNotBlank() && spamMessage.senderName != spamMessage.address) {
                appendLine(spamMessage.address)
            }
            appendLine((spamMessage.date / 1000).toInt().formatDate(this@SpamConversationsActivity))
            appendLine()
            append(spamMessage.body)
        }

        ConfirmationDialog(
            activity = this,
            message = details,
            positive = R.string.not_spam,
            negative = org.fossify.commons.R.string.close,
            dialogTitle = spamMessage.getSenderTitle()
        ) {
            ensureBackgroundThread {
                restoreSpamMessage(spamMessage)
                spamMessagesDB.delete(spamMessage.id)
                org.fossify.messages.helpers.refreshConversations()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConversations(@Suppress("unused") event: Events.RefreshConversations) {
        loadSpamMessages()
    }
}
