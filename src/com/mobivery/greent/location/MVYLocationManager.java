/* Copyright 2012 Mobiguo S.L.
 * 
 * This file is part of Green-T Library.
 * 
 * Green-T Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Green-T Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lessr General Public License
 * along with Green-T Library.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mobivery.greent.location;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

import com.mobivery.greent.MVYBatterySaverManager;
import com.mobivery.greent.MVYTimer;
import com.mobivery.greent.wifi.MVYWifiManager;
import com.mobivery.greent.wifi.MVYWifiManagerCallback;

public class MVYLocationManager implements MVYWifiManagerCallback
{
    /**
     * Limit between coarse and fine location in meters. Only if accuracy is 
     * less than 30 meters the manager will activate GPS to get the location
     */
    final private float m_limitAccuracy = 30;
    
    private float m_accuracy = -1;
    private Context m_context = null;
    private MVYLocationManagerCallback m_callback = null;
    private LocationManager m_locationManager = null;
    private MVYLocationListener m_locationListener = null;
    private Location m_currentBestLocation = null;
    private MVYWifiManager m_wifiManager = null;
    private boolean m_wifiPreviouslyON = false;
    
    /**    Timer that controls the time the manager is trying to locate the device */
    private Timer m_locationtimeOutTimer = null;
    // private MVYTimer m_locationtimeOutTimer = null;
    /**    Timer that controls the time the manager is trying to locate the device by Wifi 
     *    before trying to do it by GPS
     */
    private Timer m_wifiLocationtimeOutTimer = null;
    // private MVYTimer m_wifiLocationtimeOutTimer = null;
    
    /**
     *  Maximum miliseconds the manager is trying to the get device location.
     *  Default value is 30 seconds
     */
    private int m_locationTimeout = 30000;

    private int m_wifiLocationTimeout = -1;
    
    //--------------------------------------------------------------------------
    //    Public methods
    //--------------------------------------------------------------------------
    public void setLocationTimeout (int timeout )
    {
        m_locationTimeout = timeout;
    }
    //--------------------------------------------------------------------------
    public boolean isGPSEnabled( Context context )
    {
        LocationManager manager = (LocationManager) context.getSystemService( Context.LOCATION_SERVICE );
        return manager.isProviderEnabled( LocationManager.GPS_PROVIDER ); 
    }
    //--------------------------------------------------------------------------    
    public void getActualLocationWithAccuracy (float accuracy, 
                                                    int locationTimeout, 
                                                    int wifiTimeout, 
                                                    Context context, 
                                                    MVYLocationManagerCallback callback)
    {
        m_locationTimeout		= locationTimeout;
        m_wifiLocationTimeout	= wifiTimeout;
        getActualLocationWithAccuracy(accuracy, context, callback);
    }
    //--------------------------------------------------------------------------    
    public void getActualLocationWithAccuracy (float accuracy, Context context, MVYLocationManagerCallback callback)
    {        
        m_context = context;
        m_accuracy = accuracy;
        m_callback = callback;
        
        // start location timeout timer (timeout including GPS and Wifi location tries)
        m_locationtimeOutTimer = new Timer();
        final Handler handler = new Handler();
        m_locationtimeOutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        stopGettingLocations();
                        if ( m_currentBestLocation == null )
                            m_callback.onGetLocationError();
                        else
                            m_callback.onGetLocation( m_currentBestLocation );
                    }
                });
            }
        }, m_locationTimeout);
        
//        m_locationtimeOutTimer = new MVYTimer(new Runnable() {	
//			@Override
//			public void run() {                 
//				stopGettingLocations();
//                if ( m_currentBestLocation == null ){
//                    m_callback.onGetLocationError();
//                }else{
//                    m_callback.onGetLocation( m_currentBestLocation );
//                }  
//			}
//		}, m_locationTimeout);

        
        // start wifi location timeout timer
        if ( m_wifiLocationTimeout >=0 && m_wifiLocationTimeout < m_locationTimeout )
        {
            m_wifiLocationtimeOutTimer = new Timer();
            final Handler wifiHandler = new Handler();
            m_wifiLocationtimeOutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    wifiHandler.post(new Runnable() {
                        public void run() {
                            if ( isGPSEnabled(m_context) ){
                                resetWifiState();
                                getLocationByGPS();
                            }
                        }
                    });
                }
            }, m_wifiLocationTimeout);            
        }
        
//        m_wifiLocationtimeOutTimer = new MVYTimer(new Runnable() {
//			@Override
//			public void run() {
//                if ( isGPSEnabled(m_context) ){
//                    resetWifiState();
//                    getLocationByGPS();
//                }
//			}
//		}, m_wifiLocationTimeout);
        
        
        // Start location process
        if ( accuracy < m_limitAccuracy )
        {          
            getLocationByGPS();		// try use GPS location
        }
        else
        {
            // try use WIFI location
            m_wifiManager = new MVYWifiManager();
            m_wifiPreviouslyON = m_wifiManager.isWifiConnectionON(context);
            if ( !m_wifiPreviouslyON )
            {
            	if ( MVYBatterySaverManager.getInstance().areConnectionsManaged() ){
                    m_wifiManager.setWifiConnectionTimeout( m_locationTimeout );
                    m_wifiManager.switchON(context, this);      
                    getLocationByWifi();
            	}
            	else 
            	{
            		if ( m_wifiLocationtimeOutTimer!= null )
            			m_wifiLocationtimeOutTimer.cancel();
            		getLocationByGPS();
            	}
            } else {                	
                getLocationByWifi();
            }
        }        
    }
    
    //--------------------------------------------------------------------------
    //    Private methods
    //--------------------------------------------------------------------------
    private void getLocationByWifi ()
    {
        getLocation( LocationManager.NETWORK_PROVIDER );        
    }
    //--------------------------------------------------------------------------
    private void getLocationByGPS ()
    {
        getLocation( LocationManager.GPS_PROVIDER );
    }
    //--------------------------------------------------------------------------
    private void getLocation (String networkProvider)
    {
        // Stop and reset any previous location configurations
        stopLocationManager();
        
        // Acquire a reference to the system Location Manager
         m_locationManager = (LocationManager) m_context.getSystemService(Context.LOCATION_SERVICE);
         
        // Define a listener that responds to location updates
         m_locationListener = new MVYLocationListener();
         
        // Register the listener with the Location Manager to receive location updates
         m_locationManager.requestLocationUpdates(networkProvider, 0, 0, m_locationListener);    
    }

    //--------------------------------------------------------------------------
    private void stopLocationManager ()
    {
        // Stop getting locations and kill location manager
        if ( m_locationManager != null ){
            m_locationManager.removeUpdates(m_locationListener);
            m_locationManager = null;
            m_locationListener = null;            
        }
    }    
    //--------------------------------------------------------------------------
    /**
     *    Set the wifi in the same state it was before the location process
     */
    private void resetWifiState()
    {
        if ( !m_wifiPreviouslyON )        m_wifiManager.switchOFF(m_context);
    }
    //--------------------------------------------------------------------------
    private void stopGettingLocations ()
    {
        // Stop location timeout timer
        if ( m_locationtimeOutTimer != null ){
            m_locationtimeOutTimer.cancel();
//            m_locationtimeOutTimer.stop();
            m_locationtimeOutTimer = null;                
        }

        // Stop wifi location timeout timer
        if ( m_wifiLocationtimeOutTimer != null ){
            m_wifiLocationtimeOutTimer.cancel();
//            m_wifiLocationtimeOutTimer.stop();
            m_wifiLocationtimeOutTimer = null;    
        }
        
        // Stop getting locations and kill location manager
        stopLocationManager();

        // turn off wifi if it was previously OFF
        resetWifiState();
    }
    
    //--------------------------------------------------------------------------
    //    MVYWifiManager inherit methods
    //--------------------------------------------------------------------------
    @Override
    public void onWifiConnectionON(String wifiName) 
    {
        // Nothing to do 
    }
    //--------------------------------------------------------------------------
    @Override
    public void onWifiConnectionTimeout() {
        resetWifiState();
    }

    //--------------------------------------------------------------------------
    //    Mobivery location listener implementation
    //--------------------------------------------------------------------------
    public class MVYLocationListener implements LocationListener
    {
        @Override
        public void onLocationChanged(Location location) 
        {    
            // If it's been more than two minutes since the current location, use the new location
            // because the user has likely moved
            float acc = location.getAccuracy();
            if ( acc < m_accuracy )
            {
                stopGettingLocations();
                m_callback.onGetLocation( location );
            } 
            else if ( m_currentBestLocation == null )
            {    
                m_currentBestLocation = location;
            } 
            else if ( acc < m_currentBestLocation.getAccuracy() ) 
            {
                m_currentBestLocation = location;                    
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) { 
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
}
