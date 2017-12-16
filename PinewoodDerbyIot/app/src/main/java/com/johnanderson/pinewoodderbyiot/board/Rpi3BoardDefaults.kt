package com.johnanderson.pinewoodderbyiot.board

class Rpi3BoardDefaults : BoardDefaults {
    override fun getGPIOForLED(): String {
        return "BCM4"
    }
}