package me.leolin.isbadgeworking

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.leolin.api.IsBadgeWorkingApi
import me.leolin.api.ReportBadgeWorkingRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {


    val isBadgeWorkingApi by lazy {
        Retrofit.Builder()
                .baseUrl("https://api.leolin.me/is-badge-working/")
                .addConverterFactory(JacksonConverterFactory.create(jacksonObjectMapper()))
                .client(
                        OkHttpClient().newBuilder()
                                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                                .build()
                )
                .build()
                .create(IsBadgeWorkingApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentHomePackage = resolveInfo.activityInfo.packageName
        val packageInfo = packageManager.getPackageInfo(currentHomePackage, PackageManager.GET_ACTIVITIES)
        println("currentHomePackage=$currentHomePackage")
        println("versionCode=${packageInfo.versionCode}")
        println("versionName=${packageInfo.versionName}")
        println("SERIAL=${Build.SERIAL}")
        println("MODEL=${Build.MODEL}")
        println("MANUFACTURER=${Build.MANUFACTURER}")
        println("Build.VERSION.RELEASE=${Build.VERSION.RELEASE}")
        println("Build.VERSION.SDK_INT=${Build.VERSION.SDK_INT}")


        val req = ReportBadgeWorkingRequest(
                Build.SERIAL,
                Build.MODEL,
                Build.MANUFACTURER,
                BuildConfig.SHORTCUTBADGER_VERSION,
                Build.VERSION.RELEASE,
                currentHomePackage,
                packageInfo.versionName,
                packageInfo.versionCode.toString(),
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE.toString(),
                false
        )

        println(req)

        thread {
            isBadgeWorkingApi.sendReport(req).execute().apply {
                println(code())
            }
        }
    }


}
