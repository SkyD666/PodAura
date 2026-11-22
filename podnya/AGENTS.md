# User input and model responsibilities

Before implementing a decision in Kotlin, ask whether a capable model could make a better choice from the conversation and available data. If a fixed rule would override that judgment, expose the relevant data to the model and validate its choice in Kotlin.

Give the user's natural-language messages to the model. The model chooses the capability, interprets references and constraints, supplies structured arguments, and asks the user for clarification when needed.

Do not parse the user's words in Kotlin or scan earlier user messages for arguments. Do not use regular expressions, keyword lists, hard-coded phrases, or string matching to infer intent, extract links, interpret dates or ordinals, or rank candidates against the user's wording.

Use Kotlin to validate structured arguments against application data, check resource availability, and persist and execute accepted actions. Test these boundaries with structured responses from the project's local model substitutes.

# Design questions

When explaining a field or design decision, trace a real call from the producer through any mappings to the consumer. Explain what goes in, who uses the result, and what behavior it affects. If information is lost, identify the first mapping that drops it before proposing a change to shared code.
