package com.example.shizuku

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuCommandRunner {

    private const val TAG = "ShizukuCommandRunner"

    /**
     * Checks whether Shizuku service is running and binder is available.
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Checks whether Shizuku permission has been granted.
     */
    fun hasPermission(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Executes a shell command via Shizuku process in background silently.
     * Guaranteed no UI logging, and safely wrapped in try-catch.
     */
    suspend fun executeCommandSilent(command: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!hasPermission()) {
                Log.e(TAG, "Shizuku permission not granted")
                return@withContext false
            }

            // Execute command via Shizuku API
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            
            // Consume streams silently in background
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))
            
            while (reader.readLine() != null) {
                // Read and discard output silently
            }
            while (errReader.readLine() != null) {
                // Read and discard error output silently
            }

            val exitCode = process.waitFor()
            process.destroy()
            return@withContext exitCode == 0
        } catch (e: Throwable) {
            Log.e(TAG, "Error executing command silently: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Executes multiple shell commands sequentially.
     */
    suspend fun executeCommandsSilent(commands: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            var allSuccess = true
            for (cmd in commands) {
                val result = executeCommandSilent(cmd)
                if (!result) allSuccess = false
            }
            allSuccess
        } catch (e: Throwable) {
            Log.e(TAG, "Error executing multiple commands: ${e.message}", e)
            false
        }
    }
}
