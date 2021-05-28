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

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;

import com.qmedic.data.converter.gt3x.enums.GT3XParserOutputDataType;
import com.qmedic.data.converter.gt3x.iface.GT3XFileProcessingListener;

public class ConverterWorker implements GT3XFileProcessingListener {

	private GT3XFile _gt3xFile;
	private String _outDirectoryPath;
	private String[] _cmdLineArgs;
	
	public ConverterWorker(String[] cmdLineArgs) {
		this._outDirectoryPath = cmdLineArgs[1];
		this._cmdLineArgs = new String[]{cmdLineArgs[2], cmdLineArgs[3], cmdLineArgs[4]};
		
		GT3XParserOutputDataType odt = GT3XParserOutputDataType.MHEALTH;
		if(cmdLineArgs[5].equals("ACTIGRAPH")) 
			odt = GT3XParserOutputDataType.ACTIGRAPH;
		this._gt3xFile = new GT3XFile(cmdLineArgs[0], _outDirectoryPath, odt);
		if(cmdLineArgs[6].equals("SUMMARY_ON")) {
			this._gt3xFile.setCreateSummaryFilesOn(); // Create activity counts
		}
		if(cmdLineArgs[7].equals("DEBUG_ON")) {
			this._gt3xFile.setDebugOn(); // Turn debug mode on
		}
	}
	
	public void processFile() {
		// Check output directory validity
		File outDirectory = new File(_outDirectoryPath);
		if(!outDirectory.exists()) {
			System.out.println("Error: Output directory "+_outDirectoryPath+" doesn't exist!");
			return;
		}
		if(!outDirectory.isDirectory()) {
			System.out.println("Error: Output directory "+_outDirectoryPath+" is not a directory!!");
			return;
		}
		
		if(_gt3xFile.init(_cmdLineArgs, null)) {
			System.out.println("File ok!");
			try {
				_gt3xFile.convertToMHealth(this);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onProcessingStarted(String inputFileFullPath, String message, long bytesOfUncompressedContent, Map<String,Object> callbackMetadata) {
		System.out.println("Start processing: "+inputFileFullPath+". BytesUncompressedContent: "+bytesOfUncompressedContent+". Message: "+message+".");
	}
	
	@Override
	public void onHourlyFileCreated(String createdFilePath, String message, long bytesRead, Map<String,Object> callbackMetadata, Calendar calendar) {
		System.out.println("Created: "+createdFilePath+". BytesRead: "+bytesRead);
	}

	@Override
	public void onProcessingFinished(String inputFileFullPath, String message, long bytesReadTotal, Map<String,Object> callbackMetadata) {
		System.out.println("Finished processing: "+inputFileFullPath+". BytesReadTotal: "+bytesReadTotal+". Message: "+message+".");	
	}
	
}
