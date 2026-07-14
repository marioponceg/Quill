package io.github.marioponceg.quill.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.marioponceg.quill.Quill
import java.io.IOException
import java.net.SocketTimeoutException

private val log = Quill.logger("Demo")

@Composable
fun DemoScreen() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                section("Levels", levelScenarios())
                section("Fields & beautifier", fieldScenarios())
                section("Throwable & chunking", errorScenarios())
                // section("Conduit HTTP", ...)  ← Task 4
            }
        }
    }
}

private fun LazyListScope.section(
    title: String,
    scenarios: List<Pair<String, () -> Unit>>,
) {
    item { SectionHeader(title) }
    items(scenarios) { (label, action) -> ScenarioButton(label, action) }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun ScenarioButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label)
    }
}

data class Address(val street: String, val city: String)

data class DemoUser(
    val id: Int,
    val name: String,
    val address: Address,
    val roles: List<String>,
)

private fun levelScenarios(): List<Pair<String, () -> Unit>> = listOf(
    "Verbose" to { log.verbose("cache_probe") { "key" to "user:42"; "hit" to false } },
    "Debug" to { log.debug("query_planned") { "table" to "users"; "rows" to 128 } },
    "Info" to { log.info("user_login") { "userId" to 42; "method" to "oauth" } },
    "Warn" to { log.warn("quota_near_limit") { "used" to 91.5; "limit" to 100 } },
    "Error" to { log.error("sync_failed") { "retries" to 3; "cause" to "backend" } },
)

private fun fieldScenarios(): List<Pair<String, () -> Unit>> = listOf(
    "Primitives + null" to {
        log.info("primitives") {
            "text" to "plain"
            "int" to 42
            "double" to 3.14
            "bool" to true
            "missing" to null
        }
    },
    "Nested data class" to {
        log.info("user_loaded") {
            "user" to DemoUser(
                id = 42,
                name = "Mario",
                address = Address(street = "Gran Vía 1", city = "Madrid"),
                roles = listOf("admin", "editor"),
            )
        }
    },
    "Raw JSON string" to {
        log.info("payload_received") {
            "payload" to """{"id":42,"tags":["a","b"],"meta":{"active":true,"score":9.7}}"""
        }
    },
    "List" to { log.info("batch_ready") { "ids" to listOf(1, 2, 3, 5, 8) } },
)

private fun errorScenarios(): List<Pair<String, () -> Unit>> = listOf(
    "Error with exception" to {
        log.error(
            "sync_failed",
            throwable = IOException(
                "timeout after 30s",
                SocketTimeoutException("read timed out"),
            ),
        ) { "retries" to 3 }
    },
    "Giant JSON (chunking)" to {
        val items = (1..120).joinToString(separator = ",") { index ->
            """{"id":$index,"name":"item-$index","enabled":${index % 2 == 0}}"""
        }
        log.debug("bulk_sync") { "items" to "[$items]" }
    },
)
