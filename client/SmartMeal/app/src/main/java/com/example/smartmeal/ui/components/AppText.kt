package com.example.smartmeal.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.example.smartmeal.ui.theme.MallannaFontFamily
import com.example.smartmeal.ui.theme.MontserratFontFamily

/**
 * SmartMealText — умный компонент, который автоматически применяет:
 * - Montserrat для букв
 * - Mallanna для цифр
 */
@Composable
fun SmartMealText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current
) {
    val annotatedString = buildSmartMealString(text, fontWeight)
    
    Text(
        text = annotatedString,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = MontserratFontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = style
    )
}

/**
 * Функция-помощник для автоматического применения Mallanna к цифрам
 */
fun buildSmartMealString(text: String, fontWeight: FontWeight? = null): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        val digitRegex = "\\d+".toRegex()
        
        digitRegex.findAll(text).forEach { matchResult ->
            if (matchResult.range.first > lastIndex) {
                append(text.substring(lastIndex, matchResult.range.first))
            }
            
            val start = length
            append(matchResult.value)
            addStyle(
                style = SpanStyle(
                    fontFamily = MallannaFontFamily,
                    fontWeight = fontWeight ?: FontWeight.Normal
                ),
                start = start,
                end = length
            )
            
            lastIndex = matchResult.range.last + 1
        }
        
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}
