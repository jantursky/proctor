package com.jantursky.proctor.sample

import com.jantursky.proctor.Grade
import com.jantursky.proctor.GradeContext
import com.jantursky.proctor.Grader
import com.jantursky.proctor.Sampling
import com.jantursky.proctor.evalSuite
import kotlinx.serialization.json.Json

/** The mean score each sample suite needs to pass. The dataset editor rebuilds the suites with the same rule. */
internal val PASS_SCORES = mapOf("ticket-classifier" to 0.8, "ticket-extraction" to 0.7)

private val sampling = Sampling(temperature = 0.2, topK = 16)

/** Classification: two prompt versions on the same cases, so a run shows whether the rewrite helped. */
val classifierSuite =
    evalSuite("ticket-classifier") {
        prompt(version = "v1", TicketPrompts::classifyV1)
        prompt(version = "v2", TicketPrompts::classifyV2)

        case("My card was charged twice for the same order", id = "double-charge") { expect(Category.Billing) }
        case("I need a refund for the annual plan I never used", id = "refund") { expect(Category.Billing) }
        case("The invoice shows a company name I did not enter", id = "invoice-name") { expect(Category.Billing) }
        case("The app crashes as soon as I open the camera screen", id = "crash-camera") { expect(Category.Bug) }
        case("Search returns nothing even though the item exists", id = "search-empty") { expect(Category.Bug) }
        case("Dark mode turns the text black on a black background", id = "dark-mode-contrast") { expect(Category.Bug) }
        case("I forgot my password and the reset email never arrives", id = "reset-email") { expect(Category.Account) }
        case("Please change the email address on my profile", id = "change-email") { expect(Category.Account) }
        case("Can you add an option to export my data as CSV?", id = "export-csv") { expect(Category.Feature) }
        case("It would be great to have a widget for the home screen", id = "widget") { expect(Category.Feature) }
        case("Thanks for the quick help last week, just saying hi", id = "thanks") { expect(Category.Other) }
        case("Where is your office located?", id = "office") { expect(Category.Other) }

        graders { exactMatch(ignoreCase = true) }
        repeat(3)
        sampling(sampling)
        passWhen { score >= PASS_SCORES.getValue("ticket-classifier") }
    }

/** Structured extraction: valid JSON first, then the right values. */
val extractionSuite =
    evalSuite("ticket-extraction") {
        prompt(version = "v1", TicketPrompts::extract)

        case(
            "Order 48213: the blender stopped working. This is urgent, I need it for an event tomorrow.",
            id = "blender",
        ) {
            expect("""{"orderId":"48213","product":"blender","urgent":true}""")
        }
        case("My headphones arrived scratched. Order A-77.", id = "headphones") {
            expect("""{"orderId":"A-77","product":"headphones","urgent":false}""")
        }
        case("I cannot log in to the web dashboard, blocking my whole team right now.", id = "dashboard") {
            expect("""{"orderId":null,"product":"web dashboard","urgent":true}""")
        }
        case("Just wondering if you ship to Norway.", id = "shipping") {
            expect("""{"orderId":null,"product":null,"urgent":false}""")
        }

        graders {
            jsonSchema<TicketDetails>()
            add(DetailsMatch)
        }
        repeat(3)
        sampling(sampling)
        passWhen { score >= PASS_SCORES.getValue("ticket-extraction") }
    }

/** A custom grader: the order id and the urgency must match, the product only loosely (case-insensitive, contained). */
object DetailsMatch : Grader {
    override val name = "detailsMatch"

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun grade(context: GradeContext): Grade {
        val expectedJson = context.expected ?: return Grade.notGraded("The case has no expected value")
        val expected = json.decodeFromString(TicketDetails.serializer(), expectedJson)
        val actual =
            try {
                json.decodeFromString(TicketDetails.serializer(), jsonObjectIn(context.output))
            } catch (e: IllegalArgumentException) {
                return Grade.failed(rationale = "No readable details in the output")
            }
        val problems =
            buildList {
                if (actual.orderId != expected.orderId) add("orderId ${actual.orderId} instead of ${expected.orderId}")
                if (actual.urgent != expected.urgent) add("urgent ${actual.urgent} instead of ${expected.urgent}")
                if (!productMatches(
                        expected.product,
                        actual.product,
                    )
                ) {
                    add("product ${actual.product} instead of ${expected.product}")
                }
            }
        val score = (3 - problems.size) / 3.0
        return if (problems.isEmpty()) Grade.passed() else Grade.failed(score, problems.joinToString("; "))
    }

    private fun productMatches(
        expected: String?,
        actual: String?,
    ): Boolean =
        when {
            expected == null || actual == null -> expected == actual
            else -> actual.contains(expected, ignoreCase = true) || expected.contains(actual, ignoreCase = true)
        }
}

val sampleSuites = listOf(classifierSuite, extractionSuite)
