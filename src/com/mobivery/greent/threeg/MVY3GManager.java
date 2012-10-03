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
package com.mobivery.greent.threeg;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import com.mobivery.greent.MVYBatterySaverManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MVY3GManager extends BroadcastReceiver    
{
    private MVY3GManagerCallback m_callback = null;
    private Context m_context = null;
    
    /** Timer that controls the time the manager is trying to connnect to a 3G network */
    private Timer m_timeOutTimer = null;
    /**    Maximum miliseconds the manager is trying to connect to a 3G network. */
    private int m_connectionTimeout = 20000;
    
    //--------------------------------------------------------------------------
    //    Public methods
    //--------------------------------------------------------------------------    
    public void set3GConnectionTimeout (int timeout)
    {
        m_connectionTimeout = timeout;
    }
    //--------------------------------------------------------------------------    
    public boolean is3GConnectionON( Context context )
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
    
        if ( activeNetInfo!= null && activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE)
            return true;
        
        return false;
    }    

    //--------------------------------------------------------------------------
    public String get3GConnectionName( Context context )
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        
        if (activeNetInfo!= null )
            return activeNetInfo.getSubtypeName();
        return null;
    }
    
    //--------------------------------------------------------------------------
    public void switchOFF (Context context) 
    {
    	if ( MVYBatterySaverManager.getInstance().areConnectionsManaged() )
    		switch3GState (context, false );
    }

    //--------------------------------------------------------------------------
    public void switchON (Context context, MVY3GManagerCallback callback) 
    {
        m_callback = callback;
        m_context = context;
        
        if ( is3GConnectionON(m_context) )
        {
            m_callback.on3GConnectionON( get3GConnectionName(m_context) );
        } 
        else if ( MVYBatterySaverManager.getInstance().areConnectionsManaged() ) 
        {
            // Register to connection event 
            IntentFilter intentFilter = new IntentFilter( );
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION );
            context.registerReceiver (this, intentFilter);
            
            switch3GState (context, true );
            
            // start timeout timer
            m_timeOutTimer = new Timer();
            final Handler handler = new Handler();
            m_timeOutTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            switchOFF(m_context);
                            m_callback.on3GConnectionTimeout();
                            finish3GManager();
                        }
                    });
                }
            }, m_connectionTimeout);                                    
        } 
        else 
        {
        	m_callback.on3GConnectionTimeout();	
        }
    }
    
    //--------------------------------------------------------------------------
    void switch3GState(Context context, boolean enable)
    {
       int currentapiVersion = android.os.Build.VERSION.SDK_INT;
       if( currentapiVersion <= Build.VERSION_CODES.FROYO )
       {
           Log.i("version:", "Found Froyo");
           try{ 
               Method dataConnSwitchmethod;
               Class telephonyManagerClass;
               Object ITelephonyStub;
               Class ITelephonyClass;
               TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    
               telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
               Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
               getITelephonyMethod.setAccessible(true);
               ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
               ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());
               if ( enable ) {
                   dataConnSwitchmethod = ITelephonyClass.getDeclaredMethod("enableDataConnectivity"); 
               } else {
                   dataConnSwitchmethod =  ITelephonyClass.getDeclaredMethod("disableDataConnectivity");
               }
               dataConnSwitchmethod.setAccessible(true);
               dataConnSwitchmethod.invoke(ITelephonyStub);
           }catch(Exception e){
                  Log.e("Error:",e.toString());
           }
       } else {
           Log.i("version:", "Found Gingerbread+");
           
           try {
               final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
               final Class conmanClass = Class.forName(conman.getClass().getName());
               final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
               iConnectivityManagerField.setAccessible(true);
               final Object iConnectivityManager = iConnectivityManagerField.get(conman);
               final Class iConnectivityManagerClass =   Class.forName(iConnectivityManager.getClass().getName());
               final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
               setMobileDataEnabledMethod.setAccessible(true);
               setMobileDataEnabledMethod.invoke(iConnectivityManager, enable);
           } catch (Exception e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
           }
       }
    }

    //--------------------------------------------------------------------------
    //    Private methods
    //--------------------------------------------------------------------------
    private void finish3GManager()
    {
        m_context.unregisterReceiver( MVY3GManager.this );
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
    //    BroadcastReceiver inherit methods
    //--------------------------------------------------------------------------
    @Override
    public synchronized void onReceive(Context context, Intent intent) 
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService( Context.CONNECTIVITY_SERVICE );
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
    
        if ( activeNetInfo!= null && activeNetInfo.getType() == ConnectivityManager.TYPE_MOBILE)
        {    
            killTimeoutTimer();            
            m_callback.on3GConnectionON( activeNetInfo.getSubtypeName() );
            finish3GManager();
        }
    }
}
