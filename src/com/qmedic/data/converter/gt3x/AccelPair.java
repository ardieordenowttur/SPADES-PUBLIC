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
import com.qmedic.data.converter.gt3x.enums.GT3XParserOutputDataType;
import com.qmedic.data.converter.gt3x.model.AccelDataPoint;
import com.qmedic.data.converter.gt3x.model.AccelPairData;
import com.qmedic.data.converter.gt3x.utils.TimestampHelper;

public class AccelPair extends OutFileWriter {

	public TimestampHelper tsHelper;
	
	private boolean _inGAcceleration;
	private boolean _withTimestamps;
	private GT3XParserOutputDataType _outputDataType = GT3XParserOutputDataType.MHEALTH;
	
	private short x1;
	private short y1;
	private short z1;
	
	private double gx1;
	private double gy1;
	private double gz1;
	
	private short x2;
	private short y2;
	private short z2;
	
	private double gx2;
	private double gy2;
	private double gz2;

	public AccelPair(
			final boolean inGAcceleration,
			final boolean withTimestamps,
			final int samplingRate,
			final GT3XParserOutputDataType outputDataType
			) {
		this._inGAcceleration = inGAcceleration;
		this._withTimestamps = withTimestamps;
		this._outputDataType = outputDataType;
		this.tsHelper = new TimestampHelper(1000, samplingRate);
	}
	
	public void setAccelPair(final byte[] bytes, final double accelerationScale) {
		/* 
		 * Expect two 36-bits data (72 bits = 9 bytes)
		 * Since each second of raw activity sample is packed into 12-bit values 
		 * (in YXZ order), a single 3-axis sample takes up to 36 bits of data
		 * (12 bits per axis), ie. 4.5 bytes, which can't be read easily. To
		 * remedy this, we read two 3-axis sample at a time instead, which takes 
		 * up to 72 bits = 9 bytes, which can be read easily.
		 */
		
		if(bytes.length == 9) {
			
			int datum = 0;
			datum=(bytes[0]&0xff);datum=datum<<4;datum|=(bytes[1]&0xff)>>>4;
			this.y1=(short)datum;						
			if (y1>2047)
				y1+=61440;

			datum=bytes[1]&0x0F;datum=datum<<8;datum|=(bytes[2]&0xff);						
			this.x1=(short)datum;						
			if (x1>2047)
				x1+=61440;

			datum=bytes[3]&0xff;datum=datum<<4;datum|=(bytes[4]&0xff)>>>4;
			this.z1=(short)datum;						
			if (z1>2047)
				z1+=61440;

			datum=bytes[4]&0x0F;datum=datum<<8;datum|=(bytes[5]&0xff);
			this.y2=(short) datum;
			if (y2>2047)
				y2+=61440;

			datum=(bytes[6]&0xff);datum=datum<<4;datum|=(bytes[7]&0xff)>>>4;						
			this.x2=(short)datum;
			if (x2>2047)
				x2+=61440;

			datum=bytes[7]&0x0F;datum=datum<<8;datum|=(bytes[8]&0xff);
			this.z2=(short)datum;
			if (z2>2047)
				z2+=61440;
			
			this.gx1=x1/accelerationScale;
			this.gy1=y1/accelerationScale;
			this.gz1=z1/accelerationScale;

			this.gx2=x2/accelerationScale;
			this.gy2=y2/accelerationScale;
			this.gz2=z2/accelerationScale;
		}
	}
	
	public AccelPairData writeToFile(final BufferedWriter writer, double timestamp, final SimpleDateFormat sdf) throws IOException {	
		AccelDataPoint data1;
		AccelDataPoint data2;
		if(_withTimestamps) {			
			if (_inGAcceleration) {
				writer.append(sdf.format((long)timestamp));
				writer.append(',');
				writer.append(formatTo3Decimals(gx1));
				writer.append(',');
				writer.append(formatTo3Decimals(gy1));
				writer.append(',');
				writer.append(formatTo3Decimals(gz1));
				if(_outputDataType == GT3XParserOutputDataType.ACTIGRAPH) writer.append('\r');
				writer.append('\n');
				data1 = new AccelDataPoint(gx1,gy1,gz1);
			} else {
				writer.append(sdf.format((long)timestamp));
				writer.append(',');
				writer.append(formatTo3Decimals(x1));
				writer.append(',');
				writer.append(formatTo3Decimals(y1));
				writer.append(',');
				writer.append(formatTo3Decimals(z1));
				if(_outputDataType == GT3XParserOutputDataType.ACTIGRAPH) writer.append('\r');
				writer.append('\n');
				data1 = new AccelDataPoint(x1,y1,z1);
			}

			timestamp += tsHelper.Next();
			
			if (_inGAcceleration) {
				writer.append(sdf.format((long)timestamp));
				writer.append(',');
				writer.append(formatTo3Decimals(gx2));
				writer.append(',');
				writer.append(formatTo3Decimals(gy2));
				writer.append(',');
				writer.append(formatTo3Decimals(gz2));
				if(_outputDataType == GT3XParserOutputDataType.ACTIGRAPH) writer.append('\r');
				writer.append('\n');
				data2 = new AccelDataPoint(gx2,gy2,gz2);
			} else {
				writer.append(sdf.format((long)timestamp));
				writer.append(',');
				writer.append(formatTo3Decimals(x2));
				writer.append(',');
				writer.append(formatTo3Decimals(y2));
				writer.append(',');
				writer.append(formatTo3Decimals(z2));
				if(_outputDataType == GT3XParserOutputDataType.ACTIGRAPH) writer.append('\r');
				writer.append('\n');
				data2 = new AccelDataPoint(x2,y2,z2);
			}

			timestamp += tsHelper.Next();

		} else {
			if (_inGAcceleration) {
				writer.append(formatTo3Decimals(gx1));
				writer.append(',');
				writer.append(formatTo3Decimals(gy1));
				writer.append(',');
				writer.append(formatTo3Decimals(gz1));
				if(_outputDataType == GT3XParserOutputDataType.ACTIGRAPH) writer.append('\r');
				writer.append('\n');
				data1 = new AccelDataPoint(gx1,gy1,gz1);
			} else {
				writer.append(formatTo3Decimals(x1));
				writer.append(',');
				writer.append(formatTo3Decimals(y1));
				writer.append(',');
				writer.append(formatTo3Decimals(z1));
				if(_outputDataType == GT3XParserOutputDataType.ACTIGRAPH) writer.append('\r');
				writer.append('\n');
				data1 = new AccelDataPoint(x1,y1,z1);
			}

			timestamp += tsHelper.Next();
			
			if (_inGAcceleration) {
				writer.append(formatTo3Decimals(gx2));
				writer.append(',');
				writer.append(formatTo3Decimals(gy2));
				writer.append(',');
				writer.append(formatTo3Decimals(gz2));
				if(_outputDataType == GT3XParserOutputDataType.ACTIGRAPH) writer.append('\r');
				writer.append('\n');
				data2 = new AccelDataPoint(gx2,gy2,gz2);
			} else {
				writer.append(formatTo3Decimals(x2));
				writer.append(',');
				writer.append(formatTo3Decimals(y2));
				writer.append(',');
				writer.append(formatTo3Decimals(z2));
				if(_outputDataType == GT3XParserOutputDataType.ACTIGRAPH) writer.append('\r');
				writer.append('\n');
				data2 = new AccelDataPoint(x2,y2,z2);
			}
	
			timestamp += tsHelper.Next();
			
		}
		return new AccelPairData(timestamp, data1, data2);
	}
	
	public double advanceTimestampHelper() {
		long ts = tsHelper.Next();
		return ts;
	}
	
	public String getLastRecordedXYZ(final double accelerationScale) {
		StringBuilder sb = new StringBuilder();
		if(_inGAcceleration) {
			sb.append(formatTo3Decimals(x2/accelerationScale));
			sb.append(',');
			sb.append(formatTo3Decimals(y2/accelerationScale));
			sb.append(',');
			sb.append(formatTo3Decimals(z2/accelerationScale));			
		} else {
			sb.append(x2/accelerationScale);
			sb.append(',');
			sb.append(y2/accelerationScale);
			sb.append(',');
			sb.append(z2/accelerationScale);
		}
		return sb.toString();
	}
	
	public AccelDataPoint getLastRecordXYZ(final double accelerationScale) {
		if(_inGAcceleration) {
			return new AccelDataPoint(x2/accelerationScale, y2/accelerationScale, z2/accelerationScale);
		} else {
			return new AccelDataPoint(x2, y2, z2);
		}
	}
}
