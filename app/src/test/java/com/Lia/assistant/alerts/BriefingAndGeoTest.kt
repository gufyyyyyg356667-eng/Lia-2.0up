package com.Lia.assistant.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BriefingAndGeoTest {
    @Test fun briefingWithoutEvents() = assertTrue(Briefing.morning("বৃষ্টি নেই।", emptyList()).contains("কোনো ক্যালেন্ডার ইভেন্ট নেই"))
    @Test fun briefingListsEvents() = assertTrue(Briefing.morning(null, listOf("Meeting", "Dentist")).contains("Meeting, Dentist"))
    @Test fun samePointIsZeroMetres() = assertEquals(0, Geo.distanceMeters(23.8, 90.4, 23.8, 90.4))
    @Test fun roughlyOneKmPerHundredthOfLatitude() {
        val d = Geo.distanceMeters(23.80, 90.40, 23.81, 90.40)
        assertTrue("was $d", d in 1000..1200)
    }
}
