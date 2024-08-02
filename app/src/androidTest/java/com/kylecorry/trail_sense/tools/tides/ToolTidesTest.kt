package com.kylecorry.trail_sense.tools.tides

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.test.core.app.ActivityScenario
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.CustomUiUtils.isDarkThemeOn
import com.kylecorry.trail_sense.shared.extensions.findNavController
import com.kylecorry.trail_sense.test_utils.TestUtils
import com.kylecorry.trail_sense.test_utils.TestUtils.not
import com.kylecorry.trail_sense.test_utils.TestUtils.waitFor
import com.kylecorry.trail_sense.test_utils.views.Side
import com.kylecorry.trail_sense.test_utils.views.click
import com.kylecorry.trail_sense.test_utils.views.hasText
import com.kylecorry.trail_sense.test_utils.views.input
import com.kylecorry.trail_sense.test_utils.views.isChecked
import com.kylecorry.trail_sense.test_utils.views.quickAction
import com.kylecorry.trail_sense.test_utils.views.toolbarButton
import com.kylecorry.trail_sense.test_utils.views.view
import com.kylecorry.trail_sense.test_utils.views.viewWithText
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ToolTidesTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val grantPermissionRule = TestUtils.mainPermissionsGranted()

    @get:Rule
    val instantExec = InstantTaskExecutorRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        TestUtils.setWaitForIdleTimeout(100)
        TestUtils.setupApplication()
        scenario = TestUtils.startWithTool(Tools.TIDES)
    }

    @Test
    fun verifyBasicFunctionality() {
        // Wait for the disclaimer
        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        // Wait for the no tides dialog
        waitFor {
            viewWithText(R.string.no_tides)
            viewWithText(android.R.string.ok).click()
        }

        // Tide list
        waitFor {
            view(R.id.tide_list_title).hasText(R.string.tides)
            view(R.id.tides_empty_text).hasText(R.string.no_tides)
        }

        canCreateTide()
        canViewTide()
        canOpenTideList()
    }

    private fun canCreateTide() {
        // Click the add button
        view(R.id.add_btn).click()

        // Enter the tide details
        waitFor {
            view(R.id.create_tide_title).hasText(R.string.tide_table)
        }
        view(R.id.tide_name).input("Tide 1")
        view(R.id.tide_frequency_semidiurnal).isChecked()
        view(R.id.utm).input("42, -72")

        view(R.id.tide_type).hasText(R.string.high_tide_letter)
        view(R.id.tide_type).click()
        view(R.id.tide_type).hasText(R.string.low_tide_letter)
        view(R.id.tide_type).click()
        view(R.id.tide_type).hasText(R.string.high_tide_letter)

        view(R.id.tide_time, index = 1).click()
        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        waitFor {
            viewWithText(android.R.string.ok).click()
        }

        // TODO: Verify the time is set
        waitFor {
            view(R.id.tide_time, index = 1).hasText { it.isNotBlank() }
        }

        view(R.id.tide_height, index = 1).click()
        waitFor {
            viewWithText(R.string.distance).input("1.0")
            viewWithText(android.R.string.ok).click()
        }

        waitFor {
            view(R.id.tide_height, index = 1).hasText("1.00 ft")
        }

        view(R.id.add_tide_entry).click()

        view(R.id.tide_type, index = 1).hasText(R.string.high_tide_letter)
        view(R.id.delete, index = 1).click()

        waitFor {
            viewWithText(R.string.tide_deleted)
            not { view(R.id.tide_type, index = 1) }
        }

        waitFor {
            toolbarButton(R.id.create_tide_title, Side.Right).click()
        }

        waitFor {
            view(R.id.tide_list_title).hasText(R.string.tides)
            view(com.kylecorry.andromeda.views.R.id.title).hasText("Tide 1")
            view(com.kylecorry.andromeda.views.R.id.description).hasText("1 tide")
        }
    }

    private fun canViewTide() {
        view(com.kylecorry.andromeda.views.R.id.title).click()
        waitFor {
            view(R.id.tide_title).hasText(R.string.high_tide)
            view(R.id.tide_title).hasText("Tide 1")
        }

        // Verify that today is selected
        view(R.id.tide_list_date).hasText(R.string.today)

        // Verify at least one high and low tide is shown
        var isHighFirst = false
        view(com.kylecorry.andromeda.views.R.id.title).hasText {
            isHighFirst = it == TestUtils.getString(R.string.high_tide)
            it == TestUtils.getString(R.string.high_tide) || it == TestUtils.getString(R.string.low_tide)
        }

        view(com.kylecorry.andromeda.views.R.id.title, index = 1).hasText(
            if (isHighFirst) {
                R.string.low_tide
            } else {
                R.string.high_tide
            }
        )

        // TODO: Verify the times are correct
        // TODO: Verify the chart
    }

    private fun canOpenTideList(){
        toolbarButton(R.id.tide_title, Side.Right).click()

        waitFor {
            view(R.id.tide_list_title).hasText(R.string.tides)
            view(com.kylecorry.andromeda.views.R.id.title).hasText("Tide 1")
        }
    }
}