package com.ranzan.zbuilds.domain.usecase

import com.ranzan.zbuilds.domain.repository.PackageInspectorRepository

/**
 * Version name of this app as installed on the device.
 */
class GetAppVersionUseCase(
    private val packageInspector: PackageInspectorRepository
) {
    operator fun invoke(): String? = packageInspector.getOwnVersionName()
}
