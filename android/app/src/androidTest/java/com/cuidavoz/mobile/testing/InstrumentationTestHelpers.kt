package com.cuidavoz.mobile.testing

import android.content.Context
import java.io.File

object InstrumentationTestHelpers {
    fun clearLocalAppData(context: Context) {
        context.deleteDatabase("cuida_voz.db")
        context.filesDir.parentFile?.resolve("databases")?.listFiles()?.forEach { file ->
            if (file.name.startsWith("cuida_voz")) {
                file.delete()
            }
        }
        val datastoreDir = File(context.filesDir.parent, "datastore")
        if (datastoreDir.exists()) {
            datastoreDir.deleteRecursively()
        }
    }
}
