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
 *  - Albinali, Fahd
 * 
 ******************************************************************************************/

package com.qmedic.data.converter.gt3x;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import com.qmedic.data.converter.gt3x.base.OutFileWriter;
import com.qmedic.data.converter.gt3x.model.AccelDataPoint;

public class ActivityCountSummaryCreator extends OutFileWriter {
	
	private long _prevMinuteTs = 0;
	private double _totalSoFar = 0d;

	public ActivityCountSummaryCreator() {
		this._prevMinuteTs = 0;
		this._totalSoFar = 0d;
	}
	
	public void processNewAccelData(final BufferedWriter writer, final long timestamp, final AccelDataPoint data, final SimpleDateFormat sdf) throws IOException {
		long currMinuteTs = timestamp / 60000 * 60000;
		if(currMinuteTs != _prevMinuteTs) {
			if(_prevMinuteTs != 0) {
				writer.append(sdf.format((long)_prevMinuteTs));
				writer.append(',');
				writer.append(Integer.toString((int)_totalSoFar));
				writer.append('\n');
				_totalSoFar = 0d;
			}
			_prevMinuteTs = currMinuteTs;
		}
		_totalSoFar += Math.sqrt(data.x()*data.x() + data.y()*data.y() + data.z()*data.z());
	}
	
	public double getTotalSoFar() {
		return _totalSoFar;
	}
	
	public double getPreviousMinuteTimestamp() {
		return _prevMinuteTs;
	}
	
}
