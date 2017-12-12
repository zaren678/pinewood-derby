package com.johnanderson.pinewoodderbyiot

import android.app.Activity
import android.os.Bundle
import com.johnanderson.pinewoodderbyiot.bluetooth.BleServer
import com.johnanderson.pinewoodderbyiot.bluetooth.MotorBleServer
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : DaggerAppCompatActivity() {

    @Inject lateinit var mBleServer: MotorBleServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBleServer.startAdvertising()
        mBleServer.startServer()
    }

    override fun onDestroy() {
        super.onDestroy()
        mBleServer.close()
    }
}
