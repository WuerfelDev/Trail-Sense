package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.chart.Chart
import com.kylecorry.trail_sense.weather.ui.PressureChart

class PressureChartPreference(context: Context, attributeSet: AttributeSet) :
    Preference(context, attributeSet) {

    private var chart: PressureChart? = null
    private var data: List<Reading<Pressure>> = listOf()


    init {
        layoutResource = R.layout.preference_pressure_chart
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.itemView.isClickable = false

        chart = PressureChart(holder.findViewById(R.id.chart) as Chart)
        chart?.plot(data)
    }

    fun plot(
        data: List<Reading<Pressure>>,
        raw: List<Reading<Pressure>>? = null
    ) {
        this.data = data
        chart?.plot(data, raw)
    }
}