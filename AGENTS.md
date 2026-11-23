# Kotlin Code Style

Apply these rules when adding or modifying Kotlin code in this project:

1. Use braces for `if` and `else` branches whose bodies span multiple lines, including multiline function calls, composable calls, and calls with lambdas. Never place a multiline body directly after an unbraced branch.
2. When a `Modifier` has multiple chained calls, put `Modifier` on its own line and each call on a separate line. A single call may remain on one line.
3. Do not use semicolons to place multiple statements on one line. Put each statement on its own line.
4. When a function or constructor call has many arguments, use named arguments (`parameterName = value`) for every argument. Do not mix positional and named arguments in such calls.

Preserve existing user changes and avoid reformatting unrelated files.

# Class References

In all source code, use imports and simple class names when there is no naming conflict. Do not use package-qualified class names unless necessary to resolve a naming conflict.

# Package Organization

Do not create a subpackage for a single file. Keep that file in its parent package, and introduce a subpackage only when it groups multiple related files. Do not create extra files merely to justify a subpackage.

# Database Naming

Follow the existing Room schema conventions when adding tables and columns:

- Use singular PascalCase table names, such as `Article`, `TranslationProfile`, and `ConversationTurn`, and lowerCamelCase column names, such as `articleId`, `createdAt`, and `modelConfigId`.
- Define table names as `*_TABLE_NAME` constants and column names as `*_COLUMN` constants. Reference these constants in Room annotations, DAO queries, foreign keys, and indices, following nearby code.
- Follow Room's existing default index naming pattern (`index_<TableName>_<columnName>...`); migration SQL must use the same names as the schema.
- Preserve existing schema names and legacy exceptions. Do not rename unrelated tables or columns to enforce these conventions.
