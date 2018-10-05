# Make Lazy Cheap Robots

* With grab-bag of modern technology (Old smartphone or Raspberry pi)
* That includes all the basics (network, sensors, CPU) out of the box
* With an easy learning curve. 

A typical smartphone includes a screen, battery, GPS, WiFi, Bluetooth, camera, gyroscope, GPU, and storage all in one!


## Stack

1. Web UI hosted on [Firebase free tier](https://firebase.google.com/pricing/)
    1. Material Design Icons that use
    2. JavaScript
    2. to change values in a Firebase DB
2. Robot thin client
    1. Java (Kotlin)
    2. Reading control values from the same Firebase DB
    3. Logging sensors to the DB
    4. Sending signals to pi4j
    5. Connecting to i2c devices
    6. Through a PiCon Zero motor shield
3. Powering cheap/scavenged DC Motors
    1. Driving TPU printed wheels

## Config

* https://console.firebase.google.com/project/nextbot0/overview
* https://console.firebase.google.com/project/nextbot0/settings/serviceaccounts/adminsdk
* Service Account: firebase-adminsdk-f0r8m@nextbot0.iam.gserviceaccount.com

## TODO

* log of all changes (both in and out)
* http://pi4j.com/example/control.html
* https://github.com/Pi4J/pi4j/blob/master/pi4j-core/src/main/java/com/pi4j/io/i2c/impl/I2CDeviceImpl.java
* http://pi4j.com/usage.html
* https://archive.org/download/C.h.i.p.FlashCollection
* https://yoursunny.com/t/2018/CHIP-flash-offline/
