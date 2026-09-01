package com.aichat.imessage.tools

import android.content.Context
import android.content.Intent

data class AppInfo(val name: String, val packageName: String)

object AppLauncherHelper {

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos.mapNotNull { resolve ->
            val appName = resolve.loadLabel(pm)?.toString()
            val pkgName = resolve.activityInfo?.packageName
            if (!appName.isNullOrBlank() && !pkgName.isNullOrBlank()) {
                AppInfo(appName, pkgName)
            } else null
        }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }
    }

    fun openAppByNameOrPackage(context: Context, query: String): Boolean {
        val pm = context.packageManager
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return false

        // 1. Intentar lanzar directo por nombre de paquete
        val launchIntent = pm.getLaunchIntentForPackage(trimmed)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return true
        }

        // 2. Buscar coincidencias en apps instaladas por nombre
        val installed = getInstalledApps(context)
        val match = installed.find { it.name.equals(trimmed, ignoreCase = true) }
            ?: installed.find { it.name.contains(trimmed, ignoreCase = true) }
            ?: installed.find { it.packageName.contains(trimmed, ignoreCase = true) }

        if (match != null) {
            val intent = pm.getLaunchIntentForPackage(match.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
        }

        return false
    }
}
