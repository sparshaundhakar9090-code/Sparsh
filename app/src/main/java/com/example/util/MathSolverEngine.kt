package com.example.util

import kotlin.math.abs
import kotlin.math.sqrt

object MathSolverEngine {

    /**
     * Attempts to identify and solve mathematical equations and problems,
     * returning a complete, formatted step-by-step solution with a highlighted Final Answer.
     */
    fun trySolve(input: String): String? {
        val clean = input.trim()

        // 1. Division equation: x / 4 = 15
        val divMatch = Regex(
            """(?:solve\s*(?:for\s*[a-zA-Z]\s*)?:?\s*)?([a-zA-Z])\s*/\s*(\d+(?:\.\d+)?)\s*=\s*(-?\d+(?:\.\d+)?)\??$""",
            RegexOption.IGNORE_CASE
        ).find(clean)
        if (divMatch != null) {
            val (v, divStr, rhsStr) = divMatch.destructured
            val divisor = divStr.toDoubleOrNull() ?: return null
            val rhs = rhsStr.toDoubleOrNull() ?: return null
            val ans = rhs * divisor
            val ansFormatted = formatNumber(ans)
            return """
### 📐 Algebra: Linear Equation Solution

**Given Equation:**
$v / $divStr = $rhsStr

**Step 1: Isolate the Variable ($v)**
Multiply both sides of the equation by $divStr:
$v = $rhsStr × $divStr

**Step 2: Compute the Result**
$v = $ansFormatted

**Step 3: Verification**
Substitute $v = $ansFormatted into the original equation:
$ansFormatted / $divStr = $rhsStr (True ✓)

**Final Answer:**
$v = $ansFormatted

💡 **Study Tip:** The inverse operation of division is multiplication. Multiplying both sides by the denominator cancels it out completely.
            """.trimIndent()
        }

        // 2. Simple or two-step linear equation: ax + b = cx + d (e.g. x + 19 = 200, 2x + 5 = 25, 3x - 7 = 20)
        val linearSolution = trySolveLinearEquation(clean)
        if (linearSolution != null) {
            return linearSolution
        }

        // 3. Simple quadratic equation (e.g. x^2 - 16 = 0, x^2 = 25, ax^2 + bx + c = 0)
        val quadSolution = trySolveQuadratic(clean)
        if (quadSolution != null) {
            return quadSolution
        }

        // 4. Percentage calculation: 20% of 150
        val percentSolution = trySolvePercentage(clean)
        if (percentSolution != null) {
            return percentSolution
        }

        // 5. Arithmetic: 2+2, 15*8, 100/4
        val arithmeticSolution = trySolveArithmetic(clean)
        if (arithmeticSolution != null) {
            return arithmeticSolution
        }

        return null
    }

    private fun trySolveLinearEquation(input: String): String? {
        val sanitized = input.replace("−", "-").trim().trimEnd('?')
        // Check if there is an equals sign and a variable letter
        if (!sanitized.contains("=")) return null

        // Extract equation if preceded by "solve", "find x in", etc.
        var eqString = sanitized
        val prefixes = listOf(
            "solve for x:", "solve for x", "solve for",
            "solve:", "solve", "find x in:", "find x in",
            "find x:", "find x", "find:", "find",
            "evaluate:", "evaluate", "calculate:", "calculate"
        )
        for (p in prefixes) {
            if (eqString.startsWith(p, ignoreCase = true)) {
                eqString = eqString.substring(p.length).trim()
                break
            }
        }
        eqString = eqString.trimStart(':').trim()

        val parts = eqString.split("=")
        if (parts.size != 2) return null

        val leftStr = parts[0].trim()
        val rightStr = parts[1].trim()

        // Find primary variable letter
        val varLetter = findVariable(leftStr) ?: findVariable(rightStr) ?: return null

        // Don't treat equations with exponents as linear
        if (leftStr.contains("^") || rightStr.contains("^") || leftStr.contains("²") || rightStr.contains("²")) {
            return null
        }

        val (a, b) = parseLinearSide(leftStr, varLetter) ?: return null
        val (c, d) = parseLinearSide(rightStr, varLetter) ?: return null

        // Equation: a*v + b = c*v + d
        val netCoeff = a - c
        val netConst = d - b

        if (abs(netCoeff) < 1e-9) {
            return if (abs(netConst) < 1e-9) {
                """
### 📐 Algebra: Linear Equation

**Given Equation:**
$eqString

**Analysis:**
Both sides simplify to identical expressions.

**Final Answer:**
Infinitely many solutions (All real numbers satisfy this identity).
                """.trimIndent()
            } else {
                """
### 📐 Algebra: Linear Equation

**Given Equation:**
$eqString

**Analysis:**
Simplifies to $b = $d, which is a contradiction.

**Final Answer:**
No solution (The equation is inconsistent).
                """.trimIndent()
            }
        }

        val solution = netConst / netCoeff
        val solFormatted = formatNumber(solution)

        // Build step-by-step explanation tailored to the specific form
        val stepsBuilder = StringBuilder()

        stepsBuilder.append("### 📐 Algebra: Linear Equation Solution\n\n")
        stepsBuilder.append("**Given Equation:**\n")
        stepsBuilder.append("$eqString\n\n")

        if (c == 0.0) {
            // Standard form: a*v + b = d
            if (a == 1.0) {
                // Form: v + b = d or v - b = d (like x + 19 = 200)
                if (b > 0) {
                    stepsBuilder.append("**Step 1: Isolate the Variable ($varLetter)**\n")
                    stepsBuilder.append("Subtract ${formatNumber(b)} from both sides of the equation:\n")
                    stepsBuilder.append("$varLetter = ${formatNumber(d)} - ${formatNumber(b)}\n\n")
                } else if (b < 0) {
                    val absB = formatNumber(abs(b))
                    stepsBuilder.append("**Step 1: Isolate the Variable ($varLetter)**\n")
                    stepsBuilder.append("Add $absB to both sides of the equation:\n")
                    stepsBuilder.append("$varLetter = ${formatNumber(d)} + $absB\n\n")
                } else {
                    stepsBuilder.append("**Step 1: The variable is already isolated**\n\n")
                }
                stepsBuilder.append("**Step 2: Calculate the Result**\n")
                stepsBuilder.append("$varLetter = $solFormatted\n\n")
            } else {
                // Form: a*v + b = d (like 2x + 5 = 25)
                var currentRhs = d
                if (b != 0.0) {
                    if (b > 0) {
                        currentRhs -= b
                        stepsBuilder.append("**Step 1: Move Constant Term to the Right Side**\n")
                        stepsBuilder.append("Subtract ${formatNumber(b)} from both sides:\n")
                        stepsBuilder.append("${formatNumber(a)}$varLetter = ${formatNumber(d)} - ${formatNumber(b)}\n")
                        stepsBuilder.append("${formatNumber(a)}$varLetter = ${formatNumber(currentRhs)}\n\n")
                    } else {
                        currentRhs -= b
                        val absB = formatNumber(abs(b))
                        stepsBuilder.append("**Step 1: Move Constant Term to the Right Side**\n")
                        stepsBuilder.append("Add $absB to both sides:\n")
                        stepsBuilder.append("${formatNumber(a)}$varLetter = ${formatNumber(d)} + $absB\n")
                        stepsBuilder.append("${formatNumber(a)}$varLetter = ${formatNumber(currentRhs)}\n\n")
                    }
                    stepsBuilder.append("**Step 2: Solve for $varLetter**\n")
                    stepsBuilder.append("Divide both sides by the coefficient ${formatNumber(a)}:\n")
                    stepsBuilder.append("$varLetter = ${formatNumber(currentRhs)} / ${formatNumber(a)}\n")
                    stepsBuilder.append("$varLetter = $solFormatted\n\n")
                } else {
                    stepsBuilder.append("**Step 1: Solve for $varLetter**\n")
                    stepsBuilder.append("Divide both sides by ${formatNumber(a)}:\n")
                    stepsBuilder.append("$varLetter = ${formatNumber(d)} / ${formatNumber(a)}\n")
                    stepsBuilder.append("$varLetter = $solFormatted\n\n")
                }
            }
        } else {
            // Both sides have variables: a*v + b = c*v + d
            stepsBuilder.append("**Step 1: Collect Variable Terms on the Left Side**\n")
            stepsBuilder.append("Subtract ${formatNumber(c)}$varLetter from both sides:\n")
            stepsBuilder.append("(${formatNumber(a)} - ${formatNumber(c)})$varLetter + ${formatNumber(b)} = ${formatNumber(d)}\n")
            stepsBuilder.append("${formatNumber(netCoeff)}$varLetter + ${formatNumber(b)} = ${formatNumber(d)}\n\n")

            if (b != 0.0) {
                stepsBuilder.append("**Step 2: Move Constant to the Right Side**\n")
                stepsBuilder.append("${formatNumber(netCoeff)}$varLetter = ${formatNumber(d)} - ${formatNumber(b)}\n")
                stepsBuilder.append("${formatNumber(netCoeff)}$varLetter = ${formatNumber(netConst)}\n\n")
            }

            stepsBuilder.append("**Step 3: Divide by Coefficient**\n")
            stepsBuilder.append("$varLetter = ${formatNumber(netConst)} / ${formatNumber(netCoeff)}\n")
            stepsBuilder.append("$varLetter = $solFormatted\n\n")
        }

        // Verification step
        stepsBuilder.append("**Verification:**\n")
        val leftCheck = formatNumber(a * solution + b)
        val rightCheck = formatNumber(c * solution + d)
        stepsBuilder.append("Substitute $varLetter = $solFormatted back into original equation:\n")
        stepsBuilder.append("LHS: $leftCheck, RHS: $rightCheck (Balanced ✓)\n\n")

        stepsBuilder.append("**Final Answer:**\n")
        stepsBuilder.append("$varLetter = $solFormatted\n\n")

        stepsBuilder.append("💡 **Study Tip:** To isolate a variable in any linear equation, perform inverse operations (addition/subtraction, then multiplication/division) equally on both sides.")

        return stepsBuilder.toString()
    }

    private fun trySolveQuadratic(input: String): String? {
        val sanitized = input.replace("−", "-").replace("²", "^2").trim().trimEnd('?')
        if (!sanitized.contains("^2") && !sanitized.contains("x^2") && !sanitized.contains("x*x")) return null
        if (!sanitized.contains("=")) return null

        // Case: x^2 = C or x^2 - C = 0
        val simpleMatch = Regex("""^([a-zA-Z])\^2\s*=\s*(\d+(?:\.\d+)?)$""").find(sanitized.replace(" ", ""))
        if (simpleMatch != null) {
            val (v, cStr) = simpleMatch.destructured
            val c = cStr.toDoubleOrNull() ?: return null
            val root = sqrt(c)
            val rootFormatted = formatNumber(root)
            return """
### 📐 Algebra: Pure Quadratic Equation

**Given Equation:**
$v² = $cStr

**Step 1: Take Square Root of Both Sides**
$v = ±√($cStr)

**Step 2: Calculate Roots**
$v = $rootFormatted or $v = -$rootFormatted

**Final Answer:**
$v = ±$rootFormatted

💡 **Study Tip:** Taking the square root of both sides always produces both positive and negative roots (±).
            """.trimIndent()
        }

        val zeroMatch = Regex("""^([a-zA-Z])\^2\s*-\s*(\d+(?:\.\d+)?)\s*=\s*0$""").find(sanitized.replace(" ", ""))
        if (zeroMatch != null) {
            val (v, cStr) = zeroMatch.destructured
            val c = cStr.toDoubleOrNull() ?: return null
            val root = sqrt(c)
            val rootFormatted = formatNumber(root)
            return """
### 📐 Algebra: Difference of Squares

**Given Equation:**
$v² - $cStr = 0

**Step 1: Factor or Add $cStr to Both Sides**
$v² = $cStr

**Step 2: Take the Square Root**
$v = ±√($cStr)
$v = ±$rootFormatted

**Final Answer:**
$v = $rootFormatted or $v = -$rootFormatted

💡 **Study Tip:** A difference of squares can also be factored as ($v - $rootFormatted)($v + $rootFormatted) = 0.
            """.trimIndent()
        }

        return null
    }

    private fun trySolvePercentage(input: String): String? {
        val match = Regex(
            """(?:what\s+is\s+)?(\d+(?:\.\d+)?)\s*%\s*(?:of)\s*(\d+(?:\.\d+)?)\??$""",
            RegexOption.IGNORE_CASE
        ).find(input.trim()) ?: return null

        val (pctStr, numStr) = match.destructured
        val pct = pctStr.toDoubleOrNull() ?: return null
        val num = numStr.toDoubleOrNull() ?: return null
        val ans = (pct / 100.0) * num
        val ansFormatted = formatNumber(ans)

        return """
### 🔢 Mathematics: Percentage Calculation

**Problem:**
Calculate $pctStr% of $numStr

**Step 1: Convert Percentage to Decimal**
$pctStr% = $pctStr / 100 = ${formatNumber(pct / 100.0)}

**Step 2: Multiply by the Base Value**
${formatNumber(pct / 100.0)} × $numStr = $ansFormatted

**Final Answer:**
$pctStr% of $numStr = $ansFormatted

💡 **Study Tip:** To find 10% of any number quickly, move the decimal point one place to the left; then multiply to find any other percentage!
        """.trimIndent()
    }

    private fun trySolveArithmetic(input: String): String? {
        val clean = input.trim().replace(" ", "").replace("=", "").replace("?", "")
        val match = Regex("""^(-?\d+(?:\.\d+)?)\s*([\+\-\*\/xX×÷])\s*(-?\d+(?:\.\d+)?)$""").find(clean) ?: return null
        val (num1Str, op, num2Str) = match.destructured
        val num1 = num1Str.toDoubleOrNull() ?: return null
        val num2 = num2Str.toDoubleOrNull() ?: return null
        val result = when (op) {
            "+", "＋" -> num1 + num2
            "-", "－" -> num1 - num2
            "*", "×", "x", "X" -> num1 * num2
            "/", "÷" -> if (num2 != 0.0) num1 / num2 else return "Cannot divide by zero."
            else -> return null
        }
        val formattedResult = formatNumber(result)
        return """
### 🔢 Arithmetic Calculation

**Problem:**
Calculate $num1Str $op $num2Str

**Step-by-Step:**
$num1Str $op $num2Str = $formattedResult

**Final Answer:**
$formattedResult
        """.trimIndent()
    }

    private fun findVariable(s: String): Char? {
        val candidates = s.filter { it in 'a'..'z' || it in 'A'..'Z' }
        if (candidates.isEmpty()) return null
        // Prefer 'x' if present
        if (candidates.contains('x', ignoreCase = true)) return 'x'
        if (candidates.contains('y', ignoreCase = true)) return 'y'
        return candidates.first()
    }

    /**
     * Parses an expression like "2x + 5" or "x - 19" into Pair(varCoeff, constVal).
     */
    private fun parseLinearSide(side: String, v: Char): Pair<Double, Double>? {
        var coeff = 0.0
        var constant = 0.0

        val normalized = side.replace(" ", "")
            .replace("-", "+-")
            .split("+")
            .filter { it.isNotBlank() }

        if (normalized.isEmpty()) return Pair(0.0, 0.0)

        for (token in normalized) {
            if (token.contains(v, ignoreCase = true)) {
                val numPart = token.filterNot { it.equals(v, ignoreCase = true) || it == '*' }
                val c = when {
                    numPart.isEmpty() || numPart == "+" -> 1.0
                    numPart == "-" -> -1.0
                    else -> numPart.toDoubleOrNull() ?: return null
                }
                coeff += c
            } else {
                val num = token.toDoubleOrNull() ?: return null
                constant += num
            }
        }
        return Pair(coeff, constant)
    }

    private fun formatNumber(d: Double): String {
        return if (abs(d - d.toLong()) < 1e-9) {
            d.toLong().toString()
        } else {
            "%.4f".format(d).trimEnd('0').trimEnd('.')
        }
    }
}
