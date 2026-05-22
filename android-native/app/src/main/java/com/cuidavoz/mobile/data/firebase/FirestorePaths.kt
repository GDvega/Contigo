package com.cuidavoz.mobile.data.firebase

object FirestorePaths {
    fun familyDocument(familyId: String) = "families/$familyId"
    fun familyMembersCollection(familyId: String) = "families/$familyId/members"
    fun patientsCollection(familyId: String) = "families/$familyId/patients"
    fun patientDocument(familyId: String, patientId: String) = "families/$familyId/patients/$patientId"
    fun medicationsCollection(familyId: String, patientId: String) =
        "families/$familyId/patients/$patientId/medications"
    fun pressureCollection(familyId: String, patientId: String) =
        "families/$familyId/patients/$patientId/pressureReadings"
    fun medicationLogsCollection(familyId: String, patientId: String) =
        "families/$familyId/patients/$patientId/medicationLogs"
    fun healthSettingsDocument(familyId: String, patientId: String) =
        "families/$familyId/patients/$patientId/healthSettings/main"
    fun alertsCollection(familyId: String, patientId: String) =
        "families/$familyId/patients/$patientId/alerts"
}
