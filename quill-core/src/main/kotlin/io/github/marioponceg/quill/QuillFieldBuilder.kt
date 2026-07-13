package io.github.marioponceg.quill

/**
 * Receiver of the field-builder lambdas: `log.info("user_login") { "userId" to 42 }`.
 * The member-extension [to] shadows `kotlin.to` inside the lambda and converts values
 * automatically — primitives to [QuillValue.Text]/[QuillValue.Number]/[QuillValue.Bool],
 * char sequences that parse as JSON and arbitrary objects (via `toString()`) to
 * [QuillValue.Structured], null to [QuillValue.Null]. Users never see [QuillValue].
 */
public class QuillFieldBuilder internal constructor() {

    private val fields = LinkedHashMap<String, QuillValue>()

    public infix fun String.to(value: Any?) {
        fields[this] = convert(value)
    }

    internal fun build(): Map<String, QuillValue> = fields.toMap()

    private fun convert(value: Any?): QuillValue = when (value) {
        null -> QuillValue.Null
        is Boolean -> QuillValue.Bool(value)
        is Number -> QuillValue.Number(value)
        is Char -> QuillValue.Text(value.toString())
        is CharSequence -> {
            val text = value.toString()
            if (JsonPrettyPrinter.isValidJson(text)) {
                QuillValue.Structured(text)
            } else {
                QuillValue.Text(text)
            }
        }
        else -> QuillValue.Structured(value.toString())
    }
}
