MyMonitor
=========

A simple monitoring program / framework for things around your home.
![Screen shot](https://raw.githubusercontent.com/avatar42/MyMonitor/master/SampleImage.jpg)

Monitoring is the most often neglected part of home automation. This project aims to help fix that.

In basic terms this is a program / framework made up of:
* A simple status display with error details available by clicking on the status button for a device.
* [Several classes of checkers](https://github.com/avatar42/MyMonitor/tree/master/src/main/java/dea/monitor/checker) that connect to local and or remote devices or servers to confirm they are operating correctly 
* A framework for broadcasting status of checked things to another system like your home automation.
* A framework for calling a remote system to attempt to reset, reboot or repower a malfunctioning device. 
* [Examples](https://github.com/avatar42/MyMonitor/tree/master/src/main/resources) for the above.

You may also want to look at the script [MyMonitor.vb](https://github.com/avatar42/Homeseer/blob/master/scripts/MyMonitor.vb) I use to create virtual objects in my Homeseer instance automatically in many of the checkers.
