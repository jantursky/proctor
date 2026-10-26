package com.jantursky.proctor.sample

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class Category { Billing, Bug, Account, Feature, Other }

/** What the extraction step pulls out of a ticket. Every field is optional because tickets often lack them. */
@Serializable
data class TicketDetails(
    val orderId: String? = null,
    val product: String? = null,
    val urgent: Boolean = false,
)

object TicketPrompts {
    /** The wording the first release shipped with. */
    fun classifyV1(ticket: String): String =
        "Classify this support ticket as one of Billing, Bug, Account, Feature or Other. " +
            "Reply with the category only.\n\nTicket: $ticket"

    /** Adds a definition per category after v1 confused account and billing problems. */
    fun classifyV2(ticket: String): String =
        """
        You sort customer support tickets. Reply with exactly one word from this list and nothing else:
        Billing (charges, refunds, invoices), Bug (crashes, errors, broken behavior),
        Account (login, password, profile), Feature (requests for new functionality), Other.

        Ticket: $ticket
        Category:
        """.trimIndent()

    fun extract(ticket: String): String =
        """
        Extract details from this support ticket. Reply with one JSON object and nothing else, with the keys
        "orderId" (string or null), "product" (string or null) and "urgent" (true when the customer says it is
        urgent or blocking, otherwise false).

        Ticket: $ticket
        """.trimIndent()
}

private val lenientJson = Json { ignoreUnknownKeys = true }

/** The shipped feature: ask the on-device model, read its answer. Used by the app and measured by the suites. */
class TicketTriage(
    private val ask: suspend (prompt: String) -> String,
) {
    suspend fun classify(ticket: String): Category? = parseCategory(ask(TicketPrompts.classifyV2(ticket)))

    suspend fun details(ticket: String): TicketDetails? =
        try {
            lenientJson.decodeFromString(TicketDetails.serializer(), jsonObjectIn(ask(TicketPrompts.extract(ticket))))
        } catch (e: IllegalArgumentException) {
            null
        }
}

fun parseCategory(output: String): Category? {
    val word =
        output
            .trim()
            .substringBefore('\n')
            .substringBefore(' ')
            .trim('.', ',', ':', '"', '*', '`')
    return Category.entries.firstOrNull { it.name.equals(word, ignoreCase = true) }
}

internal fun jsonObjectIn(text: String): String {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    return if (start in 0..<end) text.substring(start, end + 1) else text
}
