package com.skyd.podaura.podnya.core

/** Built-in model IDs in picker order, with each provider's default first. */
internal object AssistantModelIds {
    // These official OpenAI models require Responses for Podnya's tool calls.
    val openAiResponses = listOf("gpt-6.1-sol", "gpt-6-luna", "gpt-6-astra", "gpt-6.1-terra")
    val deepSeek = listOf("deepseek-flash", "deepseek-v4-pro")
    val claude = listOf("claude-sonnet-5-5", "claude-haiku-5-5", "claude-opus-5-5")
    val gemini = listOf("gemini-4-hydrogen", "gemini-3.8-flash")
    val grok = listOf("grok-4.7")
}
