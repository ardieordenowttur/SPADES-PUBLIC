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
 *  - Albinali, Fahd
 * 
 ******************************************************************************************/

package com.qmedic.data.converter.gt3x;

import java.io.File;
import java.io.IOException;

public class ConverterMain {
	
	public static void main(String[] args) {

		// Command line example: java -jar GT3XParser.jar GT3XParser/sample-data/v1/sample1.gt3x home/user/Development/csv/ G_VALUE WITH_TIMESTAMP SPLIT
		
		if (args.length!=5){
			System.out.println("java -jar GT3XParser.jar [INPUT GT3X FILE] [OUTPUT CSV DIRECTORYPATH] [G_VALUE/ADC_VALUE] [WITH_TIMESTAMP/WITHOUT_TIMESTAMP] [SPLIT/NO_SPLIT]");
			return;
		}

		if ( (!args[2].equals("G_VALUE")) && (!args[2].equals("ADC_VALUE")) ){
			System.out.println("Usage: Incorrect output value option. Use G_VALUE or ADC_VALUE.");
			return;
		}

		if ( (!args[3].equals("WITHOUT_TIMESTAMP")) && (!args[3].equals("WITH_TIMESTAMP")) ){
			System.out.println("Usage: Incorrect timestamp option. Use WITH_TIMESTAMP or WITHOUT_TIMESTAMP.");
			return;
		}
		
		if ( (!args[4].equals("NO_SPLIT")) && (!args[4].equals("SPLIT")) ){
			System.out.println("Usage: Incorrect mHealth output split option. Use NO_SPLIT or SPLIT.");
			return;
		}


		try{
			if (GT3XFile.isGT3XV1(new File(args[0]))==false &&
					GT3XFile.isGT3XV2(new File(args[0]))==false){
				System.out.println("Error: "+args[0]+" not a valid GT3X file.");
				return;
			}
		}
		catch(IOException e){
			System.out.println("Error: "+args[0]+" not a valid GT3X file.");
			return;
		}
		
		// TODO If LOG_PARAMETER record is preset, then the ACCEL_SCALE (in info.txt?) value should be used.

		try {

			// Convert a file from GT3X to CSV file with/without timestamps and in g values or raw ADC
			boolean inGAcceleration=((args[2].equals("G_VALUE"))?true:false);
			boolean withTimestamp=((args[3].equals("WITH_TIMESTAMP"))?true:false);
			
			// Specify whether mHealth output file should be split into hourly files or not
			boolean mHealthSplit=((args[4].equals("SPLIT"))?true:false);
			
			GT3XFile f=GT3XFile.fromGT3XToCSV(new File(args[0]),args[1],inGAcceleration,withTimestamp,mHealthSplit);
			if (f!=null){
				System.out.println(f.toString());
			}else{
				System.out.println("Error: "+args[0]+" not a valid GT3X file.");
				return;	
			}
		} catch (IOException e) {
			System.out.println("Error: "+args[0]+" not a valid GT3X file.");
			return;
		}
		
	}
	
}
