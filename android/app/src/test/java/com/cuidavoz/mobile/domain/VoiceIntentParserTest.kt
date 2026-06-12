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
    fun `parse extracts shorthanded pressure values`() {
        val result = VoiceIntentParser.parse("mi presion es 12 8")

        assertEquals(VoiceIntent.PressureValues(120, 80, null), result)
    }

    @Test
    fun `parse handles self correction for pressure`() {
        val result = VoiceIntentParser.parse("mi presion es 190 no me equivoque es 120 sobre 80")

        assertEquals(VoiceIntent.PressureValues(120, 80, null), result)
    }

    @Test
    fun `parse recognizes pressure registration intent`() {
        val result = VoiceIntentParser.parse("quiero registrar mi presion")

        assertEquals(VoiceIntent.RegisterPressure, result)
    }

    @Test
    fun `parse confirms one medication`() {
        val result = VoiceIntentParser.parse("ya tome mi pastilla")

        assertEquals(VoiceIntent.ConfirmMedicationTaken, result)
    }

    @Test
    fun `parse confirms all medications`() {
        val result = VoiceIntentParser.parse("ya tome todas")

        assertEquals(VoiceIntent.ConfirmAllMedicationsTaken, result)
    }

    @Test
    fun `parse handles medication correction`() {
        val result = VoiceIntentParser.parse("ya tome mi pastilla no cancelar")

        assertEquals(VoiceIntent.Cancel, result)
    }

    @Test
    fun `parse handles help requests`() {
        val result = VoiceIntentParser.parse("necesito ayuda")

        assertEquals(VoiceIntent.AskForHelp, result)
    }

    @Test
    fun `parse handles repeat instruction requests`() {
        val result = VoiceIntentParser.parse("repite por favor")

        assertEquals(VoiceIntent.RepeatReminder, result)
    }

    @Test
    fun `parse handles non pill medications`() {
        assertEquals(VoiceIntent.ConfirmMedicationTaken, VoiceIntentParser.parse("ya me puse las gotas"))
        assertEquals(VoiceIntent.ConfirmMedicationTaken, VoiceIntentParser.parse("tome el jarabe"))
        assertEquals(VoiceIntent.ConfirmMedicationTaken, VoiceIntentParser.parse("ya me eche la pomada"))
    }

    @Test
    fun `positive confirmation handles supported variations`() {
        assertTrue(VoiceIntentParser.isPositiveConfirmation("si guardalo"))
        assertTrue(VoiceIntentParser.isPositiveConfirmation("anotalo"))
        assertTrue(VoiceIntentParser.isPositiveConfirmation("hazlo nomas"))
    }

    @Test
    fun `looks like medication confirmation detects fuzzy intents`() {
        assertTrue(VoiceIntentParser.looksLikeMedicationConfirmation("ya... tome... algo"))
        assertTrue(VoiceIntentParser.looksLikeMedicationConfirmation("listo ya esta"))
        assertFalse(VoiceIntentParser.looksLikeMedicationConfirmation("hola como estas"))
    }
}
