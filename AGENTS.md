# Kotlin Code Style

Apply these rules when adding or modifying Kotlin code in this project:

1. Use braces for `if` and `else` branches whose bodies span multiple lines, including multiline function calls, composable calls, and calls with lambdas. Never place a multiline body directly after an unbraced branch.
2. When a `Modifier` has multiple chained calls, put `Modifier` on its own line and each call on a separate line. A single call may remain on one line.
3. Do not use semicolons to place multiple statements on one line. Put each statement on its own line.
4. When a function or constructor call has many arguments, use named arguments (`parameterName = value`) for every argument. Do not mix positional and named arguments in such calls.

Preserve existing user changes and avoid reformatting unrelated files.
