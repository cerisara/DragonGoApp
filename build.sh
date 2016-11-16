#!/bin/bash

export ANDBIN="/home/xtof2/softs/android-sdk-linux/build-tools/19.1.0/"
export ANDJAR="/home/xtof2/softs/android-sdk-linux/platforms/android-10/android.jar"
export SDKLIB="/home/xtof2/softs/android-sdk-linux/tools/lib/sdklib.jar"


# don't use this build.sh: it does not work.
# Rather use an ANT build file with the "old" android toolchain, and jill/jack with the new one
# Here is how to build an ant file:

$ANDBIN/../../tools/android create project --target 1 --name totoproj --path . --package fr.xtof54.totoproj --activity TotoAct

exit

mkdir -p gen bin/classes

SRCFILES=$(find src -name "*.java" | awk '{a=a" "$1}END{print a}')

# $ANDBIN/aapt package -f -m --auto-add-overlay -I $ANDJAR -M AndroidManifest.xml -S res -J gen -F toto.ap_ --rename-manifest-package fr.xtof54.DragonGoApp -c en

$ANDBIN/aapt package -f -m -I $ANDJAR -M AndroidManifest.xml -J gen -S res -v

GENFILES=$(find gen -name "*.java" | awk '{a=a" "$1}END{print a}')
LIBS=$(ls libs/*.jar | awk '{a=a":"$1}END{print a}')

javac -bootclasspath $ANDJAR -source 1.7 -target 1.7 -cp "$ANDJAR""$LIBS" -d bin/classes $SRCFILES $GENFILES

JARS=$(find $PWD/libs -name "*.jar" | awk '{a=a" "$1}END{print a}')

cd bin/classes
$ANDBIN/dx --dex --output toto.dex $JARS .
cd ../..

zip -r gen/assets.zip assets

java -cp $SDKLIB com.android.sdklib.build.ApkBuilderMain toto.apk -u -z toto.ap_ -f bin/classes/toto.dex -z gen/assets.zip

# jarsigner -verbose -storepass:file pasfle -tsa http://timestamp.digicert.com toto.apk dragongoapp 
# $ANDBIN/zipalign -f 4 toto.apk dragongoapp.apk

