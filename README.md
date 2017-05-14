#### AndroidThings_RainbowHatDemo
This Android Things project executes all the hardware that is on the Pimoroni Rainbow HAT for Android Things and Raspberry Pi 3

###__Project Explanation__

The project is designed to demonstrate the specific protocols available on the Raspberry Pi, including:
* SPI - the APA102 LEDs
* 12C - the BPM280 sensor and Alphanumeric displays
* GPIO - the capacitive touch buttons and LEDs
* PWM - the piezo buzzer

### __How the demo works__

**Button A**
* (SPI) - LEDs Rainbow color switches from left/tight
* (12C) - Temperature displayed from sensor readings 
* (GPIO) - Button press registered, and RED LED turned on
* (PWM) - Buzzer makes certain sound

**Button B**
* (SPI) - LEDs Rainbow color switches between random colors
* (12C) - Barometric Indicator (LOW/MED/HIGH) displayed
* (GPIO) - Button press registered, and GREEN LED turned on
* (PWM) - Buzzer makes certain sound

**Button B**  
* (SPI) - LEDs display clear
* (12C) - Alphanumeric display clear
* (GPIO) - Button press registered, and BLUE LED turned on
* (PWM) - Buzzer makes certain sound