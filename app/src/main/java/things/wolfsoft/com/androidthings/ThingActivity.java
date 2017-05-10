package things.wolfsoft.com.androidthings;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import com.google.android.things.contrib.driver.apa102.Apa102;
import com.google.android.things.contrib.driver.bmx280.Bmx280SensorDriver;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;
import com.google.android.things.contrib.driver.pwmspeaker.Speaker;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ThingActivity extends AppCompatActivity {
    private static final String TAG = "ThingActivity";

    private Gpio ledGpio;

    private ButtonInputDriver buttonAInputDriver;
    private Button buttonB;
    private Button buttonC;

    private Apa102 ledstrip;
    private int NUM_LEDS = 7;
    private int[] mRainbow = new int[NUM_LEDS];
    private static final int LEDSTRIP_BRIGHTNESS = 1;
    private boolean rainbowOrder = true;

    private AlphanumericDisplay alphaDisplay;
    private static final float CLEAR_DISPLAY = 73638.45f;
    private enum DisplayMode {
        TEMPERATURE,
        PRESSURE
    }
    private DisplayMode displayMode = DisplayMode.TEMPERATURE;

    private Speaker speaker;
    private int SPEAKER_READY_DELAY_MS = 300;
    private boolean isSpeakerMute = false;
    private static int SOUND_HIGH = 1;

    private static final int INTERVAL_BETWEEN_BLINKS_MS = 900;
    private Handler blinkingLedHandler = new Handler();

    private Bmx280SensorDriver environmentalSensorDriver;
    private SensorManager sensorManager;
    private float lastTemperature;
    private float lastPressure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thing);

        Log.d(TAG, "Hello Android Things!");

        // Set current IP on display (need this to connect ADB)
        String currentIp = getIPAddress(true);
        TextView txt = (TextView) findViewById(R.id.text_title);
        txt.setText("Current IP address is: " + currentIp);
        Log.d(TAG, "Current IP address is: " + currentIp);

        // Initialize buttons
        // Method1 to handle key presses, using Input driver, handle Key DOWN/UP
        try {
            buttonAInputDriver = new ButtonInputDriver(BoardDefaults.getGPIOForBtnA(),
                    Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_A);
            buttonAInputDriver.register();
            Log.d(TAG, "Initialized GPIO Button that generates a keypress with KEYCODE_A");
        } catch (IOException e) {
            throw new RuntimeException("Error initializing GPIO button", e);
        }

        // Another way to register button events, simpler, but handles single press event
        try {
            buttonB = new Button(BoardDefaults.getGPIOForBtnB(),
                    Button.LogicState.PRESSED_WHEN_LOW);
            buttonB.setOnButtonEventListener(buttonCallbackB);

            buttonC = new Button(BoardDefaults.getGPIOForBtnC(),
                    Button.LogicState.PRESSED_WHEN_LOW);
            buttonC.setOnButtonEventListener(buttonCallbackC);
        } catch (IOException e) {
            Log.e(TAG, "button driver error", e);
        }

        //SPI LED Lightstrip
        //Inialize the rainbow of colors based on num of LEDs
        for (int i = 0; i < NUM_LEDS; i++) {
            float[] hsv = {i * 360.f / NUM_LEDS, 1.0f, 1.0f};
            mRainbow[i] = Color.HSVToColor(255, hsv);
        }
        try {
            ledstrip = new Apa102(BoardDefaults.getSpiBus(), Apa102.Mode.BGR);
            ledstrip.setBrightness(LEDSTRIP_BRIGHTNESS);
        } catch (IOException e) {
            ledstrip = null; // Led strip is optional.
        }

        // Alphanumeric Display
        try {
            alphaDisplay = new AlphanumericDisplay(BoardDefaults.getI2cBus());
            alphaDisplay.setEnabled(true);
            alphaDisplay.clear();
            Log.d(TAG, "Initialized I2C Display");
        } catch (IOException e) {
            Log.e(TAG, "Error initializing display", e);
            Log.d(TAG, "Display disabled");
            alphaDisplay = null;
        }


        // PWM speaker
        try {
            speaker = new Speaker(BoardDefaults.getSpeakerPwmPin());
            soundSpeaker(1);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing speaker", e);
        }

        //Initialzize Color LED
        PeripheralManagerService service = new PeripheralManagerService();
        try {
            String blueLED = BoardDefaults.getGPIOForBlueLED();
            String greenLED = BoardDefaults.getGPIOForGreenLED();
            String redLED = BoardDefaults.getGPIOForRedLED();
            ledGpio = service.openGpio(redLED);

            ledGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            blinkingLedHandler.post(blinkingRunnable);
        } catch (IOException e) {
            throw new RuntimeException("Problem connecting to IO Port", e);
        }

        // Sensor Stack
        sensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        try {
            environmentalSensorDriver = new Bmx280SensorDriver(BoardDefaults.getI2cBus());
            sensorManager.registerDynamicSensorCallback(dynamicSensorCallback);
            environmentalSensorDriver.registerTemperatureSensor();
            environmentalSensorDriver.registerPressureSensor();
            Log.d(TAG, "Initialized I2C BMP280");
        } catch (IOException e) {
            throw new RuntimeException("Error initializing BMP280", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (buttonAInputDriver != null) {
            try {
                buttonAInputDriver.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            buttonAInputDriver = null;
        }

        if (buttonB != null) {
            // TODO
        }


        blinkingLedHandler.removeCallbacks(blinkingRunnable);



        try {
            ledGpio.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }

        if (alphaDisplay != null) {
            try {
                alphaDisplay.clear();
                alphaDisplay.setEnabled(false);
                alphaDisplay.close();
            } catch (IOException e) {
                Log.e(TAG, "Error disabling display", e);
            } finally {
                alphaDisplay = null;
            }
        }

        try {
            if (ledstrip != null) {
                try {
                    ledstrip.write(new int[NUM_LEDS]);
                    ledstrip.setBrightness(0);
                    ledstrip.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error disabling ledstrip", e);
                } finally {
                    ledstrip = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error on closing LED strip", e);
        }

        // Clean up sensor registrations
        sensorManager.unregisterListener(temperatureListener);
        sensorManager.unregisterListener(pressureListener);
        sensorManager.unregisterDynamicSensorCallback(dynamicSensorCallback);

        // Clean up peripheral.
        if (environmentalSensorDriver != null) {
            try {
                environmentalSensorDriver.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            environmentalSensorDriver = null;
        }
    }

    // --- BEGIN BUTTON SETUP
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            Log.d(TAG, "The button A event was received KEY DOWN");
            displayMode = DisplayMode.TEMPERATURE;
            int[] colors = new int[NUM_LEDS];
            // Switches the rainbow from left to right on each press
            if (rainbowOrder) {
                rainbowOrder = false;
                colors[0] = mRainbow[6];
                colors[1] = mRainbow[5];
                colors[2] = mRainbow[4];
                colors[3] = mRainbow[3];
                colors[4] = mRainbow[2];
                colors[5] = mRainbow[1];
                colors[6] = mRainbow[0];
            } else {
                rainbowOrder = true;
                colors[0] = mRainbow[0];
                colors[1] = mRainbow[1];
                colors[2] = mRainbow[2];
                colors[3] = mRainbow[3];
                colors[4] = mRainbow[4];
                colors[5] = mRainbow[5];
                colors[6] = mRainbow[6];
            }
            soundSpeaker(1);
            runLedStrip(colors);

            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            Log.d(TAG, "The button A event was received KEY UP");
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Callback for buttonB events.
     */
    private Button.OnButtonEventListener buttonCallbackB =
            new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean pressed) {
                    if (pressed) {
                        displayMode = DisplayMode.PRESSURE;
                        Log.d(TAG, "button B pressed");
                        Random rand = new Random();
                        int[] colors = new int[NUM_LEDS];
                        colors[0] = mRainbow[rand.nextInt(NUM_LEDS)];
                        colors[1] = mRainbow[rand.nextInt(NUM_LEDS)];
                        colors[2] = mRainbow[rand.nextInt(NUM_LEDS)];
                        colors[3] = mRainbow[rand.nextInt(NUM_LEDS)];
                        colors[4] = mRainbow[rand.nextInt(NUM_LEDS)];
                        colors[5] = mRainbow[rand.nextInt(NUM_LEDS)];
                        colors[6] = mRainbow[rand.nextInt(NUM_LEDS)];

                        soundSpeaker(4);
                        runLedStrip(colors);
                    }
                }
            };

    /**
     * Callback for buttonC events.
     */
    private Button.OnButtonEventListener buttonCallbackC =
            new Button.OnButtonEventListener() {
                @Override
                public void onButtonEvent(Button button, boolean pressed) {
                    if (pressed) {
                        Log.d(TAG, "button C pressed");
                        updateDisplay(CLEAR_DISPLAY);
                        soundSpeaker(8);
                        clearLedStrip();
                    }
                }
            };
    // --- END BUTTON SETUP

    private void soundSpeaker(int soundType) {
        if (!isSpeakerMute) {
            int soundVal = soundType * 100;

            final ValueAnimator slide = ValueAnimator.ofFloat(soundVal, 440 * 4);

            slide.setDuration(50);
            slide.setRepeatCount(5);
            slide.setInterpolator(new LinearInterpolator());
            slide.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    try {
                        float v = (float) animation.getAnimatedValue();
                        speaker.play(v);
                    } catch (IOException e) {
                        throw new RuntimeException("Error sliding speaker", e);
                    }
                }
            });
            slide.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    try {
                        speaker.stop();
                    } catch (IOException e) {
                        throw new RuntimeException("Error sliding speaker", e);
                    }
                }
            });
            Handler handler = new Handler(getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    slide.start();
                }
            }, SPEAKER_READY_DELAY_MS);
        }
    }

    private Runnable blinkingRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                ledGpio.setValue(!ledGpio.getValue());
                blinkingLedHandler.postDelayed(blinkingRunnable, INTERVAL_BETWEEN_BLINKS_MS);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    private void runLedStrip(int[] colors) {
        try {
            ledstrip.write(colors);
            ledstrip.setBrightness(LEDSTRIP_BRIGHTNESS);
        } catch (IOException e) {
            Log.e(TAG, "Error setting ledstrip", e);
        }
    }

    private void clearLedStrip() {
        try {
            ledstrip.write(new int[NUM_LEDS]);
            ledstrip.setBrightness(0);
        } catch (IOException e) {
            Log.e(TAG, "Error setting ledstrip", e);
        }
    }

    private void updateDisplay(float value) {
        if (alphaDisplay != null) {
            try {
                if (value == CLEAR_DISPLAY) {
                    alphaDisplay.clear();
                } else {
                    alphaDisplay.display(value);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error setting display", e);
            }
        }
    }




    /**
     * A utility method to return current IP
     *
     * @param useIPv4
     * @return
     */
    private static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Exception received: " + ex, ex);
        }
        return "";
    }

    // Callback used when we register the BMP280 sensor driver with the system's SensorManager.
    private SensorManager.DynamicSensorCallback dynamicSensorCallback
            = new SensorManager.DynamicSensorCallback() {
        @Override
        public void onDynamicSensorConnected(Sensor sensor) {
            if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                Log.d(TAG, "Ambient temp sensor connected and is receiving temperature data");
                sensorManager.registerListener(temperatureListener, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL);


            } else if (sensor.getType() == Sensor.TYPE_PRESSURE) {
                Log.d(TAG, "Pressure sensor connected and is receiving temperature data");
                sensorManager.registerListener(pressureListener, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        @Override
        public void onDynamicSensorDisconnected(Sensor sensor) {
            super.onDynamicSensorDisconnected(sensor);
        }
    };

    // Callback when SensorManager delivers temperature data.
    private SensorEventListener temperatureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            lastTemperature = event.values[0];
//            Log.d(TAG, "Temperature sensor changed: " + lastTemperature);
            if (displayMode == DisplayMode.TEMPERATURE) {
                updateDisplay(lastTemperature);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "Temp accuracy changed: " + accuracy);
        }
    };

    // Callback when SensorManager delivers pressure data.
    private SensorEventListener pressureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            lastPressure = event.values[0];
//            Log.d(TAG, "Pressure sensor changed: " + lastPressure);
            if (displayMode == DisplayMode.PRESSURE) {
                updateDisplay(lastPressure);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "Pressure accuracy changed: " + accuracy);
        }
    };




}
