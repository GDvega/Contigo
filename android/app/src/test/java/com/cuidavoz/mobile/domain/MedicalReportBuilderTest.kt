package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.domain.report.MedicalReportBuilder
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicalReportBuilderTest {
    @Test
    fun `pressure status label uses safe wording for critical values`() {
        val label = MedicalReportBuilder.pressureStatusLabel(PressureStatus.CRITICAL.name)

        assertEquals("Valor muy alto, buscar orientación médica", label)
    }
}
