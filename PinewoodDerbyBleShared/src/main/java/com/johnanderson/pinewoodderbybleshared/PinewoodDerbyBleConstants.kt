package com.johnanderson.pinewoodderbybleshared

import java.util.UUID

class PinewoodDerbyBleConstants {
    companion object {
        val SERVICE_ID: UUID = UUID.fromString("69036244-29E2-4440-9398-C711E75ECA69")
        val MOTOR_STATE_CHARACTERISTIC: UUID = UUID.fromString("75FFE7BD-A035-4CF4-AF1D-A11763D2D25A")

        //Mandatory if notify property is set
        val CLIENT_CONFIG_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        val USER_DESCRIPTOR: UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb")
    }
}