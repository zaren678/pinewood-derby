package com.johnanderson.pinewoodderbyiot.peripheral

import android.system.OsConstants
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.GpioCallback
import com.google.android.things.pio.PeripheralManagerService
import com.google.android.things.pio.Pwm
import com.johnanderson.pinewoodderbybleshared.models.MotorState
import com.johnanderson.pinewoodderbyiot.board.BoardDefaults
import com.johnanderson.pinewoodderbyiot.repo.MotorStateRepo
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs

class MotorPeripheral @Inject constructor(
    mPeripheralService:PeripheralManagerService,
    private val mMotorStateRepo: MotorStateRepo,
    mBoardDefaults: BoardDefaults
): AutoCloseable {

    private val PWM_FREQUENCY:Double = 20000.0
    private val MIN_DUTY_CYCLE_PERCENT:Double = 0.0
    private val MAX_DUTY_CYCLE_PERCENT:Double = 100.0

    private val mLedGpio:Gpio
    private var mMotorEnableGpio: Gpio
    private var mMotorDirectionGpio: Gpio
    private var mMotorStatusGpio: Gpio
    private var mMotorPwm: Pwm

    private var mCurrentDutyCycle:Double = 0.0
        set(value) {
            val newValue = when {
                value > MAX_DUTY_CYCLE_PERCENT -> MAX_DUTY_CYCLE_PERCENT
                value < MIN_DUTY_CYCLE_PERCENT -> MIN_DUTY_CYCLE_PERCENT
                else -> value
            }
            Timber.d("1Set duty cycle: $value")
            if (field != newValue) {
                field = newValue
                Timber.d("2Set duty cycle: $field")
                mMotorPwm.setPwmDutyCycle(field)
            }
        }

    private var mCurrentDirection = false
        set(value) {
            field = value
            mMotorDirectionGpio.value = value
        }

    private var mLastMotorState:MotorState? = null
    private val mChangeListener:()->Unit = {
        updateToMotorState(mMotorStateRepo.getMotorState())
    }

    init {
        mMotorStateRepo.addChangeListener(mChangeListener)
        mLedGpio = mPeripheralService.openGpio(mBoardDefaults.getGPIOForLED())
        mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        mMotorEnableGpio = mPeripheralService.openGpio(mBoardDefaults.getGPIOForMotor2Enable())
        mMotorEnableGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        mMotorEnableGpio.value = true

        mMotorDirectionGpio = mPeripheralService.openGpio(mBoardDefaults.getGPIOForMotor2Dir())
        mMotorDirectionGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        mMotorDirectionGpio.value = mCurrentDirection

        mMotorStatusGpio = mPeripheralService.openGpio(mBoardDefaults.getGPIOForMotor2Status())
        mMotorStatusGpio.setDirection(Gpio.DIRECTION_IN)
        mMotorStatusGpio.registerGpioCallback(object:GpioCallback() {
            override fun onGpioError(gpio: Gpio?, error: Int) {
                Timber.e("Gpio Error: $error, ${OsConstants.errnoName(error)}")
            }

            override fun onGpioEdge(gpio: Gpio?): Boolean {
                Timber.d("Motor status edge: ${gpio?.value}")
                return true
            }
        })

        mMotorPwm = mPeripheralService.openPwm(mBoardDefaults.getGPIOForMotor2Pwm())
        mMotorPwm.setPwmFrequencyHz(PWM_FREQUENCY)
        mMotorPwm.setPwmDutyCycle(mCurrentDutyCycle)
        mMotorPwm.setEnabled(true)

        val state = mMotorStateRepo.getMotorState()
        updateToMotorState(state)
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

        if (state.speed != mLastMotorState?.speed &&
            state.speed != null &&
            state.test == false) {
            setSpeed(state.speed)
        }

        mLastMotorState = state
    }

    private fun setSpeed(speed: Double) {
        mCurrentDirection = speed >= 0.0
        mCurrentDutyCycle = abs(speed)
    }

    private fun setLightOn(on: Boolean) {
        Timber.d("Setting light to $on")
        mLedGpio.value = on
    }

    fun stopMotor() {
        mCurrentDutyCycle = 0.0
    }

    override fun close() {
        mMotorStateRepo.removeChangeListener(mChangeListener)
        mLedGpio.close()
        mMotorStatusGpio.close()
        mMotorDirectionGpio.close()

        mMotorEnableGpio.value = false
        mMotorEnableGpio.close()

        stopMotor()
        mMotorPwm.setEnabled(false)
        mMotorPwm.close()
    }
}