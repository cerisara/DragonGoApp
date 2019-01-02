DragonGoApp
===========

Android app to play the game of go on the DGS servers.
Follow below links for more information:

* For installation via F-Droid: [F-droid](https://f-droid.org/repository/browse/?fdfilter=dragongoapp&fdid=fr.xtof54.dragonGoApp) (you have to enable the archive F-droid repositories)

* You can also compile it quite easily, because it does not depend on any compilation framework/IDE: it has minimal and simple libraries, and compilation is done only with the build.sh script. So you just need to adapt the build.sh script with the correct path to your android SDK, create a signing key and write the passphrase on jarsigner.password, and run the script.

* More information: [Webpage](http://cerisara.github.io/DragonGoApp/)

How to compile
--------------

- You need a linux OS, preferably Ubuntu (text-only is enough, no GUI required).
If you're running Windows 10, you should try and activate the Ubuntu sub-module.
If you're running an older Windows, I recommend to install virtualbox and then install Ubuntu in Virtualbox.
- Compilation is done with a simple bash script: **build.sh**
You may take a look at it, it's pretty simple.
- You need to install the android libraries **android.jar** as well as android programs **aapt**, **dex** with:
	- sudo apt install android-sdk
- The above command will also install the java sdk and the jarsigner program, as well as the android platform **android.jar**. At the time of writing this README, it is /usr/lib/android-sdk/platforms/android-23/android.jar, but it may change and you should double-check where is exactly this jar file, and update accordingly the variable at the start of the build.sh script.
- At the end of the **build.sh** script, the generated apk file must be signed with a personal key that uniquely identifies you, otherwise it cannot be installed on any Android device. You must first generate this key with the following command:

```	
keytool -genkey -v -keystore $HOME/maclef.keystore -alias YOUR_ALIAS_NAME -keyalg RSA -keysize 2048 -validity 10000
```	

The variable **signername** at the start of the build.sh script defines the name the signer (YOUR_ALIAS_NAME), please set it correctly.
You may also change the location of your key store, but you should then update accordingly the end of the build.sh script.
Finally, the password that you have used when running the **keytool** command should be put in the **jarsigner.password** file
so that the build.sh script can retrieve it to sign the apk.
I've decided to put the password in a local file to avoid the risk to accidently push the password into a git repository.

Note that this **buid.sh** script is independent of the android program and you can use it for your own program as long as you
use the same simple directory structure.


