KineSutra 
=============
*Motion capture + Haptic feedback*


This project combines a kinect-based motion capture system to model the movements of a choreographer. Then, the dance student dons a vibrational haptic suit and attempts to recapitulate those moves. If she deviates from the choreographed poses, the suit sends a message to her limbs via vibration, allowing her to correct her stance!


Technology
-----------
* Kinect / Asus XTion sensor
* Processing with SimpleOpenNI library
* Serial communication to Arduino via usb or bluetooth
* 20 vibration motors

The haptic suit
-----------------------------------------------
![Making the suit](https://raw.github.com/CodeStrumpet/KineSutra/master/Images/README_images/kinesutra_making_suit.jpeg "Making the suit")

Here the UI is telling the dancer to correct the position of their left elbow
---------------------------------------------------
![Left elbow correct](https://raw.github.com/CodeStrumpet/KineSutra/master/Images/README_images/kinesutra_elbow_correct.png "Correct left elbow")

Left knee idendified as out of position
---------------------------------------------------
![Left knee correct](https://raw.github.com/CodeStrumpet/KineSutra/master/Images/README_images/kinesutra_knee_correct.png "Correct left knee")



Steps to get the code running
------------------------------
* Download the *SimpleOpenNI & ControlP5* processing libraries and place them in the 'libraries' directory in your sketchbook folder
* Restart the Processing environment to make sure the libraries will be recognized
* Plug in a Kinect or Asus Xtion sensor
* Capture a reference pose using the 'r' key on the keyboard
* Have your dancer don the haptic suit
* Get buzzed!