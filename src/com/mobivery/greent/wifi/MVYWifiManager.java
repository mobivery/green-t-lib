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
package com.mobivery.greent.wifi;

import java.util.Timer;
import java.util.TimerTask;

import com.mobivery.greent.MVYBatterySaverManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

public class MVYWifiManager extends BroadcastReceiver {
        
    private MVYWifiManagerCallback    m_callback = null;
    private Context                    m_context = null;
    
    /** Timer that controls the time the manager is trying to connnect to a Wifi network */
    private Timer                    m_timeOutTimer = null;
    /**    Maximum miliseconds the manager is trying to connect to a Wifi network. */
    private int                        m_connectionTimeout = 20000;
    
    //--------------------------------------------------------------------------
    //    Public methods
    //--------------------------------------------------------------------------
    public void setWifiConnectionTimeout (int timeout)
    {
        m_connectionTimeout = timeout;
    }
    //--------------------------------------------------------------------------    
    public boolean isWifiConnectionON( Context context )
    {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }    
    //--------------------------------------------------------------------------    
    public String getWifiConnectionName( Context context )
    {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo!= null )
            return wifiInfo.getSSID();
        return null;
    }
    
    //--------------------------------------------------------------------------
    public void switchOFF (Context context) 
    {        
    	if ( MVYBatterySaverManager.getInstance().areConnectionsManaged() )
    		switchWifiState (context, false );
    }
    //--------------------------------------------------------------------------
    public void switchON (Context context,  MVYWifiManagerCallback callback ) 
    {
        m_callback = callback;
        m_context = context;
        
    	if ( isWifiConnectionON(m_context) )
    	{
            m_callback.onWifiConnectionON( getWifiConnectionName(m_context) );
    	}
    	else if ( MVYBatterySaverManager.getInstance().areConnectionsManaged() )
        {
	        // Register to connection event 
	        IntentFilter intentFilter = new IntentFilter( );
	        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION );
	        context.registerReceiver (this, intentFilter);
	
	        switchWifiState (m_context, true );
	        
	        // start timeout timer
	        m_timeOutTimer = new Timer();
	        final Handler handler = new Handler();
	        m_timeOutTimer.schedule(new TimerTask() {
	            @Override
	            public void run() {
	                handler.post(new Runnable() {
	                    public void run() {
	                        switchOFF(m_context);
	                        m_callback.onWifiConnectionTimeout();
	                        finishWifiManager();
	                    }
	                });
	            }
	        }, m_connectionTimeout);                     
        }
        else
        {
            m_callback.onWifiConnectionTimeout();
        }        
    }

    //--------------------------------------------------------------------------
    //    Private methods
    //--------------------------------------------------------------------------
    private void switchWifiState (Context context, boolean state)
    {
         WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
         wifiManager.setWifiEnabled( state );        
    }
    //--------------------------------------------------------------------------
    private void finishWifiManager()
    {
        m_context.unregisterReceiver( MVYWifiManager.this );
        m_callback = null;
        m_context = null;
    }
    //--------------------------------------------------------------------------
    private void killTimeoutTimer()
    {
        if ( m_timeOutTimer != null ){
            m_timeOutTimer.cancel();
            m_timeOutTimer = null;                
        }        
    }
    //--------------------------------------------------------------------------
    //    BroadcastReceiver methods
    //--------------------------------------------------------------------------
    @Override
    public synchronized void onReceive(Context context, Intent intent) {        
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        
        if ( activeNetInfo!= null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI)
        {
            killTimeoutTimer();        
            // NetworkInfo.State state = activeNetInfo.getState();
            m_callback.onWifiConnectionON( getWifiConnectionName(m_context) );
            finishWifiManager();
        }    
    }
}
