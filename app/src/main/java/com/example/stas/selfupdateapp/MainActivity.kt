package com.example.stas.selfupdateapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import java.io.BufferedInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

    companion object {
        const val BUILD_URL_KEY = "MainActivity.buildUrl"
        const val BUILD_HASH_SUM_KEY = "MainActivity.buildHashSum"

        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        private const val fileName = "install_me.apk"
    }

    // todo: remove hardcode
    private val newBuildLink = "https://drive.google.com/uc?export=download&id=1CAGmSGqMIToWnoPbNAnNugMWj_eNGSgh"
    // todo: remove hardcode
    private val newBuildCheckSum = "841651841218CD4CDC7BA48E4909231DAD685E45"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        UI(true) {
            relativeLayout {

                button("update") {
                    onClick {
                        onUpdateClicked()
                    }
                }.lparams {
                    centerInParent()
                }

                textView("Version:${BuildConfig.VERSION_CODE}").lparams {
                    alignParentBottom()
                }
            }
        }
    }

    private fun onUpdateClicked() {
        launch(kotlinx.coroutines.experimental.android.UI) {
            async(CommonPool) {
                downloadApk(intent.extras?.getString(BUILD_URL_KEY) ?: newBuildLink)
            }.await()
            install()
        }
    }


    /**
     * Download apk file to internal storage.
     * @param url - direct url to .apk file
     */
    @SuppressLint("WorldReadableFiles")
    private fun downloadApk(url: String) {
        try {
            // open http connection
            val connection = (URL(url).openConnection() as HttpURLConnection)
            BufferedInputStream(connection.inputStream).use { inputStream ->
                // open file in internal storage
                openFileOutput(fileName, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Context.MODE_PRIVATE else Context.MODE_WORLD_READABLE).use { out ->
                    inputStream.copyTo(out)
                }
            }
            // close connection
            connection.disconnect()
        } catch (e: Exception) {
            installationFailed(FailReasons.IOException)
        }
    }

    /**
     * Check downloaded file and start installation activity
     */
    private fun install() {
        val apkFile = File(filesDir, fileName)

        //check SHA-1 hash
        if (intent.extras?.getString(BUILD_HASH_SUM_KEY) ?: newBuildCheckSum != apkFile.sha1) {
            installationFailed(FailReasons.CheckSumIsIncorrect)
            return
        }

        //build installation intent
        val installIntent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        setDataAndType(
                                FileProvider.getUriForFile(this@MainActivity, BuildConfig.APPLICATION_ID + ".fileprovider", apkFile),
                                APK_MIME_TYPE)
                    }
                } else {
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(
                                Uri.parse("file://" + apkFile.absolutePath),
                                APK_MIME_TYPE)
                    }
                }

        //launch installation activity
        finish()
        startActivity(installIntent)
    }

    private fun installationFailed(reason: FailReasons) {
        toast(when (reason) {
            FailReasons.IOException -> "Failed to download update"
            FailReasons.CheckSumIsIncorrect -> "Failed to install update"
        })
    }

}