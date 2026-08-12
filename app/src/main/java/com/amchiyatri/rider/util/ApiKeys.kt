package com.amchiyatri.rider.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Reads the Maps API key back out of the manifest `<meta-data>` entry at runtime, so both the
 * Maps SDK (which reads the manifest itself) and the Places SDK (which needs the key passed to
 * `Places.initialize(...)` in code) share the exact same value from `app/secrets.properties`
 * instead of duplicating it in two places.
 */
object ApiKeys {
    fun mapsApiKey(context: Context): String {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA,
        )
        return appInfo.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
    }

    /**
     * The SHA-1 of this build's signing certificate, formatted the way Google's "Android app"
     * API key restriction expects it (uppercase hex, colon-separated). An Android-restricted key
     * works for the Maps/Places *SDKs* automatically, but a plain REST call (Directions API) has
     * to prove it's really this app by sending this value plus the package name as headers -
     * see NetworkModule's interceptor.
     */
    fun androidCertSha1(context: Context): String {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        }

        val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.let { info ->
                if (info.hasMultipleSigners()) info.apkContentsSigners.firstOrNull() else info.signingCertificateHistory.firstOrNull()
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.firstOrNull()
        } ?: return ""

        val digest = MessageDigest.getInstance("SHA-1").digest(signature.toByteArray())
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
