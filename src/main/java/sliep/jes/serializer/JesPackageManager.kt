@file:Suppress("unused")

package sliep.jes.serializer

import java.util.*
import kotlin.math.min

private fun versionToInt(version: String): IntArray {
    val parts = ArrayList<Int>()
    version.split('.').forEach { part -> kotlin.runCatching { parts.add(part.toInt()) } }
    return parts.toIntArray()
}

/**
 * Use this class to manage library properties and check version compatibility
 * @author sliep
 * @property VERSION of library
 */
@Suppress("PropertyName")
class JesPackage private constructor(packageInfo: Any) {
    @JvmField
    val ID: String = packageInfo.field("ID")
    @JvmField
    val GROUP: String = packageInfo.field("GROUP")
    @JvmField
    val VERSION: String = packageInfo.field("VERSION")
    internal val versionInt = versionToInt(VERSION)

    companion object {
        @JvmStatic
        fun findPackageBy(artifactId: String, group: String = "sliep.jes") =
            try {
                JesPackage(Class.forName("$group.$artifactId.PackageInfo").fieldR("INSTANCE")[null])
            } catch (e: Throwable) {
                throw IllegalStateException("Package (id=$artifactId group=$group) not a jes package!")
            }
    }

    override fun toString() = thisToString()
}

/**
 * Check if artifact version is after given version (e.g. package after "1.2.3")
 * @author sliep
 * @param otherVersion to compare
 * @return true if current version is equal or greater than given one
 */
infix fun JesPackage.after(otherVersion: String): Boolean {
    val otherVersionInt = versionToInt(otherVersion)
    for (i in 0 until min(versionInt.size, otherVersionInt.size)) {
        when {
            versionInt[i] < otherVersionInt[i] -> return false
            versionInt[i] > otherVersionInt[i] -> return true
        }
    }
    return versionInt.size >= otherVersionInt.size
}

/**
 * Check if artifact version is before given version (e.g. package before "1.2.3")
 * @author sliep
 * @param otherVersion to compare
 * @return true if current version is less than given one
 */
infix fun JesPackage.before(otherVersion: String) = !after(otherVersion)

/**
 * Check if artifact version is equal to given version (e.g. package isVersion "1.2.3")
 * @author sliep
 * @param otherVersion to compare
 * @return true if current version is equal
 */
infix fun JesPackage.isVersion(otherVersion: String) = Arrays.equals(versionToInt(otherVersion), versionInt)
