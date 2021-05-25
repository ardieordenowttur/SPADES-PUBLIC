/******************************************************************************************
 * 
 * Copyright (c) 2015 EveryFit, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * Authors:
 *  - Billy, Stanis Laus
 *  - Rao, Zhibiao
 * 
 ******************************************************************************************/

package com.qmedic.data.converter.gt3x;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.qmedic.data.converter.gt3x.enums.DeviceType;
import com.qmedic.data.converter.gt3x.enums.DeviceVersion;

public class GT3XUtils {
	
	public static final String MHEALTH_TIMESTAMP_DATA_FORMAT = "yyyy/MM/dd HH:mm:ss.SSS"; // the format of the data timestamp
	public static final String MHEALTH_TIMESTAMP_FILE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"; // the format of the file timestamp
	public static final String MHEALTH_TIMEZONE_FILE_FORMAT = "Z"; // the format of the timezone (UTC offset)
	public static final String MHEALTH_DECIMAL_FORMAT = "0.000";
	
	public static final long MILLIS_IN_HOUR = 3600000L;
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	private static final long TICKS_AT_EPOCH = 621355968000000000L;		
	private static final long TICKS_PER_MILLISECOND = 10000;

	
	/*
	 * Fix timestamp to make the milliseconds uniformed across all minutes
	 */
	public static double FixTimeStamp(double timestamp, double delta,boolean yes)
	{		
		long tenTimesTS = (long) (timestamp*100);	
		double mmseconds;
		if (yes){
			mmseconds = (tenTimesTS - (tenTimesTS/100000)*100000)/100 + delta;  // get milliseconds
		}else{
			mmseconds = (tenTimesTS - (tenTimesTS/100000)*100000)/100;  // get milliseconds
		}	
		//
		if (mmseconds >= 1000.0 - Math.round(delta/2)){
			mmseconds = 1000;
		}else {
			mmseconds = Math.round(mmseconds/delta)*delta;
		}
		
		return (tenTimesTS/100000)*1000 + Math.round(mmseconds);
	}
	
	/*
	 * Helper method that converts .NET ticks that Actigraph uses to millisecond (UTC)
	 */
	public static long fromTickToUTCMillisecond(final long ticks)
	{		
		Date date = new Date((ticks - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND);
		TimeZone utc = TimeZone.getTimeZone("UTC");
		Calendar calendar = Calendar.getInstance(utc);
		calendar.setTime(date);
		return calendar.getTimeInMillis();
	}
	
	
	/*
	 * Helper method that converts .NET ticks that Actigraph uses to millisecond (local)
	 */
	public static long fromTickToMillisecond(final long ticks)
	{		
		Date date = new Date((ticks - TICKS_AT_EPOCH) / TICKS_PER_MILLISECOND);
		return date.getTime();
	}
	
	
	/*
	 * Helper method to find timezone offset
	 */
	public static String getTimeZone(final long dateInMillis) {
    	SimpleDateFormat sdfTz = new SimpleDateFormat(MHEALTH_TIMEZONE_FILE_FORMAT);
        return sdfTz.format(new Date(dateInMillis));
	}
	
	
	/*
	 * Helper method to find convert a timezone offset into its mHealth counterpart
	 */
	public static String getTimeZoneMHealth(final long dateInMillis) {
		StringBuilder ret = new StringBuilder(getTimeZone(dateInMillis));
		if(ret.charAt(0)=='+') {
			ret.setCharAt(0, 'P');
		} else {
			ret.setCharAt(0, 'M');
		}
		return ret.toString();
	}
	
	/*
	 * Helper method to convert an actigraph timezone string into its mHealth counterpart
	 * Sample inputs: "-04:00:00"
	 * Sample output: "M0400"
	 */
	public static String getTimeZoneMHealthFromActigraph(final String timezoneString) {
		StringBuilder ret = new StringBuilder();
		String[] tokens = timezoneString.split(":");
		if(tokens.length==3) {
			ret.append(tokens[0]);
			ret.append(tokens[1]);
			if(ret.charAt(0)=='+') {
				ret.setCharAt(0, 'P');
			} else if(ret.charAt(0)=='-'){
				ret.setCharAt(0, 'M');
			} else {
				// Reset/empty the stringbuilder if not a valid timezone
				ret.setLength(0);
			}
		}
		return ret.toString();
	}

	
	/*
	 * Helper method that displays  a byte array as a hexadecimal string
	 */
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	
	/*
	 * Helper method that displays a byte as a hexadeciman string
	 */
	public static String byteToHex(byte byyte) {
		char[] hexChars = new char[2];
		int v = byyte & 0xFF;
		hexChars[0] = hexArray[v >>> 4];
		hexChars[1] = hexArray[v & 0x0F];
		return new String(hexChars);
	}

	
	/*
	 * Helper methods to classify which version the device falls under
	 * ------------------------------------------------------------------
	 * Version - SerialNumber StartsWith - DeviceName - Firmware Version 
	 * ------------------------------------------------------------------
	 * V1 - NEO - GT3X+ - 2.5.0 and below
	 *    - MRA - ActiSleep+ - 2.5.0 and below
	 * V2 - NEO - GT3X+ - 3.0 and up
	 *    - MRA - ActiSleep+ - 3.0 and up
	 *    - CLE - wGT3X+ - all
	 *    - MOS0 - wGT3X-BT - all
	 *    - MOS2 - wGT3X-BT - all
	 *    - MOS3 - wActiSleep+ - all
	 *    - MOS4 - wActiSleepBT - all
	 *    - TAS - GT9X Link - all
	 */
	public static DeviceVersion getDeviceVersion(final String serialNumber, final String firmware) {
		DeviceVersion deviceVersion = DeviceVersion.UNKNOWN;
		if(!serialNumber.isEmpty() && !firmware.isEmpty()) {
			if(serialNumber.startsWith("NEO")
					|| serialNumber.startsWith("MRA")) {
				String[] tokens = firmware.split("\\.");				
				if(Integer.parseInt(tokens[0]) > 2) {
					// firmware 3.x
					deviceVersion = DeviceVersion.V2;
				} else {
					if(Integer.parseInt(tokens[0]) == 2) {
						if(Integer.parseInt(tokens[1]) > 6) {
							// firmware greater than 2.5.x
							deviceVersion = DeviceVersion.V2;
						} else {
							deviceVersion = DeviceVersion.V1;
						}						
					} else {
						deviceVersion = DeviceVersion.V1;
					}
				}
			} else if(serialNumber.startsWith("CLE") 
					|| serialNumber.startsWith("MOS0")
					|| serialNumber.startsWith("MOS2")
					|| serialNumber.startsWith("MOS3")
					|| serialNumber.startsWith("MOS4")
					|| serialNumber.startsWith("TAS")) {
				deviceVersion = DeviceVersion.V2;
			}
		}
		return deviceVersion;
	}
	
	public static DeviceType getDeviceType(final String serialNumber) {
		DeviceType deviceType = DeviceType.UNKNOWN;
		if(serialNumber.startsWith("NEO")) {
			deviceType = DeviceType.GT3XPLUS;
		} else if(serialNumber.startsWith("MRA")) {
			deviceType = DeviceType.ACTISLEEPPLUS;
		} else if(serialNumber.startsWith("CLE")) {
			deviceType = DeviceType.WGT3XPLUS;
		} else if(serialNumber.startsWith("MOS0")) {
			deviceType = DeviceType.WGT3XBT;
		} else if(serialNumber.startsWith("MOS2")) {
			deviceType = DeviceType.WGT3XBT;
		} else if(serialNumber.startsWith("MOS3")) {
			deviceType = DeviceType.WACTISLEEPPLUS;
		} else if(serialNumber.startsWith("MOS4")) {
			deviceType = DeviceType.WACTISLEEPBT;
		} else if(serialNumber.startsWith("TAS")) {
			deviceType = DeviceType.GT9XLINK;
		}
		return deviceType;
	}
	
	
	/*
	 * Helper method to calculate checksum.
	 * Only for V2 devices using the new LogRecord format.
	 */
	public static byte calculateCheckSum(final LogRecord record, byte chkSum) {
		chkSum ^= record.getType();
		chkSum ^= (byte)(record.getTimestamp() & 0xFF);
		chkSum ^= (byte)((record.getTimestamp()>>>8) & 0xFF);
		chkSum ^= (byte)((record.getTimestamp()>>>16) & 0xFF);
		chkSum ^= (byte)((record.getTimestamp()>>>24) & 0xFF);
		chkSum ^= (byte)(record.getPayloadSize() & 0xFF);
		chkSum ^= (byte)((record.getPayloadSize()>>>8) & 0xFF);
		for(int j=0; j < record.getPayload().length; j++) {
			chkSum ^= record.getPayload()[j];
		}
		return (byte)~chkSum;
	}
	
	/*
	 * Helper method to calculate the (UTC) timestamp of the current hour (24-hour format)
	 */
	public static long getCurrentHourTimestamp(final double timestamp) {
		return (Math.round(timestamp)/MILLIS_IN_HOUR)*MILLIS_IN_HOUR;
	}
	
	/*
	 * Helper method to construct mHealth filename
	 */
	public static String getMHealthFileName(final long timestamp, final String sensorType, final String serialNumber, final String timezoneOffset) {
        return sensorType + "." + serialNumber + "." + simpleDateFormatObject(MHEALTH_TIMESTAMP_FILE_FORMAT).format(new Date(timestamp)) + "-" + timezoneOffset + ".csv";
	}
	
    /*
     * Helper method to create a SimpleDateFormat object
     */
    public static SimpleDateFormat simpleDateFormatObject(final String formatPattern) {
    	SimpleDateFormat sdf = new SimpleDateFormat(formatPattern);
    	return sdf;
    }
    
    /*
     * Helper method to create a DecimalFormat object
     */
    public static DecimalFormat decimalFormatObject() {
    	DecimalFormat df = new DecimalFormat(MHEALTH_DECIMAL_FORMAT);
    	df.setRoundingMode(RoundingMode.HALF_UP);
    	return df;
    }
}
