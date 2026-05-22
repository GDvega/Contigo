package com.cuidavoz.mobile.domain.sync

enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
}

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE,
}

enum class SyncEntityType {
    PATIENT,
    MEDICATION,
    PRESSURE_READING,
    MEDICATION_LOG,
    HEALTH_SETTINGS,
    FAMILY_CONTACT,
    ALERT,
    LINK_CODE,
}
