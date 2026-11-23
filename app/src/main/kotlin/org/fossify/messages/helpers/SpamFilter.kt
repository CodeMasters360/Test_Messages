package org.fossify.messages.helpers

import android.content.Context
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getBlockedNumbers
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.extensions.isNumberBlocked
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.messages.extensions.config
import org.fossify.messages.helpers.ReceiverUtils.isMessageFilteredOut

/**
 * Single place that decides whether an incoming message is spam.
 *
 * A message is spam when at least one of the existing blocking rules matches:
 *  - the body contains one of the blocked keywords
 *  - the sender is in the blocked numbers list (phone numbers, patterns with `*`
 *    and character based sender names like "Google")
 *  - "block unknown numbers" is enabled and the sender is not in the contacts
 */
object SpamFilter {

    fun isSpam(context: Context, address: String, body: String): Boolean {
        if (isMessageFilteredOut(context, body)) return true
        if (isSenderBlocked(context, address)) return true
        return isBlockedAsUnknownNumber(context, address)
    }

    fun isBlockedAsUnknownNumber(context: Context, address: String): Boolean {
        if (!context.baseConfig.blockUnknownNumbers) return false

        val privateCursor = context.getMyContactsCursor(
            favoritesOnly = false,
            withPhoneNumbersOnly = true
        )
        val result = SimpleContactsHelper(context).existsSync(address, privateCursor)
        return result == ContactLookupResult.NotFound
    }

    /**
     * Checks the sender against the blocked numbers and senders.
     *
     * 1. The app's own list (Settings > Blocked numbers and senders). Numbers have to be blocked
     *    here for their messages to reach the Spam folder: Android drops the texts of the
     *    numbers in its own (system) blocked list before the app can even see them.
     * 2. The system blocked list, for what can still reach the app (e.g. MMS). In addition to
     *    what the Fossify Commons library does (digits based matching) this also supports
     *    character based sender IDs, e.g. a blocked entry `google` blocks `Google` and `GOOGLE`.
     *
     * `*` can be used as a wildcard, e.g. `AD-*` or `*bank*`.
     */
    fun isSenderBlocked(context: Context, address: String): Boolean {
        if (address.isBlank()) return false

        val ownList = context.config.blockedSenders
        if (ownList.isNotEmpty() && isSenderInOwnList(address, ownList)) return true

        val blockedNumbers = context.getBlockedNumbers()
        if (blockedNumbers.isEmpty()) return false

        if (context.isNumberBlocked(address, blockedNumbers)) return true

        return isAlphanumericSenderBlocked(address, blockedNumbers.map { it.number })
    }

    /**
     * Pure function (no Android dependencies) so it can be unit tested.
     *
     * Entries can be phone numbers (any format: `0912...`, `+98912...`, short codes), sender
     * names (case-insensitive) or patterns with `*`.
     */
    fun isSenderInOwnList(address: String, entries: Collection<String>): Boolean {
        val sender = address.trim()
        if (sender.isEmpty()) return false

        return entries.any { rawEntry ->
            val entry = rawEntry.trim()
            when {
                entry.isEmpty() -> false
                entry.contains('*') || entry.any { it.isLetter() } || sender.any { it.isLetter() } ->
                    matchesIgnoreCase(sender, entry)

                else -> isSameNumber(sender, entry)
            }
        }
    }

    // The same number can be written in many ways: 0912..., +98912..., 98912...
    private fun isSameNumber(first: String, second: String): Boolean {
        val firstDigits = first.filter { it.isDigit() }
        val secondDigits = second.filter { it.isDigit() }
        if (firstDigits.isEmpty() || secondDigits.isEmpty()) return false
        if (firstDigits == secondDigits) return true

        // short codes have to match exactly, longer numbers are compared by their last digits
        return firstDigits.length >= 7 && secondDigits.length >= 7 &&
            firstDigits.takeLast(9) == secondDigits.takeLast(9)
    }

    /**
     * Pure function (no Android dependencies) so it can be unit tested.
     */
    fun isAlphanumericSenderBlocked(address: String, blockedEntries: List<String>): Boolean {
        val sender = address.trim()
        if (sender.none { it.isLetter() }) return false

        return blockedEntries.any { entry ->
            val pattern = entry.trim()
            pattern.isNotEmpty() && matchesIgnoreCase(sender, pattern)
        }
    }

    private fun matchesIgnoreCase(sender: String, pattern: String): Boolean {
        if (!pattern.contains('*')) {
            return sender.equals(pattern, ignoreCase = true)
        }

        val regex = pattern
            .split('*')
            .joinToString(separator = ".*") { Regex.escape(it) }
            .toRegex(RegexOption.IGNORE_CASE)
        return regex.matches(sender)
    }
}
