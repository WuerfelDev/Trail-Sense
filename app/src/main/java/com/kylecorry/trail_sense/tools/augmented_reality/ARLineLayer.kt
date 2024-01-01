package com.kylecorry.trail_sense.tools.augmented_reality

import android.graphics.Color
import android.graphics.Path
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.canvas.ICanvasDrawer
import com.kylecorry.andromeda.canvas.StrokeCap
import com.kylecorry.andromeda.canvas.StrokeJoin
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.math.SolMath
import com.kylecorry.sol.math.SolMath.normalizeAngle
import com.kylecorry.trail_sense.tools.augmented_reality.position.ARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.position.AugmentedRealityCoordinate
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

// TODO: Create a generic version of this that works like the path tool. The consumers should be able to specify the line style, color, thickness, and whether it should be curved or straight between points
class ARLineLayer(
    @ColorInt private val color: Int = Color.WHITE,
    private val thickness: Float = 1f,
    private val thicknessType: ThicknessType = ThicknessType.Dp,
    private val curved: Boolean = true
) : ARLayer {

    private val path = Path()

    private val lines = mutableListOf<List<ARPoint>>()
    private val lineLock = Any()

    fun setLines(lines: List<List<ARPoint>>) {
        synchronized(lineLock) {
            this.lines.clear()
            this.lines.addAll(lines)
        }
    }

    fun clearLines() {
        synchronized(lineLock) {
            lines.clear()
        }
    }

    /*
     * Desired algorithm (for curved lines):
     * 1. Split the line into smaller lines based on the resolution
     * 2. For each line, get the pixels that make up the line
     * 3. Clip the line to the view
     * 4. Remove connections between lines that are too far apart (ex. crossing the view)
     * 5. Draw the lines
     *
     * Desired algorithm (for straight lines):
     * 1. Get the pixels that make up the line
     * 2. Split the line into smaller lines based on the resolution
     * 3. Clip the line to the view
     * 4. Remove connections between lines that are too far apart (ex. crossing the view)
     * 5. Draw the lines
     */

    override fun draw(drawer: ICanvasDrawer, view: AugmentedRealityView) {
        val maxAngle = hypot(view.fov.width, view.fov.height) * 1.5f
        val resolutionDegrees = (maxAngle / 10f).roundToInt().coerceIn(1, 5)

        val thicknessPx = when(thicknessType){
            ThicknessType.Dp -> drawer.dp(thickness)
            ThicknessType.Angle -> {
                view.sizeToPixel(thickness)
            }
        }

        drawer.noFill()
        drawer.strokeWeight(thicknessPx)
        drawer.strokeJoin(StrokeJoin.Round)
        drawer.strokeCap(StrokeCap.Round)

        val maxDistance = min(view.width, view.height)
        // TODO: Divide up the line into smaller chunks

        val lines = synchronized(lineLock) {
            lines.toList()
        }

        // Draw horizontal lines
        for (line in lines) {
            path.reset()
            drawer.stroke(color)

            if (curved) {
                // Curved + increased resolution
                val pixels = getLinePixels(
                    view,
                    line,
                    resolutionDegrees.toFloat(),
                    maxDistance.toFloat()
                )
                for (pixelLine in pixels) {
                    var previous: PixelCoordinate? = null
                    for (pixel in pixelLine) {
                        if (previous != null) {
                            path.lineTo(pixel.x, pixel.y)
                        } else {
                            path.moveTo(pixel.x, pixel.y)
                        }
                        previous = pixel
                    }
                }
            } else {
                // TODO: This should split the lines into smaller chunks (which will allow distance splitting) - keeping it this way for now for the clinometer
                var previous: PixelCoordinate? = null
                for (point in line) {
                    val pixel = view.toPixel(point.getAugmentedRealityCoordinate(view))
                    // TODO: This should split the line if the distance is too great
                    if (previous != null) {
                        path.lineTo(pixel.x, pixel.y)
                    } else {
                        path.moveTo(pixel.x, pixel.y)
                    }
                    previous = pixel
                }
            }

            drawer.path(path)
        }

        drawer.noStroke()
    }

    /**
     * Given a line it returns the pixels that make up the line.
     * It will split long lines into smaller chunks, using the resolutionDegrees.
     * It will also clip the line to the view.
     */
    private fun getLinePixels(
        view: AugmentedRealityView,
        line: List<ARPoint>,
        resolutionDegrees: Float,
        maxDistance: Float,
    ): List<List<PixelCoordinate>> {
        val pixels = mutableListOf<PixelCoordinate>()
        var previousCoordinate: AugmentedRealityCoordinate? = null
        for (point in line) {
            val coord = point.getAugmentedRealityCoordinate(view)
            pixels.addAll(if (previousCoordinate != null) {
                splitLine(previousCoordinate, coord, resolutionDegrees).map { view.toPixel(it) }
            } else {
                listOf(view.toPixel(coord))
            })

            previousCoordinate = coord
        }


        // If there are any points that are further apart than maxDistance, split them up
        val splitPixels = mutableListOf<List<PixelCoordinate>>()
        var previousPixel: PixelCoordinate? = null
        var currentLine = mutableListOf<PixelCoordinate>()
        for (pixel in pixels) {
            if (previousPixel != null && pixel.distanceTo(previousPixel) > maxDistance) {
                splitPixels.add(currentLine)
                currentLine = mutableListOf()
            }
            currentLine.add(pixel)
            previousPixel = pixel
        }
        if (currentLine.isNotEmpty()) {
            splitPixels.add(currentLine)
        }

        // Clip the lines to the view
        return splitPixels.filter { line ->
            line.isNotEmpty()
        }
    }

    // TODO: Should this operate on pixels or coordinates? - if it is pixels, it will be linear, if it is coordinates it will be curved
    private fun splitLine(
        start: AugmentedRealityCoordinate,
        end: AugmentedRealityCoordinate,
        resolutionDegrees: Float
    ): List<AugmentedRealityCoordinate> {
        val coordinates = mutableListOf<AugmentedRealityCoordinate>()
        coordinates.add(start)
        val bearingDelta = SolMath.deltaAngle(start.bearing, end.bearing)
        val elevationDelta = end.elevation - start.elevation
        val distanceDelta = end.distance - start.distance
        val distance = hypot(bearingDelta, elevationDelta)
        val steps = (distance / resolutionDegrees).roundToInt()
        val bearingStep = bearingDelta / steps
        val elevationStep = elevationDelta / steps
        val distanceStep = distanceDelta / steps
        for (i in 1..steps) {
            coordinates.add(
                AugmentedRealityCoordinate.fromSpherical(
                    normalizeAngle(start.bearing + bearingStep * i),
                    start.elevation + elevationStep * i,
                    start.distance + distanceStep * i
                )
            )
        }

        // Add the end point
        coordinates.add(end)

        return coordinates
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: ICanvasDrawer,
        view: AugmentedRealityView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override fun onFocus(drawer: ICanvasDrawer, view: AugmentedRealityView): Boolean {
        return false
    }

    // TODO: Instead of this, pass in an AR size or something
    enum class ThicknessType {
        Dp, Angle
    }
}