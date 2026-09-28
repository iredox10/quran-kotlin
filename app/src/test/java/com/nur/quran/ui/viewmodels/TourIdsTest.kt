package com.nur.quran.ui.viewmodels

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the tour skip-persistence bug: the completed-tours flow seeds from
 * persisted flags, so EVERY tour id shown in UI must be registered. When
 * "home-tour-advanced" was missing here, skipping it never stuck and it
 * reappeared on every launch despite its flag being true.
 */
class TourIdsTest {

    @Test
    fun `all UI tour ids are registered`() {
        // Every id passed as PageTourModal(tourId = ...) in UI. Others
        // (memorization/planner/library) exist only as onboarding rows.
        val usedInUi = setOf(
            "home-tour",
            "home-tour-advanced"
        )
        val missing = usedInUi - OnboardingTours.KNOWN_TOUR_IDS
        assertTrue(
            "Tour ids shown in UI but missing from KNOWN_TOUR_IDS: $missing",
            missing.isEmpty()
        )
    }

    @Test
    fun `onboarding rows stay a subset of known ids`() {
        val rows = OnboardingTours.ROWS.map { it.id }.toSet()
        assertTrue(OnboardingTours.KNOWN_TOUR_IDS.containsAll(rows))
    }
}
