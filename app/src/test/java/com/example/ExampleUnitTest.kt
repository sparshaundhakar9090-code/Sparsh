package com.example

import com.example.data.network.GeminiApiClient
import com.example.util.MathSolverEngine
import com.example.util.MathTextCleaner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testMathTextCleaner_erasesDollarSignsAndCleansLatex() {
        val d = "$"
        val rawInput = """
            ### Mathematics: Quadratic Equation
            Solve the equation:
            ${d}${d}ax^2 + bx + c = 0${d}${d}
            Calculate the discriminant:
            ${d}${d}\Delta = b^2 - 4ac${d}${d}
            Using the formula:
            ${d}x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}${d}
            The roots are ${d}x_1${d} and ${d}x_2${d}.
        """.trimIndent()

        val cleaned = MathTextCleaner.cleanMathFormatting(rawInput)

        assertFalse("Output must not contain any dollar signs", cleaned.contains("$"))
        assertFalse("Output must not contain raw LaTeX commands", cleaned.contains("\\frac"))
        assertFalse("Output must not contain raw LaTeX commands", cleaned.contains("\\sqrt"))
        assertFalse("Output must not contain raw LaTeX commands", cleaned.contains("\\Delta"))

        assertTrue(cleaned.contains("Δ = b² - 4ac"))
        assertTrue(cleaned.contains("ax² + bx + c = 0"))
        assertTrue(cleaned.contains("x₁") && cleaned.contains("x₂"))
        assertTrue(cleaned.contains("±"))
    }

    @Test
    fun testMathSolverEngineDirectly() {
        // Linear equation: x+19=200
        val res1 = MathSolverEngine.trySolve("x+19=200")
        assertNotNull(res1)
        assertTrue(res1!!.contains("x = 181"))
        assertTrue(res1.contains("Final Answer"))

        // Linear equation: 2x+5=25
        val res2 = MathSolverEngine.trySolve("2x + 5 = 25")
        assertNotNull(res2)
        assertTrue(res2!!.contains("x = 10"))

        // Linear equation: solve 3x - 7 = 20
        val res3 = MathSolverEngine.trySolve("solve 3x - 7 = 20")
        assertNotNull(res3)
        assertTrue(res3!!.contains("x = 9"))

        // Division: x / 4 = 15
        val res4 = MathSolverEngine.trySolve("x / 4 = 15")
        assertNotNull(res4)
        assertTrue(res4!!.contains("x = 60"))

        // Arithmetic: 2+2
        val res5 = MathSolverEngine.trySolve("2+2")
        assertNotNull(res5)
        assertTrue(res5!!.contains("4"))
    }

    @Test
    fun testClientGreetings() = runBlocking {
        val client = GeminiApiClient()
        val greetingRes = client.solveHomework("hello").getOrThrow()
        assertTrue(greetingRes.contains("Sparsh Aundhakar Intelligence"))
        assertFalse(greetingRes.contains("Problem Overview"))

        val howAreYouRes = client.solveHomework("how are you").getOrThrow()
        assertTrue(howAreYouRes.contains("doing great") || howAreYouRes.contains("Sparsh"))
    }

    @Test
    fun testClientEquationSolving() = runBlocking {
        val client = GeminiApiClient()
        val eq1Res = client.solveHomework("x+19=200").getOrThrow()
        assertTrue(eq1Res.contains("Final Answer"))
        assertTrue(eq1Res.contains("181"))
        assertTrue(eq1Res.contains("x = 181"))
    }
}
