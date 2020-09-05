#!/bin/bash

# put your keystore password in this (uncommited) file:
signerpass=$(cat jarsigner.password)

export ANDBIN=/home/xtof/softs/adt-bundle-linux-x86_64-20140702/sdk/build-tools/21.1.2/
export ANDJAR=/home/xtof/softs/adt-bundle-linux-x86_64-20140702/sdk/platforms/android-19/android.jar

export ANDBIN=/opt/android-sdk/build-tools/24.0.0/
export ANDJAR=/opt/android-sdk/platforms/android-19/android.jar

export ANDBIN=/usr/lib/android-sdk/build-tools/24.0.0/
export ANDJAR=/usr/lib/android-sdk/platforms/android-23/android.jar
export ANDBIN=/usr/lib/android-sdk/build-tools/24.0.0/

export ANDBIN=/home/xtof/softs/android-sdk-linux/build-tools/23.0.1/
export ANDJAR=/home/xtof/softs/android-sdk-linux/platforms/android-23/android.jar

export ANDBIN=/usr/lib/android-sdk/build-tools/27.0.1/
export ANDJAR=/usr/lib/android-sdk/platforms/android-23/android.jar

rm -rf out
mkdir gen out

$ANDBIN/aapt package -f \
    -M AndroidManifest.xml \
    -I $ANDJAR \
    -S res/ \
    -J gen/ \
    -m

# La version avec jack/jill ne marche pas sur lully, je ne sais pas pourquoi ?

#libs=$(ls libs/*.jar)
#lidx=0
#imports=""
#clp=""
#for lib in $libs; do
#    java -jar $ANDBIN/jill.jar --output libs/xx$lidx --verbose $lib
#    imports=$imports" --import libs/xx"$lidx
#    clp=$clp":"$lib
#    lidx=$(($lidx+1))
#done

GENFILES=$(find gen -name "*.java" | awk '{a=a" "$1}END{print a}')
SRCFILES=$(find src -name "*.java" | awk '{a=a" "$1}END{print a}')
LIBS=$(ls libs/*.jar | awk '{a=a":"$1}END{print a}')

mkdir out
javac -bootclasspath $ANDJAR -source 1.7 -target 1.7 -cp "$ANDJAR""$LIBS" -d out $SRCFILES $GENFILES

JARS=$(ls $PWD/libs/*.jar | awk '{a=a" "$1}END{print a}')
echo $JARS
cd out
$ANDBIN/dx --dex --output classes.dex $JARS .
cd ..

# java -jar $ANDBIN/jack.jar --classpath "$ANDJAR"$clp --output-dex out $imports src/ gen/

$ANDBIN/aapt package -f -M AndroidManifest.xml -I $ANDJAR -S res/ -F out/app.apk

find assets -type f -exec $ANDBIN/aapt add -v out/app.apk {} \;
# find lib -name "*.so" -exec $ANDBIN/aapt add -v out/app.apk {} \;
cd out
$ANDBIN/aapt add app.apk classes.dex

# run it once in your HOME:
# keytool -genkey -v -keystore PATH/TO/YOUR_RELEASE_KEY.keystore -alias YOUR_ALIAS_NAME -keyalg RSA -keysize 2048 -validity 10000

echo "$signerpass"

jarsigner -verbose -keystore $HOME/maclef.keystore -storepass $signerpass -keypass $signerpass -sigalg SHA1withRSA -digestalg SHA1 app.apk xtof54


