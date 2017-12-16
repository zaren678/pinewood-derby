package com.johnanderson.pinewoodderbyiot.board

import android.os.Build

private val DEVICE_RPI3 = "rpi3"

fun getBoardDefaults(): BoardDefaults {
    when (Build.DEVICE) {
        DEVICE_RPI3 -> return Rpi3BoardDefaults()
    }
    throw IllegalArgumentException("Device ${Build.DEVICE} isn't supported yet")
}