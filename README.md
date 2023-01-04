# RectTestCamera
A urine test paper mobile phone image acquisition system

## Software Overview
Color parameters of test paper are identified by mobile phone, and relevant parameters are extracted to realize data analysis of test paper.

### Software function
* Test paper shooting: Obtain the color block part of the test paper through the custom control, named by the time stamp.
* Camera data collection: camera state data that has an impact on imaging collected by camera at the same time as the test paper shooting is encapsulated in txt file, which has the same name as the photos at the corresponding time and is saved with the system folder DCIM.
* Camera parameter control: ISO, exposure and other parameters can be set.

### Software characteristics
This system runs based on Android environment. It can collect the camera status corresponding to the moment when the photo is taken and encapsulate the picture into txt and store it in the DCIM folder of the system. 

## Interface and operation description
### Software operation interface description
After opening the software, the login interface is directly displayed, as shown in the figure.

![image](/image/login.jpg)
![image](/image/operation.png)

① Exit the software button. Click to return to the main interface.  
② Software name.  
③ Flash button. Tap to turn on the camera's rear flash.  
④ Display of ambient light parameters. In this box, the brightness parameters of the ambient light can be displayed in real time. Debugging steps: if the lux value does not change when the software is opened, it needs to be debugging. In particular, you can move the mobile phone left and right to change the light source environmental stimulation sensor response.  
⑤ Rectangular box. When shooting the test paper, the position of the test paper is positioned and the image of the test paper is obtained.  
⑥ Label button. Click to enter text or numeric labels on the keyboard to mark the test paper. The labels are saved in the corresponding folder. However, note that the input content is not stored locally and the original input content is not saved when entering the software after exiting.  
⑦ Shoot button. After entering the operation interface, you can click the test paper to shoot and save.  
⑧ Preview button. You can preview the test paper images saved during shooting. If you use the preview function for the first time, you need to select a folder. The specific operation is shown as follows.  
![image](/image/album.png)  
Step 8 Operation: Tap position 8 to enter the system album interface and select the image to preview.

## Operating Environment
### Hardware
Android7.0 above system, running memory 2GB above.
### Software
Android7.0 or above, AndroidStudio3.0 or above.

## Install
Users can download and click the installation package on phones running Android7.0 or above and follow the prompts to complete the installation.  
Specific operations are as follows:  
![image](/image/install_1.png)
![image](/image/install_2.png)  
Follow the prompts to complete the permission to read and write device photo files, and the permission to take photos and record videos to use this software.

## Warning
In the process of user operation, the phenomenon of system error may be because the user does not follow the normal operation, for example, click the camera button several times, more than the allowed capacity of the system will cause a lag. To make you have a good experience, please follow the operation instructions. For the correct operation mode, please refer to the Interface and Operation Instructions.

## Program File
This software generates a txt file of encapsulated parameters and a photo file each time it takes a photo, which is saved in the DCIM folder of the Android system.

