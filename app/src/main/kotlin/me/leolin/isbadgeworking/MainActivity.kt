package me.leolin.isbadgeworking

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.leolin.api.IsBadgeWorkingApi
import me.leolin.api.ReportBadgeWorkingRequest
import me.leolin.shortcutbadger.ShortcutBadger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.toast
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {


    val isBadgeWorkingApi by lazy {
        Retrofit.Builder()
                .baseUrl(if (Build.VERSION.SDK_INT >= 19) {
                    "https://api.leolin.me/is-badge-working/"
                } else {
                    "http://api.leolin.me/is-badge-working/"
                })
                .addConverterFactory(JacksonConverterFactory.create(jacksonObjectMapper()))
                .client(
                        OkHttpClient().newBuilder()
                                .connectTimeout(15, TimeUnit.SECONDS)
                                .readTimeout(15, TimeUnit.SECONDS)
                                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
                                .build()
                )
                .build()
                .create(IsBadgeWorkingApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        renderAndSend()

        find<Button>(R.id.btnSetBadge).setOnClickListener {
            val i = Random().nextInt(98) + 1
            toast("Tried to set badge count to ${i}")
            ShortcutBadger.applyCount(this@MainActivity, i)
        }
        find<Button>(R.id.btnWorking).setOnClickListener { renderAndSend(true, true) }
        find<Button>(R.id.btnNotWorking).setOnClickListener { renderAndSend(false, true) }
    }

    fun renderAndSend(working: Boolean = false, send: Boolean = false) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentHomePackage = resolveInfo.activityInfo.packageName
        val packageInfo = packageManager.getPackageInfo(currentHomePackage, PackageManager.GET_ACTIVITIES)

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
                working
        )

        find<ListView>(R.id.listView).adapter = ListAdapter(
                listOf(
                        DisplayData("Launcher", "${req.launcherPackage} \n${req.launcherVersionCode} / ${req.launcherVersionName}"),
                        DisplayData("Device", "${req.deviceBrand} / ${req.deviceModel}"),
                        DisplayData("Android", "${req.androidVersion}"),
                        DisplayData("ShortcutBadger", req.shortcutBadgerVersion),
                        DisplayData("This App", "${req.reportAppVersionCode} / ${req.reportAppVersionName}")
                ),
                layoutInflater
        )


        if (send) {
            val dialog = indeterminateProgressDialog("Sending...") { show() }
            doAsync {
                try {
                    isBadgeWorkingApi.sendReport(req).execute().apply {
                        runOnUiThread {
                            dialog.dismiss()
                            if (code() == 200) {
                                toast("Thank you!")
                            } else {
                                toast("Hmm..something wrong, please try later.")
                            }
                        }
                    }
                } catch(e: Exception) {
                    Log.e("err", e.message, e)
                    runOnUiThread {
                        dialog.dismiss()
                        toast("Hmm..something wrong, please try later.")
                    }
                }

            }
        }
    }


    data class DisplayData(
            val label: String,
            val value: String
    )

    inner class ListAdapter(val displayDataList: List<DisplayData>, val layoutInflater: LayoutInflater) : BaseAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val displayData = displayDataList[position];
            return convertView ?: layoutInflater.inflate(R.layout.listitem, null).apply {
                this.find<TextView>(R.id.textViewLabel)?.apply { text = displayData.label }
                this.find<TextView>(R.id.textViewValue)?.apply { text = displayData.value }
            }
        }

        override fun getItem(position: Int) = displayDataList[position]

        override fun getItemId(position: Int) = position.toLong()

        override fun getCount() = displayDataList.size
    }

}
