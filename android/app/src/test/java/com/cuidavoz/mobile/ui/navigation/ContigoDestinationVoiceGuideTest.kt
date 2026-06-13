package com.cuidavoz.mobile.ui.navigation

import com.cuidavoz.mobile.ui.navigation.ContigoDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContigoDestinationVoiceGuideTest {
    @Test
    fun patientHomeUsesProvidedAudio() {
        val text = ContigoDestination.PatientHome.voiceGuideText(
            patientHomeAudio = "Bienvenido a Contigo",
            contactName = null,
            hasContact = false,
        )

        assertEquals("Bienvenido a Contigo", text)
    }

    @Test
    fun helpWithoutContactExplainsMissingContact() {
        val text = ContigoDestination.Help.voiceGuideText(
            patientHomeAudio = "",
            contactName = null,
            hasContact = false,
        )

        assertEquals(
            "Todavía no hay un contacto de ayuda. Puedes abrir la zona del familiar o cuidador.",
            text,
        )
    }

    @Test
    fun helpWithContactIncludesName() {
        val text = ContigoDestination.Help.voiceGuideText(
            patientHomeAudio = "",
            contactName = "María",
            hasContact = true,
        )

        assertEquals("Aquí puedes llamar o enviar un mensaje a María.", text)
    }

    @Test
    fun onboardingHasNoVoiceGuide() {
        assertNull(
            ContigoDestination.Onboarding.voiceGuideText("", null, false),
        )
    }

    @Test
    fun backupScreenHasBackupGuide() {
        assertEquals(
            "Aquí puedes guardar o recuperar una copia de seguridad.",
            ContigoDestination.Backup.voiceGuideText("", null, false),
        )
    }
}
