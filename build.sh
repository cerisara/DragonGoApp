#!/bin/bash

# put your keystore password in this (uncommited) file:
signerpass=$(cat jarsigner.password)
signername="xtof54"

export ANDBIN=/usr/lib/android-sdk/build-tools/debian/
export ANDJAR=/usr/lib/android-sdk/platforms/android-23/android.jar

rm -rf out
mkdir gen out

$ANDBIN/aapt package -f -M AndroidManifest.xml -I $ANDJAR -S res/ -J gen/ -m

GENFILES=$(find gen -name "*.java" | awk '{a=a" "$1}END{print a}')
SRCFILES=$(find src -name "*.java" | awk '{a=a" "$1}END{print a}')
LIBS=$(ls libs/*.jar | awk '{a=a":"$1}END{print a}')

mkdir out
javac -bootclasspath $ANDJAR -source 1.7 -target 1.7 -cp "$ANDJAR""$LIBS" -d out $SRCFILES $GENFILES

JARS=$(ls $PWD/libs/*.jar | awk '{a=a" "$1}END{print a}')
cd out
$ANDBIN/dx --dex --output classes.dex $JARS .
cd ..
$ANDBIN/aapt package -f -M AndroidManifest.xml -I $ANDJAR -S res/ -F out/app.apk

find assets -type f -exec $ANDBIN/aapt add -v out/app.apk {} \;
cd out
$ANDBIN/aapt add app.apk classes.dex
jarsigner -verbose -keystore $HOME/maclef.keystore -storepass $signerpass -keypass $signerpass -sigalg SHA1withRSA -digestalg SHA1 app.apk $signername

