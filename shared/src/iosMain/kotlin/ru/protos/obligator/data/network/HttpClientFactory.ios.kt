package ru.protos.obligator.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.ptr
import platform.CoreFoundation.CFArrayAppendValue
import platform.CoreFoundation.CFArrayCreateMutable
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeArrayCallBacks
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.create
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecCertificateCreateWithData
import platform.Security.SecCertificateRef
import platform.Security.SecPolicyCreateSSL
import platform.Security.SecTrustEvaluateWithError
import platform.Security.SecTrustSetAnchorCertificates
import platform.Security.SecTrustSetAnchorCertificatesOnly
import platform.Security.SecTrustSetPolicies
import platform.Security.errSecSuccess

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun createRussianTrustedRootCertificate(): SecCertificateRef? {
    val derData = NSData.create(
        base64EncodedString = russianTrustedRootCaBase64Der(),
        options = NSDataBase64DecodingIgnoreUnknownCharacters
    ) ?: return null

    @Suppress("UNCHECKED_CAST")
    val cfData = CFBridgingRetain(derData) as? CFDataRef ?: return null
    return try {
        SecCertificateCreateWithData(null, cfData)
    } finally {
        CFRelease(cfData)
    }
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
actual fun createPlatformHttpClient(
    config: HttpClientConfig<*>.() -> Unit
): HttpClient = HttpClient(Darwin) {
    engine {
        handleChallenge { _, _, challenge, completionHandler ->
            if (challenge.protectionSpace.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
                return@handleChallenge
            }

            val serverTrust = challenge.protectionSpace.serverTrust
            if (serverTrust == null) {
                completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
                return@handleChallenge
            }

            val hostCFString = CFStringCreateWithCString(
                null,
                challenge.protectionSpace.host,
                kCFStringEncodingUTF8
            )
            if (hostCFString != null) {
                try {
                    val policy = SecPolicyCreateSSL(true, hostCFString)
                    if (policy != null) {
                        try {
                            SecTrustSetPolicies(serverTrust, policy)
                        } finally {
                            CFRelease(policy)
                        }
                    }
                } finally {
                    CFRelease(hostCFString)
                }
            }

            val russianRootCert = createRussianTrustedRootCertificate()
            if (russianRootCert != null) {
                try {
                    val anchors = CFArrayCreateMutable(
                        kCFAllocatorDefault,
                        1,
                        kCFTypeArrayCallBacks.ptr
                    )
                    if (anchors != null) {
                        try {
                            CFArrayAppendValue(anchors, russianRootCert)
                            val anchorsStatus = SecTrustSetAnchorCertificates(serverTrust, anchors)
                            val anchorsOnlyStatus = SecTrustSetAnchorCertificatesOnly(serverTrust, false)
                            if (anchorsStatus == errSecSuccess &&
                                anchorsOnlyStatus == errSecSuccess &&
                                SecTrustEvaluateWithError(serverTrust, null)
                            ) {
                                completionHandler(
                                    NSURLSessionAuthChallengeUseCredential,
                                    NSURLCredential.credentialForTrust(serverTrust)
                                )
                                return@handleChallenge
                            }
                        } finally {
                            CFRelease(anchors)
                        }
                    }
                } finally {
                    CFRelease(russianRootCert)
                }
            }

            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
        }
    }
    config()
}
