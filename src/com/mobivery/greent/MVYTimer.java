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

import java.util.Date;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.util.Log;

public class MVYTimer extends Timer
{
	private long 		m_timeOut = 0;
	private Runnable	m_task = null;
	private long		m_timeStart = 0;
	private long		m_remainingTime = 0;
	private ScheduledFuture<?> m_timerHandle = null;
	
	public MVYTimer(Runnable runnable, long timeOut) {
		m_task = runnable;
		m_timeOut = timeOut;
		startTimer();
	}
	
	//--------------------------------------------------------------------------
	public void stop()
	{	
		if ( m_timerHandle != null ) {
			m_timerHandle.cancel(true);
		}
	}
	
	//--------------------------------------------------------------------------
	public void reset()
	{
		stop();
		if ( m_task != null )			startTimer();
	}
	//--------------------------------------------------------------------------
	public void pause()
	{
		Date stopDate = new Date();
		long actualTime = stopDate.getTime();
		m_remainingTime = m_timeOut - ( actualTime - m_timeStart); 
		Log.i("MVY Timer","remaining time " + m_remainingTime + " miliseconds");
		stop();
	}
	//--------------------------------------------------------------------------
	public void resume()
	{
		stop();
		if ( m_task != null ){
			if ( m_remainingTime>0 )	startTimer(m_remainingTime);
			else						m_remainingTime = 0;
		}
	}

	//--------------------------------------------------------------------------
	//	Private Methods
	//--------------------------------------------------------------------------
	private void startTimer ()
	{
		startTimer(m_timeOut);
	}
	//--------------------------------------------------------------------------	
	private void startTimer ( long timeOut )
	{
		m_remainingTime = 0;
		
		ScheduledExecutorService m_scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		m_timerHandle = m_scheduledExecutor.schedule(m_task, timeOut, TimeUnit.MILLISECONDS);
		Date startDate = new Date();
		m_timeStart = startDate.getTime();
		Log.i ("MVY Timer", "start the timer with " + timeOut + " miliseconds interval");
	}
}
