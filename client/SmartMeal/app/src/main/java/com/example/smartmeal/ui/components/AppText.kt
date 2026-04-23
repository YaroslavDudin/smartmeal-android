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
    // Для обычной строки применяем магию шрифтов (Mallanna для цифр)
    val annotatedString = buildSmartMealString(text)
    
    Text(
        text = annotatedString,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = MontserratFontFamily, // Основной шрифт для букв
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

@Composable
fun SmartMealText(
    text: AnnotatedString,
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
    // Применяем Mallanna к цифрам даже в сложных строках
    val finalAnnotatedString = applySmartStylesToAnnotatedString(text)

    Text(
        text = finalAnnotatedString,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = MontserratFontFamily, // Основной шрифт для букв
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
 * Применяет Mallanna к цифрам в обычной строке.
 */
fun buildSmartMealString(text: String): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        val digitRegex = "\\d+".toRegex()
        digitRegex.findAll(text).forEach { matchResult ->
            addStyle(
                style = SpanStyle(
                    fontFamily = MallannaFontFamily
                ),
                start = matchResult.range.first,
                end = matchResult.range.last + 1
            )
        }
    }
}

/**
 * Проходит по AnnotatedString и применяет Mallanna к цифрам,
 * если для этого участка еще не задан fontFamily.
 */
fun applySmartStylesToAnnotatedString(text: AnnotatedString): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        val plainText = text.text
        val digitRegex = "\\d+".toRegex()
        
        digitRegex.findAll(plainText).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            
            // Если для цифр еще нет своего шрифта — ставим Mallanna
            val existingStyles = text.spanStyles.filter { 
                it.start < end && it.end > start && it.item.fontFamily != null
            }
            
            if (existingStyles.isEmpty()) {
                addStyle(
                    style = SpanStyle(
                        fontFamily = MallannaFontFamily
                    ),
                    start = start,
                    end = end
                )
            }
        }
    }
}
