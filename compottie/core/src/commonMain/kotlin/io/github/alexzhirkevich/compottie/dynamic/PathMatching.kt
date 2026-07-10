package io.github.alexzhirkevich.compottie.dynamic

internal fun Iterable<String>.containsWildcards() = any { it == "**" || it == "*" }

internal fun pathMatches(path: String, pattern: String): Boolean = pathMatches(
    path = path.split(LayerPathSeparator),
    pattern = pattern.split(LayerPathSeparator)
)

internal fun pathMatches(path: List<String>, pattern: List<String>): Boolean {
    // Both patter and path are empty => we have found a match
    if (pattern.isEmpty()) return path.isEmpty()
    if (path.isEmpty()) {
        if (pattern.isEmpty()) return true
        // If path is empty but not the pattern, we need to check whether the pattern ends with
        // a globstar that permits 0 matches.
        if (pattern.all { it == "**" }) return true
        return false
    }
    val pathPart = path.first()
    return when (val patternPart = pattern.first()) {
        // For a wildcard we skip the current path element and advance in the pattern list.
        "*" -> pathMatches(
            path = path.dropFirst(),
            pattern = pattern.dropFirst()
        )

        // For a globstar we check a subpath against the same pattern (to match multiple path
        // elements) and the same path against the pattern excluding the globastar
        // (to match 0 path elements).
        "**" -> pathMatches(path = path.dropFirst(), pattern = pattern) ||
                pathMatches(path = path, pattern = pattern.dropFirst())

        // For a named pattern element we check if it equals to the path element and advance
        // in both lists.
        else -> patternPart == pathPart &&
                pathMatches(
                    path = path.dropFirst(),
                    pattern = pattern.dropFirst()
                )
    }
}

// Using subList for performance.
private fun List<String>.dropFirst() = subList(fromIndex = 1, toIndex = size)
