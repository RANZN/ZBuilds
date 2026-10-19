package com.ranzan.zbuilds.domain.repository

interface PackageInspectorRepository {
    fun isPackageInstalled(packageName: String): Boolean
    fun getInstalledVersionName(packageName: String): String?
    fun inspectApkPackageName(apkFilePath: String): String?
    fun openApp(packageName: String): Boolean
}
