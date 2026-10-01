package com.example.util

object MathTextCleaner {

    /**
     * Cleans mathematical expressions, erasing LaTeX delimiters like $ and $$,
     * converting LaTeX commands into clean unicode representations, and removing
     * raw dollar signs from student homework solutions.
     */
    fun cleanMathFormatting(rawText: String?): String {
        val safeText = rawText?.trim() ?: return ""
        if (safeText.isEmpty()) return ""

        var result: String = safeText

        // 1. Remove LaTeX block delimiters \[ ... \] and \( ... \)
        result = result.replace(Regex("""\\\[(.*?)\\\]""", RegexOption.DOT_MATCHES_ALL)) { match ->
            match.groupValues[1].trim()
        }
        result = result.replace(Regex("""\\\((.*?)\\\)""", RegexOption.DOT_MATCHES_ALL)) { match ->
            match.groupValues[1].trim()
        }

        // 2. Remove double dollar block delimiters $$ ... $$
        result = result.replace(Regex("""\$\$(.*?)\$\$""", RegexOption.DOT_MATCHES_ALL)) { match ->
            match.groupValues[1].trim()
        }

        // 3. Remove single dollar inline delimiters $ ... $
        result = result.replace(Regex("""\$([^\$\n]+?)\$""")) { match ->
            match.groupValues[1].trim()
        }

        // 4. Erase any lingering dollar signs anywhere in the text
        result = result.replace("$$", "").replace("$", "")

        // 5. Convert LaTeX roots first: \sqrt[n]{x} -> ⁿ√(x) and \sqrt{x} -> √(x)
        result = result.replace(Regex("""\\sqrt\[(\d+)\]\{([^{}]+)\}""")) { match ->
            val n = match.groupValues[1]
            val content = match.groupValues[2].trim()
            val supN = toSuperscript(n)
            "$supN√($content)"
        }
        val sqrtRegex = Regex("""\\sqrt\{([^{}]+)\}""")
        var iter = 0
        while (sqrtRegex.containsMatchIn(result) && iter < 10) {
            result = result.replace(sqrtRegex) { match ->
                "√(${match.groupValues[1].trim()})"
            }
            iter++
        }
        result = result.replace(Regex("""\\sqrt\b"""), "√")

        // 6. Remove text / font wrappers: \text{...}, \mathrm{...}, \mathbf{...}, \mathit{...}, etc.
        val textWrapperRegex = Regex("""\\(?:text|mathrm|mathbf|mathit|textbf|mathsf|mathtt)\{([^{}]+)\}""")
        iter = 0
        while (textWrapperRegex.containsMatchIn(result) && iter < 10) {
            result = result.replace(textWrapperRegex) { match ->
                match.groupValues[1]
            }
            iter++
        }

        // 7. Vector and Hat wrappers: \vec{x} -> x, \hat{x} -> x̂
        result = result.replace(Regex("""\\vec\{([^{}]+)\}""")) { match ->
            match.groupValues[1]
        }
        result = result.replace(Regex("""\\hat\{([^{}]+)\}""")) { match ->
            match.groupValues[1]
        }

        // 8. Convert LaTeX fractions: \frac{num}{den} -> (num / den)
        val fracRegex = Regex("""\\frac\{([^{}]+)\}\{([^{}]+)\}""")
        iter = 0
        while (fracRegex.containsMatchIn(result) && iter < 10) {
            result = result.replace(fracRegex) { match ->
                "(${match.groupValues[1].trim()} / ${match.groupValues[2].trim()})"
            }
            iter++
        }
        result = result.replace(Regex("""\\frac\b"""), "")

        // 9. Remove brackets formatting: \left(, \right), etc.
        result = result
            .replace("\\left(", "(")
            .replace("\\right)", ")")
            .replace("\\left[", "[")
            .replace("\\right]", "]")
            .replace("\\left\\{", "{")
            .replace("\\right\\}", "}")
            .replace("\\left|", "|")
            .replace("\\right|", "|")
            .replace("\\left.", "")
            .replace("\\right.", "")
            .replace(Regex("""\\(?:left|right|displaystyle|limits)\b"""), "")

        // 10. Greek letters and common math symbols
        result = result
            .replace("\\Delta", "Δ")
            .replace("\\delta", "δ")
            .replace("\\pi", "π")
            .replace("\\theta", "θ")
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\gamma", "γ")
            .replace("\\lambda", "λ")
            .replace("\\mu", "μ")
            .replace("\\sigma", "σ")
            .replace("\\omega", "ω")
            .replace("\\Omega", "Ω")
            .replace("\\phi", "φ")
            .replace("\\Phi", "Φ")
            .replace("\\pm", "±")
            .replace("\\mp", "∓")
            .replace("\\times", "×")
            .replace("\\div", "÷")
            .replace("\\cdot", "·")
            .replace("\\neq", "≠")
            .replace("\\leq", "≤")
            .replace("\\geq", "≥")
            .replace("\\approx", "≈")
            .replace("\\equiv", "≡")
            .replace("\\infty", "∞")
            .replace("\\rightarrow", "➔")
            .replace("\\longrightarrow", "➔")
            .replace("\\to", "➔")
            .replace("\\leftarrow", "⬅")
            .replace("\\leftrightarrow", "⟷")
            .replace("\\Sigma", "Σ")
            .replace("\\sum", "Σ")
            .replace("\\int", "∫")
            .replace("\\degree", "°")
            .replace("^\\circ", "°")

        // 11. Convert simple LaTeX subscripts with braces: _{0} -> ₀, etc.
        result = result.replace(Regex("""_\{(\d+)\}""")) { match ->
            toSubscript(match.groupValues[1])
        }
        result = result.replace(Regex("""\^\{(\d+)\}""")) { match ->
            toSuperscript(match.groupValues[1])
        }

        // 12. Convert common powers and indices
        result = result
            .replace("^2", "²")
            .replace("^3", "³")
            .replace("^0", "⁰")
            .replace("^1", "¹")
            .replace("^4", "⁴")
            .replace("^5", "⁵")
            .replace("^6", "⁶")
            .replace("^7", "⁷")
            .replace("^8", "⁸")
            .replace("^9", "⁹")
            .replace("^+", "⁺")
            .replace("^-", "⁻")
            .replace("^n", "ⁿ")
            .replace("^x", "ˣ")
            .replace("_0", "₀")
            .replace("_1", "₁")
            .replace("_2", "₂")
            .replace("_3", "₃")
            .replace("_4", "₄")
            .replace("_5", "₅")
            .replace("_6", "₆")
            .replace("_7", "₇")
            .replace("_8", "₈")
            .replace("_9", "₉")

        return result
    }

    private fun toSuperscript(text: String): String {
        val map = mapOf(
            '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
            '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
            '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
            'n' to 'ⁿ', 'x' to 'ˣ'
        )
        return text.map { map[it] ?: it }.joinToString("")
    }

    private fun toSubscript(text: String): String {
        val map = mapOf(
            '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
            '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
            '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
            'a' to 'ₐ', 'e' to 'ₑ', 'x' to 'ₓ'
        )
        return text.map { map[it] ?: it }.joinToString("")
    }
}
