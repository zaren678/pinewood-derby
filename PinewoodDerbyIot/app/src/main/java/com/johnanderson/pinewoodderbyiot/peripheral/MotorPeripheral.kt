package com.johnanderson.pinewoodderbyiot.peripheral

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManagerService
import com.johnanderson.pinewoodderbybleshared.models.MotorState
import com.johnanderson.pinewoodderbyiot.board.BoardDefaults
import com.johnanderson.pinewoodderbyiot.repo.MotorStateRepo
import timber.log.Timber
import javax.inject.Inject

class MotorPeripheral @Inject constructor(
    mPeripheralService:PeripheralManagerService,
    private val mMotorStateRepo: MotorStateRepo,
    mBoardDefaults: BoardDefaults
): AutoCloseable {

    private val mLedGpio:Gpio?
    private var mLastMotorState:MotorState? = null
    private val mChangeListener:()->Unit = {
        updateToMotorState(mMotorStateRepo.getMotorState())
    }

    init {

        val state = mMotorStateRepo.getMotorState()
        updateToMotorState(state)
        mMotorStateRepo.addChangeListener(mChangeListener)
        mLedGpio = mPeripheralService.openGpio(mBoardDefaults.getGPIOForLED())
        mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
    }

    private fun updateToMotorState(state:MotorState) {
        Timber.d("updateToMotorState: $state, previous: $mLastMotorState")
        if (state == mLastMotorState) {
            return
        }

        if (state.test != mLastMotorState?.test &&
            state.test != null){
            setLightOn(state.test)
        }

        mLastMotorState = state
    }

    private fun setLightOn(on: Boolean) {
        Timber.d("Setting light to $on")
        mLedGpio?.value = on
    }

    override fun close() {
        mMotorStateRepo.removeChangeListener(mChangeListener)
        mLedGpio?.close()
    }
}