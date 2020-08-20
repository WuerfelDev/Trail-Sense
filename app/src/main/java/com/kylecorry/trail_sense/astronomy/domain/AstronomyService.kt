package com.kylecorry.trail_sense.astronomy.domain

import com.kylecorry.trail_sense.astronomy.domain.moon.*
import com.kylecorry.trail_sense.astronomy.domain.sun.*
import com.kylecorry.trail_sense.navigation.domain.compass.Bearing
import com.kylecorry.trail_sense.shared.domain.Coordinate
import com.kylecorry.trail_sense.shared.roundNearestMinute
import com.kylecorry.trail_sense.shared.toZonedDateTime
import java.time.*
import com.kylecorry.trail_sense.astronomy.domain.sun.SunTimesMode as SunTimesMode

/**
 * The facade for astronomy related services
 */
class AstronomyService(private val clock: Clock = Clock.systemDefaultZone()) {

    private val moonPhaseCalculator = MoonPhaseCalculator()
    private val moonTimesCalculator = AltitudeMoonTimesCalculator()
    private val altitudeCalculator = AstronomicalAltitudeCalculator()

    // PUBLIC MOON METHODS

    fun getCurrentMoonPhase(): MoonPhase {
        return moonPhaseCalculator.getPhase(ZonedDateTime.now(clock))
    }

    /**
     * Gets the moon phase at noon (should this be rise/set?)
     */
    fun getMoonPhase(date: LocalDate): MoonPhase {
        val time = date.atTime(12, 0).toZonedDateTime()
        return moonPhaseCalculator.getPhase(time)
    }

    fun getMoonTimes(location: Coordinate, date: LocalDate): MoonTimes {
        return moonTimesCalculator.calculate(location, date)
    }

    fun getCenteredMoonAltitudes(location: Coordinate, time: LocalDateTime): List<AstroAltitude> {
        val startTime = time.roundNearestMinute(10).minusHours(12)
        return altitudeCalculator.getMoonAltitudes(location, startTime, Duration.ofDays(1), 10)
    }

    fun getMoonAltitudes(location: Coordinate, date: LocalDate): List<AstroAltitude> {
        return altitudeCalculator.getMoonAltitudes(location, date, 10)
    }

    fun getMoonAltitude(location: Coordinate, time: LocalDateTime): AstroAltitude {
        return altitudeCalculator.getMoonAltitude(location, time)
    }

    fun getMoonAzimuth(location: Coordinate): Bearing {
        return altitudeCalculator.getMoonAzimuth(location, LocalDateTime.now(clock))
    }

    fun isMoonUp(location: Coordinate): Boolean {
        val altitude = altitudeCalculator.getMoonAltitude(location, LocalDateTime.now(clock))
        return altitude.altitudeDegrees > 0
    }

    fun getLunarNoon(location: Coordinate, date: LocalDate = LocalDate.now()): LocalDateTime? {
        val moonTimes = getMoonTimes(location, date)
        if (moonTimes.down == null || moonTimes.up == null){
            return null
        }

        val approxNoon = moonTimes.up.plus(Duration.between(moonTimes.up, moonTimes.down).dividedBy(2))
        val hourBeforeNoon = approxNoon.minusHours(1)

        var maxAltitude: AstroAltitude? = null
        for (i in 0..(60 * 2 + 1)){
            val altitude = getMoonAltitude(location, hourBeforeNoon.plusMinutes(i.toLong()))
            if (maxAltitude == null || altitude.altitudeDegrees > maxAltitude.altitudeDegrees){
                maxAltitude = altitude
            }
        }

        return maxAltitude?.time
    }

    fun getTides(date: LocalDate = LocalDate.now()): Tide {
        val phase = getMoonPhase(date)
        return when(phase.phase){
            MoonTruePhase.New, MoonTruePhase.Full -> Tide.Spring
            MoonTruePhase.FirstQuarter, MoonTruePhase.ThirdQuarter -> Tide.Neap
            else -> Tide.Normal
        }
    }

    // PUBLIC SUN METHODS

    fun getSunTimes(location: Coordinate, sunTimesMode: SunTimesMode, date: LocalDate): SunTimes {
        return when (sunTimesMode){
            SunTimesMode.Actual -> ActualTwilightCalculator().calculate(location, date)
            SunTimesMode.Civil -> CivilTwilightCalculator().calculate(location, date)
            SunTimesMode.Nautical -> NauticalTwilightCalculator().calculate(location, date)
            SunTimesMode.Astronomical -> AstronomicalTwilightCalculator().calculate(location, date)
        }
    }

    fun getTodaySunTimes(location: Coordinate, sunTimesMode: SunTimesMode): SunTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock))
    }

    fun getTomorrowSunTimes(location: Coordinate, sunTimesMode: SunTimesMode): SunTimes {
        return getSunTimes(location, sunTimesMode, LocalDate.now(clock).plusDays(1))
    }

    fun getSunAltitudes(location: Coordinate, date: LocalDate): List<AstroAltitude> {
        return altitudeCalculator.getSunAltitudes(location, date, 10)
    }

    fun getCenteredSunAltitudes(location: Coordinate, time: LocalDateTime): List<AstroAltitude> {
        val startTime = time.roundNearestMinute(10).minusHours(12)
        return altitudeCalculator.getSunAltitudes(location, startTime, Duration.ofDays(1), 10)
    }

    fun getNextSunset(location: Coordinate, sunTimesMode: SunTimesMode): LocalDateTime? {
        val today = getTodaySunTimes(location, sunTimesMode)
        val tomorrow = getTomorrowSunTimes(location, sunTimesMode)
        return DateUtils.getClosestFutureTime(LocalDateTime.now(clock), listOf(today.down, tomorrow.down))
    }

    fun getNextSunrise(location: Coordinate, sunTimesMode: SunTimesMode): LocalDateTime? {
        val today = getTodaySunTimes(location, sunTimesMode)
        val tomorrow = getTomorrowSunTimes(location, sunTimesMode)
        return DateUtils.getClosestFutureTime(LocalDateTime.now(clock), listOf(today.up, tomorrow.up))
    }

    fun isSunUp(location: Coordinate): Boolean {
        val altitude = altitudeCalculator.getSunAltitude(location, LocalDateTime.now(clock))
        return altitude.altitudeDegrees > 0
    }

    fun getSunAzimuth(location: Coordinate): Bearing {
        return altitudeCalculator.getSunAzimuth(location, LocalDateTime.now(clock))
    }

    fun getSolarNoon(location: Coordinate, date: LocalDate = LocalDate.now()): LocalDateTime? {
        val sunTimes = getSunTimes(location, SunTimesMode.Actual, date)
        if (sunTimes.down == null || sunTimes.up == null || sunTimes.down.isBefore(sunTimes.up)){
            return null
        }

        val approxNoon = sunTimes.up.plus(Duration.between(sunTimes.up, sunTimes.down).dividedBy(2))
        val hourBeforeNoon = approxNoon.minusHours(1)

        var maxAltitude: AstroAltitude? = null
        for (i in 0..(60 * 2 + 1)){
            val altitude = getSunAltitude(location, hourBeforeNoon.plusMinutes(i.toLong()))
            if (maxAltitude == null || altitude.altitudeDegrees > maxAltitude.altitudeDegrees){
                maxAltitude = altitude
            }
        }

        return maxAltitude?.time
    }

    fun getSunAltitude(location: Coordinate, time: LocalDateTime): AstroAltitude {
        return altitudeCalculator.getSunAltitude(location, time)
    }

}