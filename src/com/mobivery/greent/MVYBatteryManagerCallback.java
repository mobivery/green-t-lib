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

public interface MVYBatteryManagerCallback
{
	/**
	 *
	 * @param connectionType
	 */
    public void onDataSendingEnabled(String connectionType);

    /**
     *
     * @param timeOut
     */
    public void onDataSendingEnabledTimeOut(float timeOut); 
}
