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

package com.qmedic.data.converter.gt3x.base;

import java.io.BufferedWriter;
import java.io.IOException;

public class OutFileWriter {

	public OutFileWriter() {}
	
	protected String formatTo3Decimals(double d) {
		StringBuilder sb = new StringBuilder();
		
		if (d < 0) {
	    	sb.append('-');
	        d = -d;
	    }
	    long scaled = (long) (d * 1e3 + 0.5);
	    long factor = 1000;
	    int scale = 4;
	    while (factor * 10 <= scaled) {
	        factor *= 10;
	        scale++;
	    }
	    long c,cn,cnn;
	    while (scale > 3) {
	        c = scaled / factor % 10;
	        factor /= 10;
	        sb.append((char) ('0' + c));
	        scale--;
	    }
	    c = scaled / factor % 10;
	    factor /= 10;
	    cn = scaled / factor % 10;
        factor /= 10;
        cnn = scaled / factor % 10;
        factor /= 10;
        
        
        if ((c!=0)||(cn!=0)||(cnn!=0))
	    	sb.append('.');
        else
        	return sb.toString();
        
    	sb.append((char) ('0' + c));
    	
    	if ((cn!=0)||(cnn!=0))
    		sb.append((char) ('0' + cn));
    	else
    		return sb.toString();
    	
    	if (cnn!=0)
    		sb.append((char) ('0' + cnn));
		
		return sb.toString();
	}
	
	protected void appendTo3Decimals(final BufferedWriter writer, double d) throws IOException {
	    if (d < 0) {
	    	writer.append('-');
	        d = -d;
	    }
	    long scaled = (long) (d * 1e3 + 0.5);
	    long factor = 1000;
	    int scale = 4;
	    while (factor * 10 <= scaled) {
	        factor *= 10;
	        scale++;
	    }
	    long c,cn,cnn;
	    while (scale > 3) {
	        c = scaled / factor % 10;
	        factor /= 10;
	        writer.append((char) ('0' + c));
	        scale--;
	    }
	    c = scaled / factor % 10;
	    factor /= 10;
	    cn = scaled / factor % 10;
        factor /= 10;
        cnn = scaled / factor % 10;
        factor /= 10;
        
        
        if ((c!=0)||(cn!=0)||(cnn!=0))
	    	writer.append('.');
        else
        	return;
        
    	writer.append((char) ('0' + c));
    	
    	if ((cn!=0)||(cnn!=0))
    		writer.append((char) ('0' + cn));
    	else
    		return;
    	
    	if (cnn!=0)
    		writer.append((char) ('0' + cnn));
	}
	
}
