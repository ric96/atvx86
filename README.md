# atvx86
Android TV based on Android-x86

Step 1:
   Copy the file inside the device folder to androidtv-x86/device/generic/common and overwrite any existing files
   
Step 2:
   Copy the google folder inside the device folder of your work directory.
   OR
     Add the following line to the default manifest: 
     ```
     <project path="device/google/atv" name="device/google/atv" groups="device,fugu,broadcom_pdk,generic_fs"/>
     ```
     and repo sync again.
     
Step 3: 
   Find latest version of Launcher binary in following link.
   https://developers.google.com/android/nexus/drivers#fugu
   Before doing Android build step, Extract the binary package on top of the Android source, Then LeanbackLauncher apk will     be placed on the path below.
   vendor/google/fugu/proprietary/LeanbackLauncher.apk
   Source: https://github.com/peyo-hd/device_brcm_rpi2/wiki#how-to-apply-android-tv-leanback-launcher
   
And then follow the normal Android-x86 build process
