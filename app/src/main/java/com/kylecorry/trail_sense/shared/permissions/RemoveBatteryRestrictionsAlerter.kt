package com.kylecorry.trail_sense.shared.permissions

import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.IAlerter

class RemoveBatteryRestrictionsAlerter(private val fragment: Fragment) : IAlerter {

    override fun alert() {
        val context = fragment.requireContext()
        CustomUiUtils.snackbar(
            fragment,
            context.getString(R.string.battery_settings_limit_accuracy),
            Snackbar.LENGTH_LONG,
            context.getString(R.string.learn_more)
        ) {
            Alerts.dialog(
                context,
                context.getString(R.string.remove_battery_restrictions),
                MarkdownService(context).toMarkdown(context.getString(R.string.battery_usage_restricted_benefit)),
                allowLinks = true
            ) { cancelled ->
                if (!cancelled) {
                    RemoveBatteryRestrictionsCommand(context).execute()
                }
            }
        }
    }

}