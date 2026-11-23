package org.fossify.messages.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.value
import org.fossify.messages.databinding.DialogAddBlockedSenderBinding
import org.fossify.messages.extensions.config

class AddBlockedSenderDialog(val activity: BaseSimpleActivity, private val originalSender: String? = null, val callback: () -> Unit) {
    init {
        val binding = DialogAddBlockedSenderBinding.inflate(activity.layoutInflater).apply {
            if (originalSender != null) {
                addBlockedSenderEdittext.setText(originalSender)
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(org.fossify.commons.R.string.ok, null)
            .setNegativeButton(org.fossify.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog ->
                    alertDialog.showKeyboard(binding.addBlockedSenderEdittext)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val newBlockedSender = binding.addBlockedSenderEdittext.value
                        if (originalSender != null && newBlockedSender != originalSender) {
                            activity.config.removeBlockedSender(originalSender)
                        }

                        if (newBlockedSender.isNotEmpty()) {
                            activity.config.addBlockedSender(newBlockedSender)
                        }

                        callback()
                        alertDialog.dismiss()
                    }
                }
            }
    }
}
