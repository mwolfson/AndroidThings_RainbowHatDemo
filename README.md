

Rainbow HAT for Android Things Demo
===================================

This (Android Things)[https://developers.android.com/things] project executes all the hardware that is on the (Pimoroni Rainbow HAT)[https://shop.pimoroni.com/products/rainbow-hat-for-android-things].  This is a add-on board for the Raspberry Pi, and is designed to help with introduction to developing with Android Things.

![Screen Flow Demo](https://github.com/mwolfson/AndroidThings_RainbowHatDemo/blob/master/art/atdemo.gif)

Project Explanation
-------------------

The project is designed to demonstrate the specific protocols available on the Raspberry Pi, including:
* (SPI) - the APA102 LEDs
* (12C) - the BPM280 sensor and Alphanumeric displays
* (GPIO) - the capacitive touch buttons and LEDs
* (PWM) - the piezo buzzer

How the demo works
------------------

_*Button A*_
* (SPI) - LEDs Rainbow color switches from left/tight
* (12C) - Temperature displayed from sensor readings 
* (GPIO) - Button press registered, and RED LED turned on
* (PWM) - Buzzer makes certain sound

_*Button B*_
* (SPI) - LEDs Rainbow color switches between random colors
* (12C) - Barometric Indicator (LOW/MED/HIGH) displayed
* (GPIO) - Button press registered, and GREEN LED turned on
* (PWM) - Buzzer makes certain sound

_*Button B*_  
* (SPI) - LEDs display clear
* (12C) - Alphanumeric display clear
* (GPIO) - Button press registered, and BLUE LED turned on
* (PWM) - Buzzer makes certain sound