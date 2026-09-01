package com.cuidavoz.mobile.data.sync

data class CaregiverAlertMessage(val alertId: String) {
    companion object {
        fun from(data: Map<String, String>): CaregiverAlertMessage? {
            if (data["kind"] != "caregiver_alert") return null
            if (data["familyId"].isNullOrBlank() || data["patientId"].isNullOrBlank()) return null
            val alertId = data["alertId"]?.takeIf(String::isNotBlank) ?: return null
            return CaregiverAlertMessage(alertId)
        }
    }
}
