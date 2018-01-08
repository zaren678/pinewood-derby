package com.johnanderson.pinewoodderbyiot.board

import com.google.android.things.pio.Pwm

interface BoardDefaults {
    fun getGPIOForLED(): String

    fun getGPIOForMotor1Pwm(): String
    fun getGPIOForMotor2Pwm(): String

    fun getGPIOForMotor1Enable(): String
    fun getGPIOForMotor2Enable(): String

    fun getGPIOForMotor1Status(): String
    fun getGPIOForMotor2Status(): String

    fun getGPIOForMotor1Dir(): String
    fun getGPIOForMotor2Dir(): String
}