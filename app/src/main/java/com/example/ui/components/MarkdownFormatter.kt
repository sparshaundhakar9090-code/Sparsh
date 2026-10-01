package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SaiBlueContainer
import com.example.ui.theme.SaiBluePrimary
import com.example.util.MathTextCleaner

sealed class SolutionBlock {
    data class Code(val content: String) : SolutionBlock()
    data class Heading(val title: String) : SolutionBlock()
    data class NumberedStep(val number: String, val content: String) : SolutionBlock()
    data class Bullet(val content: String) : SolutionBlock()
    data class FinalAnswer(val content: String) : SolutionBlock()
    data class StudyTip(val content: String) : SolutionBlock()
    data class Paragraph(val text: String) : SolutionBlock()
}

@Composable
fun FormattedSolutionText(
    text: String,
    modifier: Modifier = Modifier
) {
    // Sanitize math and parse structured blocks
    val blocks = remember(text) {
        val cleanedText = MathTextCleaner.cleanMathFormatting(text)
        parseSolutionBlocks(cleanedText)
    }

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is SolutionBlock.Code -> {
                    CodeBlockCard(code = block.content)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                is SolutionBlock.Heading -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SaiBluePrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                is SolutionBlock.FinalAnswer -> {
                    FinalAnswerCard(answer = block.content)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                is SolutionBlock.StudyTip -> {
                    StudyTipCard(tip = block.content)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                is SolutionBlock.NumberedStep -> {
                    NumberedStepItem(number = block.number, content = block.content)
                }
                is SolutionBlock.Bullet -> {
                    BulletItem(content = block.content)
                }
                is SolutionBlock.Paragraph -> {
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun parseSolutionBlocks(rawText: String): List<SolutionBlock> {
    val lines = rawText.lines()
    val blocks = mutableListOf<SolutionBlock>()

    var inCodeBlock = false
    val codeBuffer = StringBuilder()

    var inFinalAnswer = false
    val finalAnswerBuffer = StringBuilder()

    fun flushFinalAnswer() {
        if (inFinalAnswer && finalAnswerBuffer.isNotBlank()) {
            blocks.add(SolutionBlock.FinalAnswer(finalAnswerBuffer.toString().trim()))
            finalAnswerBuffer.clear()
        }
        inFinalAnswer = false
    }

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            flushFinalAnswer()
            if (inCodeBlock) {
                blocks.add(SolutionBlock.Code(codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            continue
        }

        if (inCodeBlock) {
            codeBuffer.append(line).append("\n")
            continue
        }

        // Check for Final Answer header
        if (isFinalAnswerHeaderLine(trimmed)) {
            flushFinalAnswer()
            inFinalAnswer = true
            val contentAfterHeader = extractFinalAnswerInlineContent(trimmed)
            if (contentAfterHeader.isNotBlank()) {
                finalAnswerBuffer.append(contentAfterHeader).append("\n")
            }
            continue
        }

        if (inFinalAnswer) {
            // Check if another distinct block starts that ends the final answer
            val startsOtherSection = trimmed.startsWith("###") ||
                    trimmed.startsWith("##") ||
                    trimmed.contains("💡") ||
                    trimmed.contains("Study Tip", ignoreCase = true) ||
                    trimmed.contains("Key Concept", ignoreCase = true)

            if (startsOtherSection) {
                flushFinalAnswer()
                // Process this new line below
            } else if (trimmed.isBlank() && finalAnswerBuffer.isNotBlank()) {
                finalAnswerBuffer.append("\n")
                continue
            } else {
                finalAnswerBuffer.append(line).append("\n")
                continue
            }
        }

        // Normal processing
        when {
            trimmed.startsWith("###") || trimmed.startsWith("##") -> {
                val heading = trimmed.removePrefix("###").removePrefix("##").trim()
                blocks.add(SolutionBlock.Heading(heading))
            }
            trimmed.contains("💡") || trimmed.contains("Study Tip", ignoreCase = true) || trimmed.contains("Key Concept", ignoreCase = true) -> {
                blocks.add(SolutionBlock.StudyTip(trimmed))
            }
            trimmed.matches(Regex("""^\d+\.\s.*""")) -> {
                val number = trimmed.substringBefore(".").trim()
                val stepContent = trimmed.substringAfter(".").trim()
                blocks.add(SolutionBlock.NumberedStep(number, stepContent))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val bulletContent = trimmed.drop(2).trim()
                blocks.add(SolutionBlock.Bullet(bulletContent))
            }
            trimmed.isNotBlank() -> {
                blocks.add(SolutionBlock.Paragraph(line))
            }
        }
    }

    if (inCodeBlock && codeBuffer.isNotEmpty()) {
        blocks.add(SolutionBlock.Code(codeBuffer.toString().trimEnd()))
    }
    flushFinalAnswer()

    return blocks
}

private fun isFinalAnswerHeaderLine(trimmed: String): Boolean {
    val clean = trimmed.lowercase().removePrefix("###").removePrefix("##").removePrefix("#").trim()
    return clean.startsWith("**final answer") ||
            clean.startsWith("final answer") ||
            clean.startsWith("**conclusion / final answer") ||
            clean.startsWith("**answer:**") ||
            clean.startsWith("answer:") ||
            clean.startsWith("**result:**") ||
            clean.startsWith("result:")
}

private fun extractFinalAnswerInlineContent(trimmed: String): String {
    val colonIndex = trimmed.indexOf(':')
    if (colonIndex != -1 && colonIndex < trimmed.length - 1) {
        val after = trimmed.substring(colonIndex + 1).removeSurrounding("*").trim()
        if (after.isNotBlank()) return after
    }
    return ""
}

@Composable
private fun FinalAnswerCard(
    answer: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("highlighted_final_answer_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9) // High-visibility light emerald highlight
        ),
        border = BorderStroke(2.dp, Color(0xFF2E7D32)), // Solid prominent green highlight border
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row: High-visibility Badge & Quick Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1B5E20),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F), // Golden star accent
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FINAL ANSWER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Final Answer", answer)
                        clipboard.setPrimaryClip(clip)
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("copy_final_answer_button"),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32))
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy final answer",
                        tint = Color(0xFF1B5E20),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copy Answer",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Highlighted Content Box with white contrast card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = parseInlineMarkdown(answer),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D5325),
                        lineHeight = 24.sp,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun NumberedStepItem(number: String, content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SaiBlueContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = SaiBluePrimary
                )
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = parseInlineMarkdown(content),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BulletItem(content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(SaiBluePrimary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = parseInlineMarkdown(content),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StudyTipCard(tip: String) {
    val cleanTip = tip.removePrefix("💡").trim()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF7E0))
            .border(1.dp, Color(0xFFFBBC04).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(0xFFB06000),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = parseInlineMarkdown(cleanTip),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF663C00),
                    lineHeight = 18.sp
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CodeBlockCard(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .padding(12.dp)
    ) {
        Text(
            text = code.trimEnd(),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFFE2E8F0),
            lineHeight = 18.sp
        )
    }
}

fun parseInlineMarkdown(text: String) = buildAnnotatedString {
    var currentIndex = 0
    val boldPattern = Regex("""\*\*(.*?)\*\*""")
    val matches = boldPattern.findAll(text).toList()

    for (match in matches) {
        if (match.range.first > currentIndex) {
            append(text.substring(currentIndex, match.range.first))
        }

        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }

        currentIndex = match.range.last + 1
    }

    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}
