package com.johnanderson.pinewoodderbyiot.board

class Rpi3BoardDefaults : BoardDefaults {

    override fun getGPIOForMotor1Pwm(): String {
        return "PWM0"
    }

    override fun getGPIOForMotor2Pwm(): String {
        return "PWM1"
    }

    override fun getGPIOForMotor1Enable(): String {
        return "BCM22"
    }

    override fun getGPIOForMotor2Enable(): String {
        return "BCM23"
    }

    override fun getGPIOForMotor1Status(): String {
        return "BCM5"
    }

    override fun getGPIOForMotor2Status(): String {
        return "BCM6"
    }

    override fun getGPIOForMotor1Dir(): String {
        return "BCM24"
    }

    override fun getGPIOForMotor2Dir(): String {
        return "BCM25"
    }

    override fun getGPIOForLED(): String {
        return "BCM4"
    }
}