package com.mlab.android.gpsmanager;


import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

/**
 * Proporciona gestión del GPS del dispositivo. <p>
 * Mantiene una lista de GpsListenerer que reciben notificación de las 
 * nuevas posiciones leidas del GPS a través del método 
 * 'updateLocation()' del interface GpsListenerer.<p>
 * Para añadir un GpsListenerer a la lista de escuchantes del GpsManager
 * se utiliza el método 'registerGpsListenerer()'. También se dispone de un
 * método 'unregisterGpsListenerer()'.<p>
 * Para utilizar el GpsManager hay que instanciarle a través del constructor
 * de la clase, que recibe como parámetro el Context de la Activity que le instancia.<p>
 * Una vez instanciado hay que comprobar con el método 'isGpsEnabled()' que el GPS se ha activado 
 * correctamente.<p>
 * Para iniciar las actualizaciones hay que llamar al  método '<em>startGpsUpdates()</em>'<p>
 * Para detener las actualizaciones se utiliza el método <em>'stopGpsUpdates()'</em>
 * @author shiguera
 *
 */
public class GpsManager {

	// FIXME trabajar en un thread secundario
	private Context context;
	/**
	 * Lista de listeners del GpsManager
	 */
	private ArrayList<GpsListener> gpsListeners;
	
	// locationManager
	private LocationManager locationManager;
	//private LocationProvider locationProvider;	
	// minTime : the minimum time interval for notifications, in milliseconds. 
	// This field is only used as a hint to conserve power, 
	// and actual time between location updates may be greater or lesser than this value.
	private long minTime;
	
	// minDistance : the minimum distance interval for notifications, in meters
	private float minDistance;
	
	// gpsEventFirstFix : Indica si ya se ha producido el evento FIRST_FIX
	private boolean gpsEventFirstFix;
	
	// lastLocation: Almacena el último objeto location leido del GPS
	private Location lastLocation;

	// gpsStatus: Almacena el ultimo objeto GpsStatus leido del GPS
	private GpsStatus gpsStatus;
	
	// gpsSatellite: Lista de satelites utilizada en el último location (lastLocation)
	private ArrayList<GpsSatellite> gpsSatellite; 
	
	// numpuntos: Número de puntos leidos desde que se activó el GPS
	private int numpuntos;
	
	
	/**
	 *  Constructor
	 * @param ctxt Contexto de la Actividad que inicializa el GpsManager
	 */
	public GpsManager(Context ctxt) {
		Log.d("HAL","GpsManager.builder()");
		this.gpsListeners= new ArrayList<GpsListener>();
		
		this.context=ctxt;
		this.minTime=10;
		this.minDistance=10.0f;
		this.initGps();
		lastLocation=new Location(LocationManager.GPS_PROVIDER);
		gpsSatellite=new ArrayList<GpsSatellite>();
		gpsEventFirstFix=false;
	}		

	/**
	 * Añade el GpsController a la lista de los notificados por cambio de posición
	 * @param gpsController
	 */
	public boolean registerGpsListener(GpsListener gpsController) {
		Log.d("HAL", "GpsManager.registerGpsController()");
		boolean resp=this.gpsListeners.add(gpsController);
		return resp;
	}
	public boolean unregisterGpsListener(GpsListener gpsController) {
		Log.d("HAL", "GpsManager.unregisterGpsController()");
		boolean resp=this.gpsListeners.remove(gpsController);
		return resp;
	}
	
	/**
	 * Inicializa el LocationManager del GPS 
	 * @return true: Si la inicialización se realizó sin problemas<p>
	 *        false: en caso contrario
	 */
	private boolean initGps() {
        Log.d("HAL","GpsManager.initGps()");
		this.locationManager = (LocationManager) (this.context.getSystemService(Context.LOCATION_SERVICE));
        if (isGpsEnabled()) {
        	//this.locationProvider = this.locationManager.getProvider(LocationManager.GPS_PROVIDER);       	
        	Log.d("HAL","GpsManager.initGps(): isGpsEnabled=true");
        	return true;
        } else {
        	Log.w("HAL","GpsManager.initGps()-WARNING: isGpsEnabled=false");
        	return false;
        }
	}
	
	/**
	 * Indica si el LocationManager está disponible, esto es, si está activado el GPS.
	 * @return true: si el proveedor Gps está disponible<p>
	 *         false: en caso contrario
	 */
	public boolean isGpsEnabled() {
        try {
        	return this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
        	return false;
        }
	}
	
	/**
	 * Devuelve true si ya se ha fijado posición por primera vez desde la última activación 
	 * los updates del Gps
	 * @return true: si el evento FIRST_FIX ya se ha producido<p>
	 *         false: en caso contrario
	 */
	//        
	public boolean isGpsEventFirstFix() {
		return gpsEventFirstFix;
	}
	
	/**
	 * Inicia la actualización periódica de la posición proporcionada por el GPS.<p>
	 * Asigna el GpsStatusListener e inicia el requestLocationUpdates
	 * @return TRUE si el Gps está habilitado<p>
	 *         false si no se dispone de actualizaciñon de posición
	 */
	public boolean startGpsUpdates() {
		Log.d("HAL","GpsManager.startGpsUpdates()");
		if(!isGpsEnabled()) {
			initGps();
		}
		if (isGpsEnabled()) {
        	this.locationManager.addGpsStatusListener(this.gpsStatusListener);
            //Log.d("HAL", "    GpsStatusListener asignado");
        	this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, this.minTime, 
        			this.minDistance, this.locationListener);
            //Log.d("HAL", "    GpsManager: requestLocationUpdates started");
        	return true;
        } else {
        	return false;
        }
	}

	/**
	 * Detiene las actualizaciones de la posición del Gps
	 */
	public void stopGpsUpdates() {
		// Detener actualizaciones del GPS
		Log.d("HAL","GpsManager.stopGpsUpdates()");
		if (this.locationManager != null) {
			//Log.d("HAL", "    GpsManager: Removing updates");
			this.locationManager.removeUpdates(this.locationListener);
			//Log.d("HAL", "    GpsManager: Removed locationListener");
			this.locationManager.removeGpsStatusListener(this.gpsStatusListener);
			//Log.d("HAL", "    GpsManager: Removed gpsStatusListener");			
		}
		this.gpsSatellite = new ArrayList<GpsSatellite>();
	}
	
	//
	// gpsStatusListener
	//
	Listener gpsStatusListener = new GpsStatus.Listener() {		
		
		@Override
		public void onGpsStatusChanged(int event) {
			String text="";
			switch(event) {
			case(GpsStatus.GPS_EVENT_SATELLITE_STATUS):
				text="GPS_EVENT_SATELLITE_STATUS";
				GpsStatus status = locationManager.getGpsStatus(null);
				updateGpsStatus(status);				
				break;
			case(GpsStatus.GPS_EVENT_STARTED):
				text="GPS_EVENT_STARTED";
				break;
			case(GpsStatus.GPS_EVENT_STOPPED):
				text="GPS_EVENT_STOPED";
				break;
			case(GpsStatus.GPS_EVENT_FIRST_FIX):
				text="GPS_EVENT_FIRST_FIX";
				GpsManager.this.gpsEventFirstFix=true;
				break;
			}
			//Log.d("HAL", "GpsManager.onGpsStatusChanged(): "+text);
		}
	};
	
	// updateGpsStatus()
	//        Actualiza los valores de gpsStatus, satelites y otros
	private void updateGpsStatus(GpsStatus status) {
		this.gpsStatus=status;
		Iterable<GpsSatellite> sats = gpsStatus.getSatellites();
		Iterator<GpsSatellite> it = sats.iterator();
		gpsSatellite = new ArrayList<GpsSatellite>();
		while(it.hasNext()) {
			GpsSatellite sat = it.next();
			gpsSatellite.add(sat);
		}
	}

	/**
	 * Este método es llamado cada vez que el LocationManager actualiza 
	 * la posición en el método 'onLocationChanged()' de su LocationListener.<p>
	 * El método actualiza las variables 'lastLocation' y 'numpuntos' y
	 * luego notifica a todos los GpsController regitrados llamando a sus métodos
	 * 'updateLocation()'<p>
	 * 
	 * @param loc Ultima actualización de Location proporcionada por el LocationManager 
	 */
	private void updateLocation(Location loc) {
		// Actualizar lastLocation
		lastLocation.set(loc);
		// Cambio la fecha por el bug detectado en la fecha UTC de los gps de Samsung Galaxy
    	Date now = new Date();
        long tt=now.getTime();
        lastLocation.setTime(tt);
        for(int i=0; i<this.gpsListeners.size(); i++) {
        	this.gpsListeners.get(i).updateLocation(loc);
        }
    	// Actualizar numero de puntos leidos
    	numpuntos++;
	}
	
	/**
	 * LocationListener del LocationManager que recibe las notificaciones de nuevas posiciones
	 */
	LocationListener locationListener = new LocationListener() {
	    @Override
		public void onLocationChanged(Location loc) {
	    	Log.d("HAL","LocationListener.onLocationChanged():\n");
	    	updateLocation(loc);
	    }
	    @Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
	    	switch(status) {
	    	case(LocationProvider.AVAILABLE):
	    		Log.d("HAL","LocationListener.onStatusChanged(): LocationProvider.AVAILABLE");
	    		
	    		break;
	    	case(LocationProvider.TEMPORARILY_UNAVAILABLE):
	    		Log.d("HAL","LocationListener.onStatusChanged(): LocationProvider.TEMPORARILY_UNAVAILABLE");

	    		break;
	    	case(LocationProvider.OUT_OF_SERVICE):
	    		Log.d("HAL","LocationListener.onStatusChanged(): LocationProvider.OUT_OF_SERVICE");
	    		
	    		break;
	    	}
	    	//Toast.makeText(getApplicationContext(), "Status Changed", 2000).show();
	    }
	    @Override
		public void onProviderEnabled(String provider) {
	    	Log.d("HAL","LocationListener.onProviderEnabled()");
	    }	
	    @Override
		public void onProviderDisabled(String provider) {
	    	Log.d("HAL","LocationListener.onProviderDisabled()");
	    }
	};
	
	public long getMinTime() {
		return minTime;
	}
	public void setMinTime(long minTime) {
		this.minTime = minTime;
	}
	public float getMinDistance() {
		return minDistance;
	}
	public void setMinDistance(float minDistance) {
		this.minDistance = minDistance;
	}
	public int getNumsats() {
		return gpsSatellite.size();
	}
	public Location getLastLocation() {
		return lastLocation;
	}
	public float getAccuracy() {
		return lastLocation.getAccuracy();
	}
	public int getNumpuntos() {
		return numpuntos;
	}
	
}
