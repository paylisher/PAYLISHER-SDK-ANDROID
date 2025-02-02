package com.paylisher.android.internal

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.paylisher.PaylisherInternal
import com.paylisher.android.PaylisherAndroidConfig

@Suppress("DEPRECATION")
internal fun getPackageInfo(
    context: Context,
    config: PaylisherAndroidConfig,
): PackageInfo? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context
                .packageManager
                .getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0.toLong()),
                )
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    } catch (e: Throwable) {
        config.logger.log("Error getting package info: $e.")
        null
    }
}

@Suppress("DEPRECATION")
internal fun PackageInfo.versionCodeCompat(): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        versionCode.toLong()
    }
}

internal fun Context.displayMetrics(): DisplayMetrics {
    return resources.displayMetrics
}

internal fun Context.windowManager(): WindowManager? {
    return getSystemService(Context.WINDOW_SERVICE) as? WindowManager
}

internal fun Int.densityValue(density: Float): Int {
    return (this / density).toInt()
}

@Suppress("DEPRECATION")
internal fun Context.screenSize(): PaylisherScreenSizeInfo? {
    val windowManager = windowManager() ?: return null
    val displayMetrics = displayMetrics()
    val screenHeight: Int
    val screenWidth: Int
    val density: Float
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val currentWindowMetrics = windowManager.currentWindowMetrics
        val screenBounds = currentWindowMetrics.bounds

//        TODO: do this when we upgrade API to 34
//        density = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            currentWindowMetrics.density
//        } else {
//            displayMetrics.density
//        }
        density = displayMetrics.density

        screenHeight = (screenBounds.bottom - screenBounds.top).densityValue(density)
        screenWidth = (screenBounds.right - screenBounds.left).densityValue(density)
    } else {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        screenHeight = size.y.densityValue(displayMetrics.density)
        screenWidth = size.x.densityValue(displayMetrics.density)
        density = displayMetrics.density
    }
    return PaylisherScreenSizeInfo(
        width = screenWidth,
        height = screenHeight,
        density = density,
    )
}

internal fun Context.appContext(): Context {
    return applicationContext ?: this
}

internal fun Context.hasPermission(permission: String): Boolean {
    return checkPermission(
        permission,
        Process.myPid(),
        Process.myUid(),
    ) == PackageManager.PERMISSION_GRANTED
}

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
internal fun Context.isConnected(): Boolean {
    val connectivityManager = connectivityManager() ?: return true

    if (!hasPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
        return true
    }
    val networkInfo = connectivityManager.activeNetworkInfo ?: return false
    return networkInfo.isConnected
}

internal fun Context.connectivityManager(): ConnectivityManager? {
    return getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
}

internal fun Context.telephonyManager(): TelephonyManager? {
    return getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
}

@Suppress("DEPRECATION")
internal fun Activity.activityLabelOrName(config: PaylisherAndroidConfig): String? {
    return try {
        val activityInfo = packageManager.getActivityInfo(componentName, GET_META_DATA)
        val activityLabel = activityInfo.loadLabel(packageManager).toString()
        val applicationLabel = applicationInfo.loadLabel(packageManager).toString()

        if (activityLabel.isNotEmpty() && activityLabel != applicationLabel) {
            if (activityLabel == activityInfo.name) {
                activityLabel.substringAfterLast('.')
            } else {
                activityLabel
            }
        } else {
            activityInfo.name.substringAfterLast('.')
        }
    } catch (e: Throwable) {
        config.logger.log("Error getting the Activity's label or name: $e.")
        null
    }
}

@Suppress("DEPRECATION")
internal fun Intent.getReferrerInfo(config: PaylisherAndroidConfig): Map<String, String> {
    val referrerInfoMap = mutableMapOf<String, String>()
    val referrer: Uri?
    val referrerName: String?
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
        referrer = this.getParcelableExtra(Intent.EXTRA_REFERRER)
        referrerName = this.getStringExtra(Intent.EXTRA_REFERRER_NAME)
    } else {
        referrer = this.getParcelableExtra("android.intent.extra.REFERRER")
        referrerName = this.getStringExtra("android.intent.extra.REFERRER_NAME")
    }
    referrer?.let {
        referrerInfoMap["\$referrer"] = referrer.toString()
        referrer.host?.let { referrerInfoMap["\$referring_domain"] = it }
    } ?: referrerName?.let {
        referrerInfoMap["\$referrer"] = referrerName
        referrerName.tryParse(config)?.let { uri ->
            uri.host?.let { referrerInfoMap["\$referring_domain"] = it }
        }
    }
    return referrerInfoMap
}

internal fun String.tryParse(config: PaylisherAndroidConfig): Uri? {
    return try {
        Uri.parse(this)
    } catch (e: Throwable) {
        config.logger.log("Error parsing string: $this. Exception: $e.")
        null
    }
}

internal fun isMainThread(mainHandler: MainHandler): Boolean {
    return Thread.currentThread().id == mainHandler.mainLooper.thread.id
}

@PaylisherInternal
@Suppress("DEPRECATION")
@Throws(PackageManager.NameNotFoundException::class)
public fun getApplicationInfo(context: Context): ApplicationInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context
            .packageManager
            .getApplicationInfo(
                context.packageName,
                PackageManager.ApplicationInfoFlags.of(GET_META_DATA.toLong()),
            )
    } else {
        context
            .packageManager
            .getApplicationInfo(context.packageName, GET_META_DATA)
    }
