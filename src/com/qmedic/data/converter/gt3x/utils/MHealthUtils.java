/******************************************************************************************
 * 
 * Copyright (c) 2016 EveryFit, Inc.
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
 * 
 ******************************************************************************************/

package com.qmedic.data.converter.gt3x.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.qmedic.data.converter.gt3x.enums.GT3XParserOutputDataType;

public class MHealthUtils {
	
	public static final String MHEALTH_TIMESTAMP_DATA_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"; // the format of the data timestamp
	public static final String ACTIGRAPH_TIMESTAMP_DATA_FORMAT = "M/d/yyyy HH:mm:ss.SSS"; // the format of the data timestamp (actigraph data format)
	public static final String MHEALTH_TIMESTAMP_FILE_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"; // the format of the file timestamp
	public static final String MHEALTH_TIMEZONE_FILE_FORMAT = "Z"; // the format of the timezone (UTC offset)
	
	private SimpleDateFormat _sdfFile;
	private SimpleDateFormat _sdfFileTz;
	private SimpleDateFormat _sdfData;
	
	public MHealthUtils(final GT3XParserOutputDataType outputDataType) {
		this._sdfFile = new SimpleDateFormat(MHEALTH_TIMESTAMP_FILE_FORMAT);
		_sdfFile.setTimeZone(TimeZone.getTimeZone("UTC")); // Data is in UTC
		
		this._sdfFileTz = new SimpleDateFormat(MHEALTH_TIMEZONE_FILE_FORMAT);
		
		String outputDataFormat = (outputDataType == GT3XParserOutputDataType.ACTIGRAPH) ? ACTIGRAPH_TIMESTAMP_DATA_FORMAT : MHEALTH_TIMESTAMP_DATA_FORMAT;
		this._sdfData = new SimpleDateFormat(outputDataFormat);
		_sdfData.setTimeZone(TimeZone.getTimeZone("UTC")); // Data is in UTC
	}

	public SimpleDateFormat dataSimpleDateFormat() {
		return this._sdfData;
	}

	/*
	 * Helper method to find timezone offset
	 */
	public String getTimeZone(final long dateInMillis) {
		return _sdfFileTz.format(new Date(dateInMillis));
	}

	/*
	 * Helper method to construct mHealth filename
	 */
	public String getMHealthFileName(final long timestamp, final String deviceType, final String firmware, final String serialNumber, final String timezoneOffset) {
		StringBuilder sb = new StringBuilder();
		sb.append(deviceType);
		sb.append('-');
		sb.append("AccelerationCalibrated");
		sb.append('-');
		sb.append(firmware.replaceAll("\\.", "x"));
		sb.append('.');
		sb.append(serialNumber);
		sb.append('.');
		sb.append(_sdfFile.format(new Date(timestamp)));
		sb.append('-');
		sb.append(timezoneOffset);
		sb.append(".sensor.csv");
		return sb.toString();
	}
	
	/*
	 * Helper method to create ActivityCount mHealth filename for its AccelerationCalibrated counterpart
	 * Replaces "-AccelerationCalibrated" by "-ActivityCount" in the first token,
	 * and adds "-ActivityCount" to the second token if it doesn't exist
	 */
	public String getActivityCountMHealthFileName(final String accelerationCalibratedFileName) {
		StringBuilder sb = new StringBuilder();
		String[] tokens = accelerationCalibratedFileName.split("\\.");
		if(tokens.length > 2) {
			for(int i = 0; i<tokens.length; i++) {
				sb.append((i==0) ? "" : '.');
				if(i==0 && tokens[i].contains("-AccelerationCalibrated-")) {
					sb.append(tokens[i].replace("-AccelerationCalibrated-", "-ActivityCount-"));	
				} else {
					sb.append(tokens[i]);
				}
				sb.append((i==1 && !tokens[i].contains("ActivityCount")) ? "-ActivityCount" : "");
			}
		}
		return sb.toString();
	}

	/*
	 * Helper method to find convert a timezone offset into its mHealth counterpart
	 */
	public String getTimeZoneMHealth(final long dateInMillis) {
		StringBuilder ret = new StringBuilder(getTimeZone(dateInMillis));
		if(ret.charAt(0)=='+') {
			ret.setCharAt(0, 'P');
		} else {
			ret.setCharAt(0, 'M');
		}
		return ret.toString();
	}
	
	/*
	 * Helper method to convert milliseconds to mHealth data timestamp
	 */
	public String convertToMHealthDataTimestamp(final long timestamp) {
		return _sdfData.format(new Date(timestamp));
	}

	/*
	 * Helper method to convert an actigraph timezone string into its mHealth counterpart
	 * Sample inputs: "-04:00:00"
	 * Sample output: "M0400"
	 */
	public String getTimeZoneMHealthFromActigraph(final String timezoneString) {
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
}
