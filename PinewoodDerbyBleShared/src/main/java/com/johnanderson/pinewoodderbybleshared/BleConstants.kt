package com.johnanderson.pinewoodderbybleshared

import java.util.*

class BleConstants {
    companion object {
        //Mandatory if notify property is set
        val CLIENT_CONFIG_DESCRIPTOR: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        val USER_DESCRIPTOR: UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb")
    }
}