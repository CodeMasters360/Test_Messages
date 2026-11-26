package org.fossify.messages.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.areSystemAnimationsEnabled
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.R
import org.fossify.messages.adapters.SpamConversationsAdapter
import org.fossify.messages.databinding.ActivitySpamConversationsBinding
import org.fossify.messages.dialogs.SpamMessageDialog
import org.fossify.messages.extensions.removeAllSpamMessages
import org.fossify.messages.extensions.spamMessagesDB
import org.fossify.messages.models.Conversation
import org.fossify.messages.models.Events
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
                R.id.manage_spam_senders -> startActivity(Intent(this, ManageBlockedSendersActivity::class.java))
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun loadSpamMessages() {
        ensureBackgroundThread {
            val conversations = runCatching {
                ArrayList(spamMessagesDB.getAll().map { it.toConversation() })
            }.getOrDefault(ArrayList())

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

    @SuppressLint("NotifyDataSetChanged")
    private fun getOrCreateConversationsAdapter(): SpamConversationsAdapter {
        var currAdapter = binding.conversationsList.adapter
        if (currAdapter == null) {
            hideKeyboard()
            currAdapter = SpamConversationsAdapter(
                activity = this,
                recyclerView = binding.conversationsList,
                onRefresh = { binding.conversationsList.adapter?.notifyDataSetChanged() },
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
        val isEmpty = conversations.isEmpty()
        binding.conversationsFastscroller.beGoneIf(isEmpty)
        binding.noConversationsPlaceholder.beVisibleIf(isEmpty)
        binding.noConversationsPlaceholder.setTextColor(getProperTextColor())
        binding.noConversationsPlaceholder.text = getString(R.string.no_spam_conversations)
        binding.spamToolbar.menu.findItem(R.id.empty_spam)?.isVisible = !isEmpty

        getOrCreateConversationsAdapter().updateConversations(conversations)
    }

    private fun handleSpamMessageClick(any: Any) {
        val conversation = any as Conversation
        ensureBackgroundThread {
            val spamMessage = spamMessagesDB.getById(conversation.threadId) ?: return@ensureBackgroundThread
            runOnUiThread {
                SpamMessageDialog(this, spamMessage)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshConversations(@Suppress("unused") event: Events.RefreshConversations) {
        loadSpamMessages()
    }
}
