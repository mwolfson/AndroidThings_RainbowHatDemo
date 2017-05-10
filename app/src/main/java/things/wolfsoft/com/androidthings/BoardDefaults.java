package things.wolfsoft.com.androidthings;
import android.os.Build;

import com.google.android.things.pio.PeripheralManagerService;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class BoardDefaults {



    private static final String DEVICE_EDISON_ARDUINO = "edison_arduino";
    private static final String DEVICE_EDISON = "edison";
    private static final String DEVICE_JOULE = "joule";
    private static final String DEVICE_RPI3 = "rpi3";
    private static final String DEVICE_PICO = "imx6ul_pico";
    private static final String DEVICE_VVDN = "imx6ul_iopb";
    private static final String DEVICE_NXP = "imx6ul";
    private static String sBoardVariant = "";

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    public static String getGPIOForRedLED() {
        switch (getBoardVariant()) {
            case DEVICE_EDISON_ARDUINO:
                return "IO13";
            case DEVICE_EDISON:
                return "GP45";
            case DEVICE_RPI3:
                return "BCM6";
            case DEVICE_NXP:
                return "GPIO4_IO20";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    public static String getGPIOForGreenLED() {
        switch (getBoardVariant()) {
            case DEVICE_EDISON_ARDUINO:
                return "IO13";
            case DEVICE_EDISON:
                return "GP45";
            case DEVICE_RPI3:
                return "BCM19";
            case DEVICE_NXP:
                return "GPIO4_IO20";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    public static String getGPIOForBlueLED() {
        switch (getBoardVariant()) {
            case DEVICE_EDISON_ARDUINO:
                return "IO13";
            case DEVICE_EDISON:
                return "GP45";
            case DEVICE_RPI3:
                return "BCM26";
            case DEVICE_NXP:
                return "GPIO4_IO20";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    public static String getGPIOForBtnA() {
        switch (getBoardVariant()) {
            case DEVICE_EDISON_ARDUINO:
                return "IO13";
            case DEVICE_EDISON:
                return "GP45";
            case DEVICE_RPI3:
                return "BCM21";
            case DEVICE_NXP:
                return "GPIO4_IO20";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    public static String getGPIOForBtnB() {
        switch (getBoardVariant()) {
            case DEVICE_EDISON_ARDUINO:
                return "IO13";
            case DEVICE_EDISON:
                return "GP45";
            case DEVICE_RPI3:
                return "BCM20";
            case DEVICE_NXP:
                return "GPIO4_IO20";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    public static String getGPIOForBtnC() {
        switch (getBoardVariant()) {
            case DEVICE_EDISON_ARDUINO:
                return "IO13";
            case DEVICE_EDISON:
                return "GP45";
            case DEVICE_RPI3:
                return "BCM16";
            case DEVICE_NXP:
                return "GPIO4_IO20";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    public static String getI2cBus() {
        switch (getBoardVariant()) {
            case DEVICE_EDISON_ARDUINO:
                return "I2C6";
            case DEVICE_EDISON:
                return "I2C1";
            case DEVICE_JOULE:
                return "I2C0";
            case DEVICE_RPI3:
                return "I2C1";
            case DEVICE_PICO:
                return "I2C2";
            case DEVICE_VVDN:
                return "I2C4";
            default:
                throw new IllegalArgumentException("Unknown device: " + Build.DEVICE);
        }
    }

    public static String getSpiBus() {
        switch (getBoardVariant()) {
            case DEVICE_EDISON_ARDUINO:
                return "SPI1";
            case DEVICE_EDISON:
                return "SPI2";
            case DEVICE_JOULE:
                return "SPI0.0";
            case DEVICE_RPI3:
                return "SPI0.0";
            case DEVICE_PICO:
                return "SPI3.0";
            case DEVICE_VVDN:
                return "SPI1.0";
            default:
                throw new IllegalArgumentException("Unknown device: " + Build.DEVICE);
        }
    }

    public static String getSpeakerPwmPin() {
        switch (getBoardVariant()) {
            case DEVICE_EDISON_ARDUINO:
                return "IO3";
            case DEVICE_EDISON:
                return "GP13";
            case DEVICE_JOULE:
                return "PWM_0";
            case DEVICE_RPI3:
                return "PWM1";
            case DEVICE_PICO:
                return "PWM7";
            case DEVICE_VVDN:
                return "PWM3";
            default:
                throw new IllegalArgumentException("Unknown device: " + Build.DEVICE);
        }
    }

    private static String getBoardVariant() {
        if (!sBoardVariant.isEmpty()) {
            return sBoardVariant;
        }
        sBoardVariant = Build.DEVICE;
        // For the edison check the pin prefix
        // to always return Edison Breakout pin name when applicable.
        if (sBoardVariant.equals(DEVICE_EDISON)) {
            PeripheralManagerService pioService = new PeripheralManagerService();
            List<String> gpioList = pioService.getGpioList();
            if (gpioList.size() != 0) {
                String pin = gpioList.get(0);
                if (pin.startsWith("IO")) {
                    sBoardVariant = DEVICE_EDISON_ARDUINO;
                }
            }
        }
        return sBoardVariant;
    }



}