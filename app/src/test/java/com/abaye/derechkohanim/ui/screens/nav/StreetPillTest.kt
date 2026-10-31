package com.abaye.derechkohanim.ui.screens.nav

import com.abaye.derechkohanim.ui.theme.FontSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreetPillTest {

    @Test
    fun joinsRoadAndStreet() {
        assertEquals("כביש 1 • דרך בגין", streetPillText("כביש 1", "דרך בגין"))
    }

    @Test
    fun showsWhicheverExists() {
        assertEquals("כביש 443", streetPillText("כביש 443", null))
        assertEquals("הרצל", streetPillText(null, "הרצל"))
        assertNull(streetPillText(null, " "))
    }

    @Test
    fun fontStepsDownWithLength() {
        assertEquals(FontSize.body, streetPillFontSize(16))
        assertEquals(FontSize.label, streetPillFontSize(17))
        assertEquals(FontSize.label, streetPillFontSize(24))
        assertEquals(FontSize.caption, streetPillFontSize(25))
    }

    @Test
    fun speedKmhRoundsAndNeverGoesNegative() {
        assertEquals(50, speedKmh(13.9))
        assertEquals(0, speedKmh(-1.0))
    }
}
