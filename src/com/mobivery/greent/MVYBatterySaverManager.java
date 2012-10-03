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
package com.mobivery.greent;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import com.mobivery.greent.threeg.MVY3GManager;
import com.mobivery.greent.threeg.MVY3GManagerCallback;
import com.mobivery.greent.wifi.MVYWifiManager;
import com.mobivery.greent.wifi.MVYWifiManagerCallback;

/**
 * Singleton class that controls 3G and Wifi connections, notify battery level 
 *
 */
public class MVYBatterySaverManager implements MVYWifiManagerCallback, MVY3GManagerCallback {
    
    private static final MVYBatterySaverManager instance = new MVYBatterySaverManager();
    
    public enum ManagerState 
    {
        IDLE,
        gettingWIfiConnection,
        getting3GConnection,
        gettingAnyConnection,
        CONNECTED
    }
    
    private class Connections
    {
        boolean gettingWIFIConnection = false;
        boolean getting3GConnection = false;
    }
    /**
     * 	Current manager state
     */
    private ManagerState m_state = ManagerState.IDLE;
    /**
     * Activity that is waiting for library answers
     */
    private MVYBatteryManagerCallback m_Callback = null;
    private Context m_Context = null;
    private Connections m_currentConnections;
    /**
     * 	Timer that controls the elapsed time since last user interaction
     */
    private MVYTimer m_disconnectionTimer = null;
    /**
     * 	Time in Miliseconds before no user interaction event is launched
     */ 
    private int m_networkConnectionTimeout = 10000;
    /**
     * 	Boolean that stablishes if there was a running timer before the app
     * 	was sent to background
     */
    private boolean wasTimerON = false;
    /**
     * Boolean that controls if the library can manage the internet connections or not
     */
    private boolean m_manageConnections = false;    
    /**
     * Boolean that controls if the library can manage the connections from background
     */
    private boolean m_manageConnectionsFromBackground = false;
    
    //--------------------------------------------------------------------------
    //    Private constructor 
    //--------------------------------------------------------------------------
    private MVYBatterySaverManager ()
    {
        m_currentConnections = new Connections();
    }
    
    //--------------------------------------------------------------------------
    //  Public methods
    //--------------------------------------------------------------------------
    public static MVYBatterySaverManager getInstance() 
    {
        return instance;
    }
    //--------------------------------------------------------------------------
    public boolean areConnectionsManaged()
    {
    	return m_manageConnections;
    }
    //--------------------------------------------------------------------------
    public void setLibraryToManageConnections(boolean value)
    {
    	m_manageConnections = value;
    }
    //--------------------------------------------------------------------------
    public void setLibraryToOperateFromBackground( boolean value )
    {
    	m_manageConnectionsFromBackground = value;
    }
    //--------------------------------------------------------------------------    
    public boolean isWifiEnabled( Context context )
    {
        return new MVYWifiManager().isWifiConnectionON(context);
    }
    //--------------------------------------------------------------------------
    public boolean is3GEnabled( Context context )
    {
        return new MVY3GManager().is3GConnectionON(context);
    }
    //--------------------------------------------------------------------------
    public int getBatteryLevel( Context context )
    {
        IntentFilter ifilter = new IntentFilter( Intent.ACTION_BATTERY_CHANGED );
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra( BatteryManager.EXTRA_LEVEL, -1);
        
        return level;
    }    
    //--------------------------------------------------------------------------
    public ManagerState getManagerState ()
    {
    	return m_state;
    }
    
    //--------------------------------------------------------------------------
    public void sendAppToBackground()
    {
    	if ( !m_manageConnectionsFromBackground && m_disconnectionTimer!=null ){
        	wasTimerON = true;
        	m_disconnectionTimer.pause();
    	}
    }
    //--------------------------------------------------------------------------
    public void bringAppToFront()
    {
    	if ( !m_manageConnectionsFromBackground && wasTimerON )
    	{
    		wasTimerON = false;
    		if ( m_disconnectionTimer == null )		startUserInactivityTimer();
    		else					    			m_disconnectionTimer.resume();
    	}  
    }    
    //--------------------------------------------------------------------------
    public void enableSendingData (Context context,  MVYBatteryManagerCallback callback) 
    {
        m_Callback = callback;
        enableSendingData(context);
    }
    //--------------------------------------------------------------------------
    public synchronized void enableSendingData (Context context) 
    {
    	setManagerState(ManagerState.gettingAnyConnection);
        m_Context = context;
        
        // stop any previous disconnection timer
        stopUserInactivityTimer();

        // Enable all connection types
        m_currentConnections.gettingWIFIConnection = true;
        m_currentConnections.getting3GConnection = true;	
        
        MVYWifiManager wifiManager = new MVYWifiManager();
        wifiManager.switchON(m_Context, this);
        
        MVY3GManager threeGManager = new MVY3GManager();
        threeGManager.switchON(m_Context, this);
    }
    //--------------------------------------------------------------------------
    /**
     * If the library has control over connections, it closes them all after a period
     * of time without any user interaction
     * 
     * @param timeOut	time in milliseconds without any user activity before closing all connections  
     * @param context	app context
     */
    public void closeAllDataConnectionsWhenNoUserInteractionDuring(int timeOut, Context context) 
    {
    	if ( m_manageConnections )
    	{
            m_networkConnectionTimeout = timeOut;
            m_Context = context;
            startUserInactivityTimer();    		
    	}
    }
    //--------------------------------------------------------------------------
    /**
     *  If library has control over connections, it closes them all immediately
     *  
     *  @param context	app context
     */
    public boolean closeAllDataConnections(Context context) 
    {
        // switch off wifi
        MVYWifiManager wifiManager = new MVYWifiManager();
        wifiManager.switchOFF(context);
        
        // switch off 3g
        MVY3GManager threeGManager = new MVY3GManager();
        threeGManager.switchOFF(context);
     
        setManagerState( ManagerState.IDLE );
        
        return true;
    }
    //--------------------------------------------------------------------------
    /**
     *  If there is an inactivity timer running this methods resets it 
     */
    public void resetUserInactivityTimer()
    {
        if ( m_disconnectionTimer!=null ){
            Log.i("Battery Saver Manager", "Se resetea el timer de desconexion");
            m_disconnectionTimer.reset();
        }
    }
    
    //--------------------------------------------------------------------------
    //    Private methods
    //--------------------------------------------------------------------------
    private void setManagerState (ManagerState newState)
    {
    	if ( newState != m_state ){
        	m_state = newState;
        	Log.i("Battery Saver Manager", "the new Manager state is: " + newState);
    	}
    }
    //--------------------------------------------------------------------------
    /**
     * Start the user inactivity timer to send the noUserinteraction event
     * after m_networkConnectionTimeout miliseconds
     */
    private void startUserInactivityTimer()
    {    
        stopUserInactivityTimer();
        
        m_disconnectionTimer = new MVYTimer( new Runnable() {
			@Override
			public void run() {
				onNoUserInteractionTimeout(m_Context);
			}
        }, m_networkConnectionTimeout);
    }
    //--------------------------------------------------------------------------
    /**
     * Event launched once user inactivity timer expires
     * 
     * @param context
     */
    private void onNoUserInteractionTimeout( Context context )
    {
            Log.i("Battery Saver Manager", "Se cierran todas las conexiones");
            stopUserInactivityTimer();
            closeAllDataConnections(context);
    }
    //--------------------------------------------------------------------------
    private void stopUserInactivityTimer()
    {    
        if ( m_disconnectionTimer!= null )
        {
            m_disconnectionTimer.stop();
            m_disconnectionTimer = null;            
        }
    }
    
    //--------------------------------------------------------------------------
    //    BroadcastReceiver inherit methods
    //--------------------------------------------------------------------------    
    private void onReceiveConnection ( String connectionName  )
    {        
        switch ( m_state ) {
            case gettingAnyConnection:
                
                if ( !m_currentConnections.getting3GConnection && !m_currentConnections.gettingWIFIConnection )
                	setManagerState(ManagerState.IDLE);

                setManagerState(ManagerState.CONNECTED);

                if ( m_Callback != null )
                    m_Callback.onDataSendingEnabled( connectionName );
                break;

            case getting3GConnection:
            case gettingWIfiConnection:
            	setManagerState(ManagerState.IDLE);

                if ( m_Callback != null )
                    m_Callback.onDataSendingEnabled( connectionName );                
                break;
                
            case IDLE:
            default:
                // Do nothing
                break;
        }
    }
    
    //--------------------------------------------------------------------------
    //    MVYWifiManager inherit methods
    //--------------------------------------------------------------------------
    @Override
    public synchronized void onWifiConnectionON( String wifiName ) 
    {
        if ( m_state== ManagerState.gettingAnyConnection )
            m_currentConnections.gettingWIFIConnection = false;
        onReceiveConnection( wifiName );            
    }
    //--------------------------------------------------------------------------    
    @Override
    public synchronized void onWifiConnectionTimeout() 
    {
        if ( m_state== ManagerState.gettingAnyConnection )
        {
            m_currentConnections.gettingWIFIConnection = false;
            if ( !m_currentConnections.getting3GConnection ){
            	setManagerState(ManagerState.IDLE);
                MVY3GManager threeGManager = new MVY3GManager();
                if ( threeGManager.is3GConnectionON(m_Context) )
                    m_Callback.onDataSendingEnabledTimeOut( m_networkConnectionTimeout );
            }
        } 
        else if ( m_state== ManagerState.gettingWIfiConnection && m_Callback != null )
        {
            m_Callback.onDataSendingEnabledTimeOut( m_networkConnectionTimeout );    
        	setManagerState(ManagerState.IDLE);
        }else if (  m_state== ManagerState.CONNECTED ){
        	// Do nothing...
        }else {
        	setManagerState(ManagerState.IDLE);
        }
    }
    
    //--------------------------------------------------------------------------
    //    MVY3GManager inherit methods
    //--------------------------------------------------------------------------    
    @Override
    public synchronized void on3GConnectionON(String type) 
    {
        if ( m_state== ManagerState.gettingAnyConnection ) 
            m_currentConnections.getting3GConnection = false;
        onReceiveConnection( type );        
    }
    //--------------------------------------------------------------------------    
    @Override
    public synchronized void on3GConnectionTimeout() {
        if ( m_state== ManagerState.gettingAnyConnection )
        {
            m_currentConnections.getting3GConnection = false;
            
            if ( !m_currentConnections.gettingWIFIConnection ){
            	setManagerState(ManagerState.IDLE);
                MVYWifiManager wifiManager = new MVYWifiManager();
                if ( !wifiManager.isWifiConnectionON(m_Context) )
                    m_Callback.onDataSendingEnabledTimeOut( m_networkConnectionTimeout );    
            }
        } 
        else if ( m_state== ManagerState.getting3GConnection && m_Callback != null )
        {
            m_Callback.onDataSendingEnabledTimeOut( m_networkConnectionTimeout );
        	setManagerState(ManagerState.IDLE);
        }else if (  m_state== ManagerState.CONNECTED ){
        	// Do nothing...
        }else {
        	setManagerState(ManagerState.IDLE);
        }
    }    
}

