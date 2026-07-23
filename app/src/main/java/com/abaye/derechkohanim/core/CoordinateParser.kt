package com.abaye.derechkohanim.core

import com.abaye.derechkohanim.data.model.LatLng

/**
 * Recognises a pasted coordinate pair so the destination field can answer without any
 * network call. Accepts "32.0873, 34.8288", "32.0873 34.8288" and the same wrapped in
 * parentheses. Israel is comfortably inside the valid ranges, so no extra bounds check.
 */
object CoordinateParser {

    private val pattern = Regex(
        """^\(?\s*([+-]?\d{1,3}(?:\.\d+)?)\s*[,;\s]\s*([+-]?\d{1,3}(?:\.\d+)?)\s*\)?$"""
    )

    fun parse(input: String): LatLng? {
        val match = pattern.find(input.trim()) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lng = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat < -90.0 || lat > 90.0) return null
        if (lng < -180.0 || lng > 180.0) return null
        return LatLng(lat, lng)
    }

    /** Display form. Always LTR digits, so it is wrapped by the caller when needed. */
    fun format(location: LatLng): String =
        "%.5f, %.5f".format(java.util.Locale.US, location.lat, location.lng)
}
