package com.ranzan.zbuilds.data.local

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.ranzan.zbuilds.domain.repository.PackageInspectorRepository
import java.io.File

class PackageInspectorImpl(
    private val context: Context
) : PackageInspectorRepository {

    override fun isPackageInstalled(packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    override fun getInstalledVersionName(packageName: String): String? {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            info.versionName
        } catch (e: Exception) {
            null
        }
    }

    override fun inspectApkPackageName(apkFilePath: String): String? {
        return try {
            val file = File(apkFilePath)
            if (!file.exists()) return null
            val archiveInfo = context.packageManager.getPackageArchiveInfo(apkFilePath, 0)
            archiveInfo?.packageName
        } catch (e: Exception) {
            null
        }
    }

    override fun openApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
