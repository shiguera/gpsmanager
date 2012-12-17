#! /bin/bash

mvn install:install-file -Dfile=bin/gpsmanager.jar -DgroupId=com.mlab.android.gpsmanager -DartifactId=GpsManager -Dpackaging=jar -Dversion=1.0
