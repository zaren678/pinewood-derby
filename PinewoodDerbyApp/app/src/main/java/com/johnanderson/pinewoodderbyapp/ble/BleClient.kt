package com.johnanderson.pinewoodderbyapp.ble

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Bundle
import com.google.common.util.concurrent.ListenableFuture

interface BleClient {
    fun connect(address: String): ListenableFuture<Bundle>
    fun readCharacteristic(char: BluetoothGattCharacteristic): ListenableFuture<Bundle>
    //fun writeCharacteristic(uuid: UUID, data: byte[]): ListenableFuture<Void>
    fun getSupportedGattServices(): List<BluetoothGattService>?
}