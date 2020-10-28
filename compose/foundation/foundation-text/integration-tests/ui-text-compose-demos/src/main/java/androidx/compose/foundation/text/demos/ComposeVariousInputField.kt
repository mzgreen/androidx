/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.text.demos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase

/**
 * The offset translator used for credit card input field.
 *
 * @see creditCardFilter
 */
private val creditCardOffsetTranslator = object : OffsetMap {
    override fun originalToTransformed(offset: Int): Int {
        if (offset <= 3) return offset
        if (offset <= 7) return offset + 1
        if (offset <= 11) return offset + 2
        if (offset <= 16) return offset + 3
        return 19
    }

    override fun transformedToOriginal(offset: Int): Int {
        if (offset <= 4) return offset
        if (offset <= 9) return offset - 1
        if (offset <= 14) return offset - 2
        if (offset <= 19) return offset - 3
        return 16
    }
}

/**
 * The visual filter for credit card input field.
 *
 * This filter converts up to 16 digits to hyphen connected 4 digits string.
 * For example, "1234567890123456" will be shown as "1234-5678-9012-3456".
 */
private val creditCardFilter = object : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 16) text.text.substring(0..15) else text.text
        var out = ""
        for (i in 0 until trimmed.length) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 15) out += "-"
        }
        return TransformedText(AnnotatedString(out), creditCardOffsetTranslator)
    }
}

/**
 * The offset translator which works for all offset keep remains the same.
 */
private val identityTranslater = object : OffsetMap {
    override fun originalToTransformed(offset: Int): Int = offset
    override fun transformedToOriginal(offset: Int): Int = offset
}

/**
 * The visual filter for capitalization.
 *
 * This filer converts ASCII characters to capital form.
 */
private class CapitalizeTransformation(
    val locale: LocaleList = LocaleList("en-US")
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Note: identityTranslater doesn't work for some locale, e.g. Turkish
        return TransformedText(AnnotatedString(text.text).toUpperCase(locale), identityTranslater)
    }
}

/**
 * The offset translator for phone number
 *
 * @see phoneNumberFilter
 */
private val phoneNumberOffsetTranslater = object : OffsetMap {
    override fun originalToTransformed(offset: Int): Int {
        return when (offset) {
            0 -> 1
            1 -> 2
            2 -> 3
            3 -> 6
            4 -> 7
            5 -> 8
            6 -> 10
            7 -> 11
            8 -> 12
            9 -> 13
            else -> 14
        }
    }

    override fun transformedToOriginal(offset: Int): Int {
        return when (offset) {
            0 -> 0
            1 -> 0
            2 -> 1
            3 -> 2
            4 -> 3
            5 -> 3
            6 -> 3
            7 -> 4
            8 -> 5
            9 -> 6
            10 -> 6
            11 -> 7
            12 -> 8
            13 -> 9
            else -> 10
        }
    }
}

/**
 * The visual filter for phone number.
 *
 * This filter converts up to 10 digits to phone number form.
 * For example, "1234567890" will be shown as "(123) 456-7890".
 */
private val phoneNumberFilter = object : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 10) text.text.substring(0..9) else text.text
        val filled = trimmed + "_".repeat(10 - trimmed.length)
        val res = "(" + filled.substring(0..2) + ") " + filled.substring(3..5) + "-" +
            filled.substring(6..9)
        return TransformedText(AnnotatedString(text = res), phoneNumberOffsetTranslater)
    }
}

private val emailFilter = object : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return if (text.text.indexOf("@") == -1) {
            TransformedText(AnnotatedString(text = text.text + "@gmail.com"), identityTranslater)
        } else {
            TransformedText(text, identityTranslater)
        }
    }
}

@Composable
fun VariousInputFieldDemo() {
    ScrollableColumn {
        TagLine(tag = "Capitalization")
        VariousEditLine(
            keyboardType = KeyboardType.Ascii,
            onValueChange = { old, new ->
                if (new.any { !it.isLetterOrDigit() }) old else new
            },
            visualTransformation = CapitalizeTransformation()
        )

        TagLine(tag = "Capitalization (Turkish)")
        VariousEditLine(
            keyboardType = KeyboardType.Ascii,
            onValueChange = { old, new ->
                if (new.any { !it.isLetterOrDigit() }) old else new
            },
            visualTransformation = CapitalizeTransformation(LocaleList("tr"))
        )

        TagLine(tag = "Password")
        VariousEditLine(
            keyboardType = KeyboardType.Password,
            onValueChange = { old, new ->
                if (new.any { !it.isLetterOrDigit() }) old else new
            },
            visualTransformation = PasswordVisualTransformation()
        )

        TagLine(tag = "Phone Number")
        VariousEditLine(
            keyboardType = KeyboardType.Number,
            onValueChange = { old, new ->
                if (new.length > 10 || new.any { !it.isDigit() }) old else new
            },
            visualTransformation = phoneNumberFilter
        )

        TagLine(tag = "Credit Card")
        VariousEditLine(
            keyboardType = KeyboardType.Number,
            onValueChange = { old, new ->
                if (new.length > 16 || new.any { !it.isDigit() }) old else new
            },
            visualTransformation = creditCardFilter
        )

        TagLine(tag = "Email Suggestion")
        VariousEditLine(
            keyboardType = KeyboardType.Email,
            visualTransformation = emailFilter
        )

        TagLine(tag = "Editfield with Hint Text")
        HintEditText {
            Text(
                text = "Hint Text",
                color = Color(0xFF888888),
                style = TextStyle(fontSize = fontSize8)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun VariousEditLine(
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onValueChange: (String, String) -> String = { _, new -> new },
    visualTransformation: VisualTransformation
) {
    val state = savedInstanceState(saver = TextFieldValue.Saver) { TextFieldValue() }
    BasicTextField(
        modifier = demoTextFieldModifiers,
        value = state.value,
        keyboardType = keyboardType,
        imeAction = imeAction,
        visualTransformation = visualTransformation,
        onValueChange = {
            val value = onValueChange(state.value.text, it.text)
            val selection = it.selection.constrain(0, value.length)
            val composition = it.composition?.constrain(0, value.length)
            state.value = TextFieldValue(value, selection, composition)
        },
        textStyle = TextStyle(fontSize = fontSize8)
    )
}

private fun TextRange.constrain(minimumValue: Int, maximumValue: Int): TextRange {
    val newStart = start.coerceIn(minimumValue, maximumValue)
    val newEnd = end.coerceIn(minimumValue, maximumValue)
    if (newStart != start || newEnd != end) {
        return TextRange(newStart, newEnd)
    }
    return this
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun HintEditText(hintText: @Composable () -> Unit) {
    val state = savedInstanceState(saver = TextFieldValue.Saver) { TextFieldValue() }

    Box(demoTextFieldModifiers) {
        BasicTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.value,
            onValueChange = { state.value = it },
            textStyle = TextStyle(fontSize = fontSize8)
        )
        if (state.value.text.isEmpty()) {
            hintText()
        }
    }
}