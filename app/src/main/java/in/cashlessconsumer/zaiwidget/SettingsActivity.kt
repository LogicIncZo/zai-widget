package `in`.cashlessconsumer.zaiwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val input = findViewById<EditText>(R.id.key_input)
        Prefs.apiKey(this)?.let { input.setText(it) }
        val clock = findViewById<CheckBox>(R.id.opt_clock)
        val stats = findViewById<CheckBox>(R.id.opt_stats)
        clock.isChecked = Prefs.showClock(this)
        stats.isChecked = Prefs.showStats(this)
        findViewById<Button>(R.id.save).setOnClickListener {
            Prefs.saveKey(this, input.text.toString())
            Prefs.setShowClock(this, clock.isChecked)
            Prefs.setShowStats(this, stats.isChecked)
            val mgr = AppWidgetManager.getInstance(this)
            val ids = mgr.getAppWidgetIds(ComponentName(this, WidgetProvider::class.java))
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                    ?: AppWidgetManager.INVALID_APPWIDGET_ID)
            setResult(RESULT_OK, result)
            WidgetProvider().onUpdate(this, mgr, ids)
            finish()
        }
    }
}
