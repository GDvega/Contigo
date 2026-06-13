package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.testing.MedicationTestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DailyStatusCalculatorTest {
    private val today = LocalDate.parse("2026-06-11")

    @Test
    fun `all taken yields low risk and zero pending`() {
        val med = MedicationTestFixtures.medication(id = "m1")
        val logs = listOf(MedicationTestFixtures.log("l1", "m1", "TAKEN"))

        val snapshot = DailyStatusCalculator.calculate(
            medications = listOf(med),
            todayMedicationLogs = logs,
            latestPressureToday = null,
            today = today,
        )

        assertEquals(1, snapshot.takenMedicationCount)
        assertEquals(0, snapshot.pendingMedicationCount)
        assertEquals(DailyRiskLevel.LOW, snapshot.riskLevel)
        assertEquals("Todo en orden", snapshot.statusTitle)
    }

    @Test
    fun `pending medications raise risk to medium without pressure alert`() {
        val med = MedicationTestFixtures.medication(id = "m1")

        val snapshot = DailyStatusCalculator.calculate(
            medications = listOf(med),
            todayMedicationLogs = emptyList(),
            latestPressureToday = null,
            today = today,
        )

        assertEquals(1, snapshot.pendingMedicationCount)
        assertEquals(DailyRiskLevel.MEDIUM, snapshot.riskLevel)
        assertEquals("Hay pendientes", snapshot.statusTitle)
    }

    @Test
    fun `critical pressure overrides medication pending to high risk`() {
        val med = MedicationTestFixtures.medication(id = "m1")
        val pressure = MedicationTestFixtures.pressureReading(status = PressureStatus.CRITICAL)

        val snapshot = DailyStatusCalculator.calculate(
            medications = listOf(med),
            todayMedicationLogs = emptyList(),
            latestPressureToday = pressure,
            today = today,
        )

        assertEquals(DailyRiskLevel.HIGH, snapshot.riskLevel)
        assertEquals("Revisar con familiar o médico", snapshot.statusTitle)
    }

    @Test
    fun `expired medications are excluded from daily counts`() {
        val expired = MedicationTestFixtures.medication(
            id = "expired",
            scheduleType = ScheduleType.DATE_RANGE,
            startDate = "2026-01-01",
            endDate = "2026-06-01",
        )

        val snapshot = DailyStatusCalculator.calculate(
            medications = listOf(expired),
            todayMedicationLogs = emptyList(),
            latestPressureToday = null,
            today = today,
        )

        assertEquals(0, snapshot.activeMedicationCount)
        assertEquals(0, snapshot.pendingMedicationCount)
        assertEquals(DailyRiskLevel.LOW, snapshot.riskLevel)
    }

    @Test
    fun `skipped medication is not counted as pending`() {
        val med = MedicationTestFixtures.medication(id = "m1")
        val logs = listOf(MedicationTestFixtures.log("l1", "m1", "SKIPPED"))

        val snapshot = DailyStatusCalculator.calculate(
            medications = listOf(med),
            todayMedicationLogs = logs,
            latestPressureToday = null,
            today = today,
        )

        assertEquals(0, snapshot.pendingMedicationCount)
        assertEquals(0, snapshot.takenMedicationCount)
    }

    @Test
    fun `getPendingMedicationsForTime respects logs at schedule slot`() {
        val meds = listOf(
            MedicationTestFixtures.medication(id = "a", scheduleTime = "08:00"),
            MedicationTestFixtures.medication(id = "b", scheduleTime = "08:00"),
        )
        val logs = listOf(MedicationTestFixtures.log("l1", "a", "TAKEN"))

        val pending = DailyStatusCalculator.getPendingMedicationsForTime(
            scheduleTime = "08:00",
            medications = meds,
            todayMedicationLogs = logs,
            today = today,
        )

        assertEquals(listOf("b"), pending.map { it.id })
    }

    @Test
    fun `next medication group is populated when pending remain`() {
        val med = MedicationTestFixtures.medication(id = "m1", scheduleTime = "20:00")

        val snapshot = DailyStatusCalculator.calculate(
            medications = listOf(med),
            todayMedicationLogs = emptyList(),
            latestPressureToday = null,
            today = today,
        )

        assertEquals("20:00", snapshot.nextMedicationGroup?.scheduleTime)
    }

    @Test
    fun `out of range pressure yields medium risk when meds are taken`() {
        val med = MedicationTestFixtures.medication(id = "m1")
        val logs = listOf(MedicationTestFixtures.log("l1", "m1", "TAKEN"))
        val pressure = MedicationTestFixtures.pressureReading(status = PressureStatus.OUT_OF_RANGE)

        val snapshot = DailyStatusCalculator.calculate(
            medications = listOf(med),
            todayMedicationLogs = logs,
            latestPressureToday = pressure,
            today = today,
        )

        assertEquals(DailyRiskLevel.MEDIUM, snapshot.riskLevel)
        assertNull(snapshot.nextMedicationGroup)
    }
}
