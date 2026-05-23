package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.domain.voice.VoiceIntent
import com.cuidavoz.mobile.domain.voice.VoiceIntentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceIntentParserTest {
    @Test
    fun `parse extracts pressure values with pulse`() {
        val result = VoiceIntentParser.parse("Mi presion es 120 sobre 80 pulso 70")

        assertEquals(VoiceIntent.PressureValues(120, 80, 70), result)
    }

    @Test
    fun `parse extracts spoken pressure values`() {
        val result = VoiceIntentParser.parse("Mi presion es ciento veinte sobre ochenta pulso setenta")

        assertEquals(VoiceIntent.PressureValues(120, 80, 70), result)
    }

    @Test
    fun `parse maps family help phrases`() {
        val result = VoiceIntentParser.parse("Llama a mi familiar")

        assertEquals(VoiceIntent.AskForHelp, result)
    }

    @Test
    fun `positive confirmation accepts guardar`() {
        assertTrue(VoiceIntentParser.isPositiveConfirmation("Si, guardar"))
    }

    @Test
    fun `negative confirmation does not treat no entendi as cancel`() {
        assertFalse(VoiceIntentParser.isNegativeConfirmation("No entendi"))
    }
}
