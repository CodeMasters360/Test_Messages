package org.fossify.messages.dialogs

import android.view.*
import androidx.appcompat.widget.PopupMenu
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.copyToClipboard
import org.fossify.commons.extensions.getPopupMenuTheme
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.setupViewBackground
import org.fossify.commons.interfaces.RefreshRecyclerViewListener
import org.fossify.commons.views.MyRecyclerView
import org.fossify.messages.R
import org.fossify.messages.databinding.ItemManageBlockedSenderBinding
import org.fossify.messages.extensions.config

class ManageBlockedSendersAdapter(
    activity: BaseSimpleActivity, var blockedSenders: ArrayList<String>, val listener: RefreshRecyclerViewListener?,
    recyclerView: MyRecyclerView, itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick) {
    init {
        setupDragListener(true)
    }

    override fun getActionMenuId() = R.menu.cab_blocked_senders

    override fun prepareActionMode(menu: Menu) {
        menu.apply {
            findItem(R.id.cab_copy_sender).isVisible = isOneItemSelected()
        }
    }

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_copy_sender -> copySenderToClipboard()
            R.id.cab_delete -> deleteSelection()
        }
    }

    override fun getSelectableItemCount() = blockedSenders.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = blockedSenders.getOrNull(position)?.hashCode()

    override fun getItemKeyPosition(key: Int) = blockedSenders.indexOfFirst { it.hashCode() == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemManageBlockedSenderBinding.inflate(layoutInflater, parent, false)
        return createViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val blockedSender = blockedSenders[position]
        holder.bindView(blockedSender, allowSingleClick = true, allowLongClick = true) { itemView, _ ->
            setupView(itemView, blockedSender)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = blockedSenders.size

    private fun getSelectedItems() = blockedSenders.filter { selectedKeys.contains(it.hashCode()) }

    private fun setupView(view: View, blockedSender: String) {
        ItemManageBlockedSenderBinding.bind(view).apply {
            root.setupViewBackground(activity)
            manageBlockedSenderHolder.isSelected = selectedKeys.contains(blockedSender.hashCode())
            manageBlockedSenderTitle.apply {
                text = blockedSender
                setTextColor(textColor)
            }

            overflowMenuIcon.drawable.apply {
                mutate()
                setTint(activity.getProperTextColor())
            }

            overflowMenuIcon.setOnClickListener {
                showPopupMenu(overflowMenuAnchor, blockedSender)
            }
        }
    }

    private fun showPopupMenu(view: View, blockedSender: String) {
        finishActMode()
        val theme = activity.getPopupMenuTheme()
        val contextTheme = ContextThemeWrapper(activity, theme)

        PopupMenu(contextTheme, view, Gravity.END).apply {
            inflate(getActionMenuId())
            setOnMenuItemClickListener { item ->
                val blockedSenderId = blockedSender.hashCode()
                when (item.itemId) {
                    R.id.cab_copy_sender -> {
                        executeItemMenuOperation(blockedSenderId) {
                            copySenderToClipboard()
                        }
                    }

                    R.id.cab_delete -> {
                        executeItemMenuOperation(blockedSenderId) {
                            deleteSelection()
                        }
                    }
                }
                true
            }
            show()
        }
    }

    private fun executeItemMenuOperation(blockedSenderId: Int, callback: () -> Unit) {
        selectedKeys.add(blockedSenderId)
        callback()
        selectedKeys.remove(blockedSenderId)
    }

    private fun copySenderToClipboard() {
        val selectedSender = getSelectedItems().firstOrNull() ?: return
        activity.copyToClipboard(selectedSender)
        finishActMode()
    }

    private fun deleteSelection() {
        val deleteBlockedSenders = HashSet<String>(selectedKeys.size)
        val positions = getSelectedItemPositions()

        getSelectedItems().forEach {
            deleteBlockedSenders.add(it)
            activity.config.removeBlockedSender(it)
        }

        blockedSenders.removeAll(deleteBlockedSenders)
        removeSelectedItems(positions)
        if (blockedSenders.isEmpty()) {
            listener?.refreshItems()
        }
    }
}
