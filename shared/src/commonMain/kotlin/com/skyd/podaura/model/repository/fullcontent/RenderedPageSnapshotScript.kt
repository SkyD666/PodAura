package com.skyd.podaura.model.repository.fullcontent

internal object RenderedPageSnapshotScript {
    const val MAX_HTML_CHARS = 5 * 1024 * 1024

    fun stabilityState(key: String): String = """
        (() => {
          const key = ${jsString(key)};
          const now = Date.now();
          if (!window[key]) {
            const state = { lastMutation: now };
            const observer = new MutationObserver(() => { state.lastMutation = Date.now(); });
            if (document.documentElement) {
              observer.observe(document.documentElement, {
                subtree: true, childList: true, characterData: true, attributes: true
              });
            }
            state.observer = observer;
            window[key] = state;
          }
          return JSON.stringify({
            ready: document.readyState,
            quietMillis: now - window[key].lastMutation
          });
        })()
    """.trimIndent()

    val snapshot: String = """
        (() => {
          if (!document.documentElement) return JSON.stringify({ html: "", url: location.href });
          const safeProperties = [
            "color", "font-family", "font-size", "font-style", "font-weight",
            "letter-spacing", "line-height", "text-align", "text-decoration",
            "text-indent", "white-space", "direction", "background-color",
            "border-top-color", "border-right-color", "border-bottom-color",
            "border-left-color", "border-top-style", "border-right-style",
            "border-bottom-style", "border-left-style", "border-top-width",
            "border-right-width", "border-bottom-width", "border-left-width",
            "border-radius", "border-collapse", "caption-side", "margin-top",
            "margin-right", "margin-bottom", "margin-left", "padding-top",
            "padding-right", "padding-bottom", "padding-left", "aspect-ratio",
            "list-style-type", "list-style-position", "max-width", "width",
            "height", "object-fit", "opacity", "overflow-wrap", "table-layout",
            "text-transform", "vertical-align", "word-break", "word-wrap"
          ];
          const original = document.documentElement;
          const clone = original.cloneNode(true);
          const originals = [original, ...original.querySelectorAll("*")];
          const clones = [clone, ...clone.querySelectorAll("*")];
          const count = Math.min(originals.length, clones.length, 50000);
          for (let i = 0; i < count; i++) {
            const source = originals[i];
            const target = clones[i];
            const tag = source.tagName ? source.tagName.toLowerCase() : "";
            if (["script", "noscript", "iframe", "object", "embed", "form", "input",
                 "button", "textarea", "select", "option"].includes(tag)) continue;
            const style = getComputedStyle(source);
            if (style.display === "none" || style.visibility === "hidden") {
              target.setAttribute("data-podaura-remove", "");
              continue;
            }
            const declarations = [];
            for (const property of safeProperties) {
              const value = style.getPropertyValue(property).trim();
              if (!value || value.length > 256 || /url\s*\(|expression\s*\(|behavior\s*:/i.test(value)) continue;
              declarations.push(property + ": " + value);
            }
            if (declarations.length) target.setAttribute("style", declarations.join("; "));
          }
          clone.querySelectorAll(
            "script,noscript,iframe,object,embed,form,input,button,textarea,select,option," +
            "[data-podaura-remove]"
          ).forEach(node => node.remove());
          clone.querySelectorAll("*").forEach(node => {
            [...node.attributes].forEach(attribute => {
              if (/^on/i.test(attribute.name)) node.removeAttribute(attribute.name);
            });
          });
          return JSON.stringify({
            html: "<!doctype html>" + clone.outerHTML,
            url: location.href
          });
        })()
    """.trimIndent()

    private fun jsString(value: String): String = buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(character)
            }
        }
        append('"')
    }
}
