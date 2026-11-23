package org.fossify.messages.activities

import android.os.Bundle
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.deleteBlockedNumber
import org.fossify.commons.extensions.getBlockedNumbers
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.underlineText
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.messages.R
import org.fossify.messages.databinding.ActivityManageBlockedSendersBinding
import org.fossify.messages.dialogs.AddBlockedSenderDialog
import org.fossify.messages.dialogs.ManageBlockedSendersAdapter
import org.fossify.messages.extensions.config
import org.fossify.messages.extensions.toArrayList

/**
 * The app's own list of blocked numbers and sender names. Messages received from them are kept
 * in the Spam folder.
 */
class ManageBlockedSendersActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private val binding by viewBinding(ActivityManageBlockedSendersBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateBlockedSenders()
        setupOptionsMenu()

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.manageBlockedSendersList))
        setupMaterialScrollListener(
            scrollingView = binding.manageBlockedSendersList,
            topAppBar = binding.blockSendersAppbar
        )
        updateTextColors(binding.manageBlockedSendersWrapper)

        binding.manageBlockedSendersPlaceholder2.apply {
            underlineText()
            setTextColor(getProperPrimaryColor())
            setOnClickListener {
                addOrEditBlockedSender()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.blockSendersAppbar, NavigationIcon.Arrow)
    }

    private fun setupOptionsMenu() {
        binding.blockSendersToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_blocked_sender -> {
                    addOrEditBlockedSender()
                    true
                }

                R.id.import_system_blocked -> {
                    askImportFromSystemList()
                    true
                }

                else -> false
            }
        }
    }

    // Android drops the texts of numbers in its own blocked list before the app receives them,
    // so those numbers are moved to the app's list, where their messages can go to Spam.
    private fun askImportFromSystemList() {
        ConfirmationDialog(
            activity = this,
            message = getString(R.string.import_system_blocked_confirmation)
        ) {
            ensureBackgroundThread {
                val systemNumbers = getBlockedNumbers()
                if (systemNumbers.isEmpty()) {
                    toast(R.string.no_system_blocked_numbers)
                    return@ensureBackgroundThread
                }

                systemNumbers.forEach {
                    config.addBlockedSender(it.number)
                    runCatching { deleteBlockedNumber(it.number) }
                }

                toast(R.string.import_system_blocked_done)
                updateBlockedSenders()
            }
        }
    }

    override fun refreshItems() {
        updateBlockedSenders()
    }

    private fun updateBlockedSenders() {
        ensureBackgroundThread {
            val blockedSenders = config.blockedSenders.sorted().toArrayList()
            runOnUiThread {
                ManageBlockedSendersAdapter(
                    activity = this,
                    blockedSenders = blockedSenders,
                    listener = this,
                    recyclerView = binding.manageBlockedSendersList
                ) {
                    addOrEditBlockedSender(it as String)
                }.apply {
                    binding.manageBlockedSendersList.adapter = this
                }

                binding.manageBlockedSendersPlaceholder.beVisibleIf(blockedSenders.isEmpty())
                binding.manageBlockedSendersPlaceholder2.beVisibleIf(blockedSenders.isEmpty())
            }
        }
    }

    private fun addOrEditBlockedSender(sender: String? = null) {
        AddBlockedSenderDialog(this, sender) {
            updateBlockedSenders()
        }
    }
}
