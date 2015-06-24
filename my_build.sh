#!/bin/sh

export ANDROID_SDK=/home/aFei/work/google/android-sdk-linux
export ANDROID_NDK=/home/aFei/work/google/android-ndk-r10d
export JAVA_HOME=/home/aFei/work/google/jdk1.7.0_76
export CLASSPATH=.:$JAVA_HOME/lib:$JAVA_HOME/jre/lib
export PATH=$PATH:$JAJVA_HOME/bin:$ANDROID_SDK/platform-tools:$ANDROID_SDK/tools

[ $# -eq 1 -a "$1" = "clean" ] && {
	echo "clean..."
	rm vlc/build-android-arm-linux-androideabi/ -rf
	rm vlc/contrib/*android* -rf
	rm vlc-android/build -rf
	rm libvlc/build -rf

	exit 0
}

sh compile.sh -a armeabi-v7a -r

#sh compile.sh -a armeabi-v7a  release
