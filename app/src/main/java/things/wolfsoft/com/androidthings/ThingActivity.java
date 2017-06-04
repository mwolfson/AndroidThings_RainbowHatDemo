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
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

public class ThingActivity extends AppCompatActivity {
    private static final String TAG = "ThingActivity";

    private ButtonInputDriver buttonAInputDriver;
    private Button buttonB;
    private Button buttonC;

    private Gpio ledGpioRed;
    private Gpio ledGpioBlue;
    private Gpio ledGpioGreen;
    private static int RED_LED = 1;
    private static int BLUE_LED = 2;
    private static int GREEN_LED = 3;

    private Apa102 ledstrip;
    private int NUM_LEDS = 7;
    private int[] mRainbow = new int[NUM_LEDS];
    private static final int LEDSTRIP_BRIGHTNESS = 1;
    private boolean rainbowOrder = true;

    private AlphanumericDisplay alphaDisplay;
    private static final float CLEAR_DISPLAY = 73638.45f;
    private String message;
    private Stack<Character> charStack = new Stack();
    private Handler handler;
    private int counter = 0;

    private int[] colors = new int[NUM_LEDS];
    private Runnable runnable;

    private enum DisplayMode {
        TEMPERATURE,
        PRESSURE,
        CLEAR,
        MESSAGE
    }

    private DisplayMode displayMode = DisplayMode.TEMPERATURE;
    private boolean useFarenheit = true;

    private Speaker speaker;
    private int SPEAKER_READY_DELAY_MS = 300;
    private boolean isSpeakerMute = false;
    private static int SOUND_LOW = 1;
    private static int SOUND_MED = 4;
    private static int SOUND_HIGH = 8;

    private Bmx280SensorDriver environmentalSensorDriver;
    private SensorManager sensorManager;
    private float lastTemperature;
    private float lastPressure;
    private static final float BAROMETER_RANGE_LOW = 965.f;
    private static final float BAROMETER_RANGE_HIGH = 1035.f;

    private TextView titleTxt;
    private TextView tempTxt;
    private TextView pressureTxt;

    private CircularLinkedList<Character> linkedList = new CircularLinkedList<>();
    DatabaseReference ref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thing);

        handler = new Handler();

        Log.d(TAG, "Hello Android Things!");
        titleTxt = (TextView) findViewById(R.id.text_title);
        tempTxt = (TextView) findViewById(R.id.text_temperature);
        pressureTxt = (TextView) findViewById(R.id.text_pressure);

        FirebaseApp.initializeApp(getApplicationContext());
        ref = FirebaseDatabase.getInstance().getReference();

        ref.child("message").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                linkedList = new CircularLinkedList<>();
                handler.removeCallbacks(runnable);
                displayMode = DisplayMode.MESSAGE;
                message = (String) dataSnapshot.getValue();
                if (message.length() > 4) {
                    message = message + "   ";
                    createLinkedList();
                    scrollMarquee();
                } else {
                    updateDisplay(message);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        listenForColor();

        // Set current IP on display (need this to connect ADB)
        String currentIp = getIPAddress(true);

        titleTxt.setText("Current IP (time started):\n    " + currentIp + "\n    " + Utilities.getDate());
        Log.d(TAG, "Current IP address is: " + currentIp);

        // Initialize buttons
        // Method1 to handle key presses, using Input driver, handle Key DOWN/UP
        try {
            buttonAInputDriver = new ButtonInputDriver(BoardDefaults.getGPIOForBtnA(),
                    Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_A);
            buttonAInputDriver.register();
            Log.d(TAG, "Button A registered, will generate KEYCODE_A");
        } catch (IOException e) {
            throw new RuntimeException("Error initializing GPIO button A", e);
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

        //GPIO Individual Color LED
        try {
            PeripheralManagerService service = new PeripheralManagerService();
            ledGpioRed = service.openGpio(BoardDefaults.getGPIOForRedLED());
            ledGpioGreen = service.openGpio(BoardDefaults.getGPIOForGreenLED());
            ledGpioBlue = service.openGpio(BoardDefaults.getGPIOForBlueLED());
        } catch (IOException e) {
            throw new RuntimeException("Problem connecting to IO Port", e);
        }

        //SPI LED Lightstrip and rainbow color array
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
            Log.d(TAG, "Initialized PWM speaker");
        } catch (IOException e) {
            throw new RuntimeException("Error initializing PWM speaker", e);
        }

        // I2C Sensors - Temperature and Pressure
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

    private void listenForColor() {
        ref.child("mood-color").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String color = (String) dataSnapshot.getValue();
                System.out.println(color);


                for (int i = 0; i < 7; i++) {
                    colors[i] = Color.parseColor(color);
                }

                runLEDStrip(colors);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void runLEDStrip(int[] colors) {
        try {
            ledstrip.write(colors);
            ledstrip.setBrightness(LEDSTRIP_BRIGHTNESS);
        } catch (IOException e) {
            Log.e(TAG, "Error setting ledstrip", e);
        }
    }

    private void createLinkedList() {
        for (int i=0; i < message.length(); i++){
            linkedList.addNodeAtEnd(message.charAt(i));
        }
    }

    private void scrollMarquee() {
        updateDisplay(message);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                updateDisplay(newMessage(message));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(runnable);
    }

    private String newMessage(String message) {
        StringBuilder fourChars = new StringBuilder();
        Node node = linkedList.getNode(counter);

        for (int i=0; i<3; i++){
            fourChars.append(node.data);
            node = node.next;
        }
        fourChars.append(node.data);

        if ((counter++) == message.length()){
         counter = -1;
        }
        counter++;
        Log.d("Chars: ", fourChars.toString());
        return fourChars.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Button
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

        // GPIO LEDS
        try {
            ledGpioRed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            ledGpioBlue.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            ledGpioGreen.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            ledGpioRed.close();
            ledGpioBlue.close();
            ledGpioGreen.close();
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }

        // LED Lightstrip
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

        // Alphanumeric Display
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
            soundSpeaker(SOUND_LOW);
            runLedStrip(colors);
            showLED(RED_LED);

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

                        soundSpeaker(SOUND_MED);
                        runLedStrip(colors);
                        showLED(GREEN_LED);
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
                        displayMode = DisplayMode.CLEAR;
                        updateDisplay(CLEAR_DISPLAY);
                        soundSpeaker(SOUND_HIGH);
                        clearLedStrip();
                        showLED(BLUE_LED);
                    }
                }
            };

    /**
     * Helper Method to turn one of 3 LEDs, and turn off the others
     *
     * @param ledType
     */
    private void showLED(int ledType) {
        try {
            ledGpioRed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            ledGpioBlue.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            ledGpioGreen.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            switch (ledType) {
                case 1:
                    ledGpioRed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                    break;
                case 2:
                    ledGpioBlue.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                    break;
                case 3:
                    ledGpioGreen.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                    break;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    private void updateDisplay(float value) {
        updateDisplay(value, "");
    }

    private void updateDisplay(String message) {
        updateDisplay(0, message);
    }

    private void updateDisplay(float value, String message) {
        if (alphaDisplay != null) {
            try {
                if (displayMode == DisplayMode.PRESSURE) {
                    if (value > BAROMETER_RANGE_HIGH) {
                        alphaDisplay.display("HIGH");
                    } else if (value < BAROMETER_RANGE_LOW) {
                        alphaDisplay.display("LOW");
                    } else {
                        alphaDisplay.display("MED");
                    }
                } else if (displayMode == DisplayMode.CLEAR) {
                    alphaDisplay.clear();
                } else if (displayMode == DisplayMode.MESSAGE) {
                    alphaDisplay.display(message);
                } else {
                    alphaDisplay.display(value);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error setting display", e);
            }
        }
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
            if (useFarenheit) {
                tempTxt.setText("Current Temperature in Farenheit (time reported):\n    " + Utilities.convertCelciusToFahrenheit(lastTemperature) + "\n    " + Utilities.getDate());
            } else {
                tempTxt.setText("Current Temperature in Celcius (time reported):\n    " + lastTemperature + "\n    " + Utilities.getDate());
            }


            if (displayMode == DisplayMode.TEMPERATURE) {
                updateDisplay(Utilities.convertCelciusToFahrenheit(lastTemperature));
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
            pressureTxt.setText("Barometric Pressure in hectoPascals (time reported):\n    " + lastPressure / 100 + "\n    " + Utilities
                    .getDate());
            if (displayMode == DisplayMode.PRESSURE) {

                updateDisplay(lastPressure);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "Pressure accuracy changed: " + accuracy);
        }
    };

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
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Exception received getting IP info: " + ex, ex);
        }
        return "NO IP ADDRESS FOUND";
    }


}
