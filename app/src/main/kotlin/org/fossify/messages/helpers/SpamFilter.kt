package org.fossify.messages.helpers

import android.content.Context
import android.provider.BlockedNumberContract
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getBlockedNumbers
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.extensions.isNumberBlocked
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.messages.helpers.ReceiverUtils.isMessageFilteredOut

/**
 * Single place that decides whether an incoming message is spam.
 *
 * A message is spam when at least one of the existing blocking rules matches:
 *  - the body contains one of the blocked keywords
 *  - the sender is in the blocked numbers list (phone numbers, short codes, patterns with `*`
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
     * Checks the sender against the blocked numbers list.
     *
     * 1. Android's own check, the one the system dialer and other messaging apps rely on.
     * 2. The list read by the Fossify Commons library. Its comparison only looks at the last 9
     *    digits, which misses short codes that arrive with a country code (`100020` vs
     *    `+98100020`) and character based sender IDs, so the entries are also compared by
     *    [isInBlockedList].
     */
    fun isSenderBlocked(context: Context, address: String): Boolean {
        if (address.isBlank()) return false
        if (isBlockedBySystem(context, address)) return true

        val blockedNumbers = context.getBlockedNumbers()
        if (blockedNumbers.isEmpty()) return false

        if (context.isNumberBlocked(address, blockedNumbers)) return true

        return isInBlockedList(address, blockedNumbers.map { it.number })
    }

    private fun isBlockedBySystem(context: Context, address: String): Boolean {
        return runCatching { BlockedNumberContract.isBlocked(context, address) }.getOrDefault(false)
    }

    /**
     * Pure function (no Android dependencies) so it can be unit tested.
     *
     * Entries can be phone numbers (any format: `0912...`, `+98912...`, short codes), sender
     * names (case-insensitive, e.g. `google` blocks `Google`) or patterns with `*` (`AD-*`).
     */
    fun isInBlockedList(address: String, entries: Collection<String>): Boolean {
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
        val firstDigits = first.filter { it.isDigit() }.trimStart('0')
        val secondDigits = second.filter { it.isDigit() }.trimStart('0')
        if (firstDigits.isEmpty() || secondDigits.isEmpty()) return false
        if (firstDigits == secondDigits) return true

        val firstIsLonger = firstDigits.length > secondDigits.length
        val longDigits = if (firstIsLonger) firstDigits else secondDigits
        val shortDigits = if (firstIsLonger) secondDigits else firstDigits
        val longOriginal = (if (firstIsLonger) first else second).trim()

        // the longer one has a country code in front of the other one
        val extraDigits = longDigits.length - shortDigits.length
        if (extraDigits !in 1..MAX_COUNTRY_CODE_LENGTH || !longDigits.endsWith(shortDigits)) return false

        // short codes (3000, 100020) only match when the country code is explicit (+98100020)
        val hasExplicitCountryCode = longOriginal.startsWith("+") || longOriginal.startsWith("00")
        return hasExplicitCountryCode || shortDigits.length >= MIN_LENGTH_WITHOUT_EXPLICIT_COUNTRY_CODE
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

    private const val MAX_COUNTRY_CODE_LENGTH = 3
    private const val MIN_LENGTH_WITHOUT_EXPLICIT_COUNTRY_CODE = 7
}
