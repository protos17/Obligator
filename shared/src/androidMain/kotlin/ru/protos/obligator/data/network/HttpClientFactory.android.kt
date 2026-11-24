package ru.protos.obligator.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal data class RussianTrustedSslConfig(
    val sslContext: SSLContext,
    val trustManager: X509TrustManager
)

internal fun createRussianTrustedSslConfig(
    certificatePem: String = RUSSIAN_TRUSTED_ROOT_CA_PEM
): RussianTrustedSslConfig {
    val defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm()

    val defaultTrustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm).apply {
        init(null as KeyStore?)
    }
    val systemTrustManager = defaultTrustManagerFactory.trustManagers
        .filterIsInstance<X509TrustManager>()
        .firstOrNull()
        ?: error("Default X509TrustManager is not available")

    val certificateFactory = CertificateFactory.getInstance("X.509")
    val russianRootCa = ByteArrayInputStream(certificatePem.toByteArray(Charsets.UTF_8)).use { input ->
        certificateFactory.generateCertificate(input) as X509Certificate
    }

    val combinedKeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
        load(null, null)
        systemTrustManager.acceptedIssuers.forEachIndexed { index, certificate ->
            setCertificateEntry("system_ca_$index", certificate)
        }
        setCertificateEntry("russian_trusted_root_ca", russianRootCa)
    }

    val combinedTrustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm).apply {
        init(combinedKeyStore)
    }
    val combinedTrustManager = combinedTrustManagerFactory.trustManagers
        .filterIsInstance<X509TrustManager>()
        .firstOrNull()
        ?: error("Combined X509TrustManager is not available")

    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, arrayOf(combinedTrustManager), null)
    }

    return RussianTrustedSslConfig(
        sslContext = sslContext,
        trustManager = combinedTrustManager
    )
}

actual fun createPlatformHttpClient(
    config: HttpClientConfig<*>.() -> Unit
): HttpClient = HttpClient(OkHttp) {
    engine {
        val sslConfig = createRussianTrustedSslConfig()
        config {
            sslSocketFactory(sslConfig.sslContext.socketFactory, sslConfig.trustManager)
        }
    }
    config()
}
