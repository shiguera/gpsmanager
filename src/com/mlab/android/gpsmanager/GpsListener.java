package com.mlab.android.gpsmanager;


import android.location.Location;

public interface GpsListener {
	void firstFixEvent();
	void updateLocation(Location loc);
	
}

