#!/bin/bash

export ANDBIN="/home/xtof2/softs/android-sdk-linux/build-tools/19.1.0/"
export ANDJAR="/home/xtof2/softs/android-sdk-linux/platforms/android-10/android.jar"
export SDKLIB="/home/xtof2/softs/android-sdk-linux/tools/lib/sdklib.jar"

mkdir -p gen bin/classes

SRCFILES=$(find src -name "*.java" | awk '{a=a" "$1}END{print a}')

$ANDBIN/aapt package -f -m --auto-add-overlay -I $ANDJAR -M AndroidManifest.xml -S res -J gen -F toto.ap_ --rename-manifest-package fr.xtof54.DragonGoApp -c en

GENFILES=$(find gen -name "*.java" | awk '{a=a" "$1}END{print a}')
LIBS=$(ls libs/*.jar | awk '{a=a":"$1}END{print a}')

javac -cp "$ANDJAR""$LIBS" -d bin/classes $SRCFILES $GENFILES

$ANDBIN/dx --dex --output toto.dex bin/classes

zip -r gen/assets.zip assets

java -cp $SDKLIB com.android.sdklib.build.ApkBuilderMain toto.apk -u -z toto.ap_ -f toto.dex -z gen/assets.zip

jarsigner -verbose -storepass:file pasfle -tsa http://timestamp.digicert.com toto.apk dragongoapp 
$ANDBIN/zipalign -f 4 toto.apk dragongoapp.apk

