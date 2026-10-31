package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.builder.ApkPackagingEngine
import com.example.data.BuildOutputFormat
import com.example.data.SourceType
import com.example.data.WebAppProjectEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and verify installable Signed APK and Studio ZIP builder`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Web2APK Studio", appName)

        val engine = ApkPackagingEngine(context)
        val project = WebAppProjectEntity(
            id = 7L,
            appName = "My Portal",
            packageName = "com.portal.webapp",
            versionName = "1.0.0",
            versionCode = 2,
            sourceType = SourceType.WEBSITE_URL.name,
            websiteUrl = "https://portal.org"
        )

        val signedRecord = engine.buildArtifact(project, BuildOutputFormat.SIGNED_APK) {}
        val apkFile = File(signedRecord.filePath)
        assertTrue(apkFile.exists())
        assertTrue(signedRecord.fileSizeBytes > 0)
        assertTrue(signedRecord.sha256Signature.isNotBlank())

        ZipFile(apkFile).use { zip ->
            assertTrue(zip.getEntry("AndroidManifest.xml") != null)
            assertTrue(zip.getEntry("META-INF/MANIFEST.MF") != null)
            assertTrue(zip.getEntry("META-INF/CERT.SF") != null)
            assertTrue(zip.getEntry("META-INF/CERT.RSA") != null)
            val embeddedEntry = zip.getEntry("assets/embedded_webapp_config.json")
            assertTrue(embeddedEntry != null)
            val parsed = ApkPackagingEngine.parseEmbeddedConfig(
                zip.getInputStream(embeddedEntry).bufferedReader().readText()
            )
            assertEquals("My Portal", parsed.appName)
            assertEquals("https://portal.org", parsed.websiteUrl)
        }

        val zipRecord = engine.buildArtifact(project, BuildOutputFormat.ANDROID_STUDIO_ZIP) {}
        assertTrue(File(zipRecord.filePath).exists())
        assertTrue(zipRecord.fileSizeBytes > 0)
    }
}
