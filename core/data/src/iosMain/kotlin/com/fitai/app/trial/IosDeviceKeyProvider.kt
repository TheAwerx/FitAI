@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class, ExperimentalUnsignedTypes::class)

package com.fitai.app.trial

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.errSecDuplicateItem
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecAttrSynchronizable
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.UIKit.UIDevice

/**
 * A random UUID kept in the Keychain with `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` and no
 * iCloud sync: it survives app reinstall but not a device wipe or restore to another phone. Apple
 * DeviceCheck would be the stronger follow-up (needs an Apple key; docs/trial.md).
 */
class IosDeviceKeyProvider : DeviceKeyProvider {

    private val mutex = Mutex()
    private var cached: DeviceKey? = null

    override val showsDiagnostics: Boolean = Platform.isDebugBinary

    override suspend fun deviceKey(): DeviceKey = mutex.withLock {
        cached ?: compute().also { cached = it }
    }

    private fun compute(): DeviceKey {
        val stored = Keychain.read(SERVICE, ACCOUNT) ?: run {
            val fresh = NSUUID().UUIDString
            when (Keychain.add(SERVICE, ACCOUNT, fresh)) {
                errSecSuccess -> fresh
                // Written meanwhile; read it back so every caller ends up with the same id.
                errSecDuplicateItem -> Keychain.read(SERVICE, ACCOUNT)
                else -> null
            }
        }
        if (stored != null) {
            return DeviceKey(sha256Hex("${DEVICE_KEY_SALT}ios:$stored"), DevicePlatform.Ios, DeviceKeySource.Keychain)
        }
        UIDevice.currentDevice.identifierForVendor?.UUIDString?.let { vendorId ->
            return DeviceKey(sha256Hex("${DEVICE_KEY_SALT}ios-vendor:$vendorId"), DevicePlatform.Ios, DeviceKeySource.VendorId)
        }
        val defaults = NSUserDefaults.standardUserDefaults
        val generated = defaults.stringForKey(KEY_GENERATED)
            ?: NSUUID().UUIDString.also { defaults.setObject(it, forKey = KEY_GENERATED) }
        return DeviceKey(sha256Hex("${DEVICE_KEY_SALT}ios-generated:$generated"), DevicePlatform.Ios, DeviceKeySource.Generated)
    }

    private fun sha256Hex(input: String): String {
        val bytes = input.encodeToByteArray()
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        bytes.usePinned { source ->
            digest.usePinned { target ->
                CC_SHA256(source.addressOf(0), bytes.size.convert(), target.addressOf(0))
            }
        }
        return digest.asByteArray().toHex()
    }

    private companion object {
        const val SERVICE = "com.fitai.app.device"
        const val ACCOUNT = "trial-device-id"
        const val KEY_GENERATED = "fitai_generated_device_id"
    }
}

/** Generic-password items via CoreFoundation, so nothing depends on toll-free bridging. */
private object Keychain {

    fun read(service: String, account: String): String? = memScoped {
        val owned = mutableListOf<CFTypeRef?>()
        val query = baseQuery(service, account, owned)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        release(query, owned)
        val value = result.value
        if (status != errSecSuccess || value == null) return@memScoped null
        val data: CFDataRef = value.reinterpret()
        val length = CFDataGetLength(data).toInt()
        val text = CFDataGetBytePtr(data)?.readBytes(length)?.decodeToString()
        CFRelease(value)
        text?.takeIf { it.isNotBlank() }
    }

    fun add(service: String, account: String, value: String): Int {
        val owned = mutableListOf<CFTypeRef?>()
        val query = baseQuery(service, account, owned)
        val bytes = value.encodeToByteArray()
        val data = bytes.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), bytes.size.convert()) }
        owned += data
        CFDictionaryAddValue(query, kSecValueData, data)
        CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        CFDictionaryAddValue(query, kSecAttrSynchronizable, kCFBooleanFalse)
        val status = SecItemAdd(query, null)
        release(query, owned)
        return status
    }

    private fun baseQuery(service: String, account: String, owned: MutableList<CFTypeRef?>): CFMutableDictionaryRef? {
        val query = CFDictionaryCreateMutable(null, 8, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        val cfService = CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
        val cfAccount = CFStringCreateWithCString(null, account, kCFStringEncodingUTF8)
        owned += cfService
        owned += cfAccount
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, cfService)
        CFDictionaryAddValue(query, kSecAttrAccount, cfAccount)
        return query
    }

    private fun release(query: CFMutableDictionaryRef?, owned: List<CFTypeRef?>) {
        query?.let { CFRelease(it) }
        owned.forEach { ref -> ref?.let { CFRelease(it) } }
    }
}
