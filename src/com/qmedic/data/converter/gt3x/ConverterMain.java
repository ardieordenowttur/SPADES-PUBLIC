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

package com.qmedic.data.converter.gt3x;

public class ConverterMain {

	public static void main(String[] args) {

		// Command line example: java -jar GT3XParser.jar GT3XParser/sample-data/v1/sample1.gt3x home/user/Development/csv/ G_VALUE WITH_TIMESTAMP SPLIT MHEALTH
		if (args.length!=8){
			System.out.println("java -jar GT3XParser.jar [INPUT GT3X FILE] [OUTPUT CSV DIRECTORYPATH] [G_VALUE/ADC_VALUE] [WITH_TIMESTAMP/WITHOUT_TIMESTAMP] [SPLIT/NO_SPLIT] [MHEALTH/ACTIGRAPH] [SUMMARY_ON/SUMMARY_OFF] [DEBUG_ON/DEBUG_OFF]");
			return;
		}
		
		// Try to process the file
		ConverterWorker cw = new ConverterWorker(args);
		cw.processFile();
		
	}
}
