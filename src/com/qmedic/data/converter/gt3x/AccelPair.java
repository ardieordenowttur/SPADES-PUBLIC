/******************************************************************************************
 * 
 * Authors:
 *  - Billy, Stanis Laus
 *  - Albinali, Fahd
 *  
 * Company:
 *  - Qmedic
 * 
 ******************************************************************************************/

package com.qmedic.data.converter.gt3x;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class AccelPair {
	
	public short x1;
	public short y1;
	public short z1;
	
	public double gx1;
	public double gy1;
	public double gz1;
	
	public short x2;
	public short y2;
	public short z2;
	
	public double gx2;
	public double gy2;
	public double gz2;

	public AccelPair(final byte[] bytes, final double accelerationScale) {
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
	
	public double writeToFile(final FileWriter writer, double timestamp,final double delta, final boolean inGAcceleration, final boolean withTimestamps) throws IOException {				
		if(withTimestamps) {			
			if (inGAcceleration)
				writer.append(GT3XUtils.SDF.format(new Date(Math.round(timestamp)))+","+GT3XUtils.DF.format(gx1)+","+GT3XUtils.DF.format(gy1)+","+GT3XUtils.DF.format(gz1)+"\n");
			else
				writer.append(GT3XUtils.SDF.format(new Date(Math.round(timestamp)))+","+x1+","+y1+","+z1+"\n");
			timestamp+=delta;		
			
			if (inGAcceleration)
				writer.append(GT3XUtils.SDF.format(new Date(Math.round(timestamp)))+","+GT3XUtils.DF.format(gx2)+","+GT3XUtils.DF.format(gy2)+","+GT3XUtils.DF.format(gz2)+"\n");
			else
				writer.append(GT3XUtils.SDF.format(new Date(Math.round(timestamp)))+","+x2+","+y2+","+z2+"\n");
			timestamp+=delta;
		} else {
			if (inGAcceleration)
				writer.append(GT3XUtils.DF.format(gx1)+","+GT3XUtils.DF.format(gy1)+","+GT3XUtils.DF.format(gz1)+"\n");
			else
				writer.append(x1+","+y1+","+z1+"\n");
			timestamp+=delta;
			if (inGAcceleration)
				writer.append(GT3XUtils.DF.format(gx2)+","+GT3XUtils.DF.format(gy2)+","+GT3XUtils.DF.format(gz2)+"\n");
			else
				writer.append(x2+","+y2+","+z2+"\n");
			timestamp+=delta;
		}
		return timestamp;
	}
}
