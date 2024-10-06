package com.leocatto.olcapp.presentation

class OlcAppState {
    var fullLocationCode: String? = null
        set(value) {
            // Don't allow un-setting, if already set.
            if (value == null) return

            field = value
            regionPart = value.substring(0, 4)
            areaPart = value.substring(4, 8)
            plusPart = value.substring(8, 11)
        }

    var regionPart = "XXXX"
    var areaPart = "YYYY"
    var plusPart = "+ZZ"
}