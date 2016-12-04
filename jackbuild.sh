
ANDH="/mnt/extsd/xtof/androidsdk/"

# create R files
mkdir gen
/data/data/com.termux/files/home/bin/aapt package -f \
-M AndroidManifest.xml \
-I "$ANDH/platforms/android-10/android.jar" \
-S res/ \
-J gen/ \
-m

# compile into DEX
mkdir out
# --import lib/picasso-2.5.2.jar \
jack --classpath "$ANDH/platforms/android-10/android.jar" \
--output-dex out/ \
src/ gen/

# create first APK
/data/data/com.termux/files/home/bin/aapt package -f \
-M AndroidManifest.xml \
-I "$ANDH/platforms/android-10/android.jar" \
-S res/ \
-F out/dgapp.apk

# add classes.dex in the apk
cd out/
/data/data/com.termux/files/home/bin/aapt add dgapp.apk classes.dex

# sign with debug key
apksigner -p android $ANDH/debug.keystore dgapp.apk dragondoapp.apk


