#!/bin/bash

#make-standalone-toolchain.sh --platform=android-15 --arch=arm  --toolchain=arm-linux-androideabi-4.8 --install-dir=toolchain-platform-15-arm-gcc-4.8
#source android-setup-env.sh

#define
#$CXX
#$BOOST_ROOT
#$LIBTORRENT_ROOT
#$JDK_INCLUDE_1
#$JDK_INCLUDE_2
#$LBITORRENT_LIBS
#$ANDROID_ARM_TOOLCHAIN_ROOT

#export CXXFLAGS="-mthumb -fno-strict-aliasing -lstdc++ -D__GLIBC__ -D_GLIBCXX__PTHREADS -D__arm__ -D_REENTRANT -O3"
#copy user-config.jam to ~/
#$BOOST_ROOT/b2 toolset=gcc variant=release link=static
#$BOOST_ROOT/bjam toolset=gcc-androidarm variant=release deprecated-functions=off link=static

CXXFLAGS="-mthumb -fno-strict-aliasing -lstdc++ -D__GLIBC__ -D_GLIBCXX__PTHREADS -D__arm__ -D_REENTRANT -O3"
DEFINES="-DNDEBUG=1 -DBOOST_ASIO_SEPARATE_COMPILATION=1 -DTORRENT_USE_CLOCK_GETTIME=1 -DTORRENT_DISABLE_GEO_IP=1 -DTORRENT_NO_DEPRECATE=1"
INCLUDES="-I$BOOST_ROOT -I$LIBTORRENT_ROOT/include -I$JDK_INCLUDE_1 -I$JDK_INCLUDE_2"
LIBS="-ltorrent -lboost_system -lboost_chrono -lboost_date_time -lboost_thread"
LDFLAGS="-Wl,-Bsymbolic -L$LBITORRENT_LIBS"
TARGET="libjlibtorrent.so"

$CXX $CXXFLAGS $DEFINES $INCLUDES -c swig/libtorrent_jni.cpp
$CXX -shared -o $TARGET libtorrent_jni.o $LIBS $LDFLAGS

rm -rf libtorrent_jni.o