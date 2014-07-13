NETMUSECLIENT

Compiling 

Please note that due to a bug with OpenGL and the Proclipsing plug-in it is very difficult to export an OSX application. It's possible, but the plug-in cannot automatically move the required libraries, so it has to be done by hand. For now it will have to be a Windows only version. Feel free to write your own script, or wait for a fix from the developers.

An already compiled binary is available in the application.windows folder.

You will need:

Java JDK 1.6 
Eclipse 
Proclipsing-Eclipse Plug-in
Processing 2.0

Libraries required:
controlP5
sqlibrary for Processing
oscp5 and netp5
Beads

Please note that the libraries/jar files are included with the source code, in the 'lib' folder, and the project should hopefully find them automatically even if you don't have them.

Procedure:
After installing eclipse and the proclipsing plug-in, make sure Proclipsing knows where the processing folder is (it will ask for it). Then, right click in the project explorer, choose Import, choose Existing Projects into Workspace, then navigate to the netMuseClient folder and it should automatically find the project. It should then be compilable right away. If it doesn't work you likely missed a dependancy, or Eclipse has become confused (unfortunately, it's not the most stable of IDEs). If you need help compiling or would like to report a bug, please e-mail bv524@york.ac.uk.

If you want to export binaries, after compiling there are a few more steps to take to put the data in the right place. I will automate this in a future version, but for now you need to copy the 'data' folder from the Eclipse project into the application.windows or folder (next to the exe file). You will also need to copy everything from the folder titled DLL into that folder. 

Please not that it will not run under JRE 1.6, and it may not be completely compatible with JRE 1.7.