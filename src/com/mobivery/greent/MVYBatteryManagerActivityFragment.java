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

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;

public class MVYBatteryManagerActivityFragment extends FragmentActivity
{
	private boolean m_isSendingToBackground = false;
	
	//--------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
    }
	//--------------------------------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
	//--------------------------------------------------------------------------
    @Override
    protected void onPause() {
        super.onPause();
        boolean appSentToBackground = MVYBatterySaverManager.getInstance().areConnectionsManaged() && 
        		isApplicationBroughtToBackground(this);
        if ( appSentToBackground )
        	MVYBatterySaverManager.getInstance().sendAppToBackground();
    }
	//--------------------------------------------------------------------------   
    @Override
    protected void onResume() {
        super.onResume();
        boolean appComesFromBackground = MVYBatterySaverManager.getInstance().areConnectionsManaged() && 
        		!m_isSendingToBackground;
        if ( appComesFromBackground )
        	MVYBatterySaverManager.getInstance().bringAppToFront();
    }
	//--------------------------------------------------------------------------   
    @Override
    public void onUserInteraction(){
        super.onUserInteraction();
        
        boolean userInteraction = MVYBatterySaverManager.getInstance().areConnectionsManaged() && 
        		!isApplicationBroughtToBackground(this) && !m_isSendingToBackground;
        if ( userInteraction )
        	MVYBatterySaverManager.getInstance().resetUserInactivityTimer();
    }
    
	//--------------------------------------------------------------------------
    @Override
    public boolean dispatchKeyEvent (KeyEvent event) 
    {    	
    	if ( MVYBatterySaverManager.getInstance().areConnectionsManaged() ){
	        if (event.getAction()==KeyEvent.ACTION_DOWN ) {
	            switch (event.getKeyCode()) {
	            	case KeyEvent.KEYCODE_BACK:
                		m_isSendingToBackground = true;
                		MVYBatterySaverManager.getInstance().sendAppToBackground();            			
	            		break;
	            }
	        } 
    	}
        return super.dispatchKeyEvent(event);
    }
    
	//--------------------------------------------------------------------------
	public static boolean isApplicationBroughtToBackground(final Activity activity) 
    {
        ActivityManager activityManager = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(1);

        // Check the top Activity against the list of Activities contained in the Application's package.
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            try {
                PackageInfo pi = activity.getPackageManager().getPackageInfo(activity.getPackageName(), PackageManager.GET_ACTIVITIES);
                for (ActivityInfo activityInfo : pi.activities) {
                    if(topActivity.getClassName().equals(activityInfo.name))
                        return false;
                }
            } catch( PackageManager.NameNotFoundException e) {
                return false; // Never happens.
            }
        }
        return true;
    }
}
