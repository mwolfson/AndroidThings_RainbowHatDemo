Android Things - Rainbow HAT "Demo Everything" Project
===================================

This [Android Things](https://developers.android.com/things) project executes all the hardware that is on the [Pimoroni Rainbow HAT](https://shop.pimoroni.com/products/rainbow-hat-for-android-things).  This is a add-on board for the Raspberry Pi, and is designed to help with introduction to developing with Android Things.

![Screen Flow Demo](https://github.com/mwolfson/AndroidThings_RainbowHatDemo/blob/master/art/atdemo.gif)

Project Explanation
-------------------

The project is designed to demonstrate the specific protocols available on the Raspberry Pi, including:
* (SPI) - the APA102 LEDs
* (I2C) - the BPM280 sensor and Alphanumeric displays
* (GPIO) - the capacitive touch buttons and LEDs
* (PWM) - the piezo buzzer

How the demo works
------------------

_*Button A*_
* (SPI) - LEDs Rainbow color switches from left/tight
* (I2C) - Temperature displayed from sensor readings 
* (GPIO) - Button press registered, and RED LED turned on
* (PWM) - Buzzer makes certain sound

_*Button B*_
* (SPI) - LEDs Rainbow color switches between random colors
* (I2C) - Barometric Indicator (LOW/MED/HIGH) displayed
* (GPIO) - Button press registered, and GREEN LED turned on
* (PWM) - Buzzer makes certain sound

_*Button C*_  
* (SPI) - LEDs display clear
* (I2C) - Alphanumeric display clear
* (GPIO) - Button press registered, and BLUE LED turned on
* (PWM) - Buzzer makes certain sound

_*HDMI Display*_ (optional)
* Current IP Assigned (time powered on)
* Current Temperature (time reported)
* Current Barometric Pressure (time reported)

About this project
------------------

This application will demonstrate all the hardware on the Rainbow HAT device in a single app.  

I used the [individual contrib drivers](https://github.com/androidthings/contrib-drivers) for this, but could have used the [Rainbow Hat Contrib Driver](https://github.com/androidthings/contrib-drivers/tree/master/rainbowhat) if I wanted.  This application should run on _*all supported*_ Android Things hardware (including the Intel Edison or Joule for example), but hasn't been tested on any devices other then the Rasberry Pi 3.

I made extensive use of the drivers and examples on the [Official Android Things Gitbub](https://github.com/androidthings) site - it is a great resource.

License
-------

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
