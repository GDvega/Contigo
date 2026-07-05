package com.cuidavoz.mobile.testing

import android.content.Context
import java.io.File

object InstrumentationTestHelpers {
    fun clearLocalAppData(context: Context) {
        val datastoreDir = File(context.filesDir.parent, "datastore")
        if (datastoreDir.exists()) {
            datastoreDir.deleteRecursively()
        }
    }
}
