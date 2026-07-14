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
                // Sections land in the next tasks:
                // section("Levels", levelScenarios())               ← Task 3
                // section("Fields & beautifier", fieldScenarios())  ← Task 3
                // section("Throwable & chunking", errorScenarios()) ← Task 3
                // section("Conduit HTTP", ...)                      ← Task 4
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
