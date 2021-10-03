package com.kylecorry.trail_sense.weather.infrastructure.clouds

import com.kylecorry.sol.science.meteorology.clouds.CloudType

data class CloudObservation(
    val cover: Float,
    val luminance: Float,
    val contrast: Float,
    val type: CloudType?,
    val typeConfidence: Float?
)
// TODO Add the following features
/*
    - blueEnergy
    - blueEntropy
    - blueContrast
    - blueHomogenity
 */