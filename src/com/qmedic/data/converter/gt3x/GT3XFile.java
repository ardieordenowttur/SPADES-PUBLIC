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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.qmedic.data.converter.gt3x.enums.DeviceType;
import com.qmedic.data.converter.gt3x.enums.DeviceVersion;
import com.qmedic.data.converter.gt3x.enums.LogRecordType;
import com.qmedic.data.converter.gt3x.enums.GT3XParserOutputDataType;
import com.qmedic.data.converter.gt3x.iface.GT3XFileProcessingListener;
import com.qmedic.data.converter.gt3x.model.AccelPairData;
import com.qmedic.data.converter.gt3x.model.LogRecord;
import com.qmedic.data.converter.gt3x.utils.GT3XUtils;
import com.qmedic.data.converter.gt3x.utils.MHealthUtils;

public class GT3XFile {

	// TODO If LOG_PARAMETER record is preset, then the ACCEL_SCALE (in info.txt?) value should be used.
	public GT3XFileProcessingListener listener;

	private static final int ZIP_INDICATOR = 0x504b0304; // first 4 bytes of all zip file
	private static final double ACCELERATION_SCALE_FACTOR_NEO_CLE=341.0;
	private static final double ACCELERATION_SCALE_FACTOR_MOS=256.0;
	
	// File information
	private String _inputFileFullPath = null;
	private String _outputDirectory = null;
	private GT3XParserOutputDataType _outputDataType = GT3XParserOutputDataType.MHEALTH;
	private ZipFile _sourceGt3x = null;
	private DeviceVersion _deviceVersion = DeviceVersion.UNKNOWN;
	private DeviceType _deviceType = DeviceType.UNKNOWN;
	private long _bytesOfUncompressedContent = 0;
	private Map<String,Object> _callbackMetadata = null;
	
	// File metadata from info.txt (V1, V2)
	private String _serialNumber = null;
	private String _firmware = null;
	private double _batteryVoltage = -1;
	private int _sampleRate = -1;
	private long _startDate = -1;
	private long _downloadDate = -1;
	private long _boardRevision = -1;
	private double _luxScaleFactor = -1;
	private double _luxMaxValue = -1;
	
	// File metadata from info.txt (V2 only)
	private long _lastSampleTime = -1;
	private double _accelerationScale = -1;
	private double _accelerationMin = -1;
	private double _accelerationMax = -1;
	private String _timeZone = null;
	private String _timeZoneOffsetMHealth = null;
	
	// Initialization parameters
	private boolean _optionInGAcceleration = false;
	private boolean _optionWithTimestamp = false;
	private boolean _optionSplit =false;
	
	// Processed data info
	private boolean _doneProcessing = false;
	private boolean _debug = false;
	private boolean _createSummaryFilesOn = false;
	private ZipEntry _logData = null;
	private ZipEntry _activityData = null;
	private long _totalBytes = 0;
	private long _currHourTs = 0;
	private long _prevHourTs = 0;
	private String _currOutputFile = null;
	private String _currMHealthFileName = null;
	private String _currOutputSummaryFile = null;
	// Process data info (V2 only)
	private long _lastRecordedTs = 0;
	private String _lastRecordedXYZ = null;
	private double _delta = -1;
	
	// Helpers
	private MHealthUtils _mHealthUtils = null;
	
	public GT3XFile(final String inputFileFullPath, final String outputDirectory, final GT3XParserOutputDataType outputDataType) {
		this._inputFileFullPath = inputFileFullPath;
		this._outputDirectory = outputDirectory;
		if(!_outputDirectory.endsWith("/")) {
			this._outputDirectory = _outputDirectory + "/";
		}
		this._outputDataType = outputDataType;
		this._doneProcessing = false;
	}
	
	public void setDebugOn() {
		this._debug = true;
	}
	
	public boolean debugOn() {
		return _debug;
	}
	
	public void setCreateSummaryFilesOn() {
		this._createSummaryFilesOn = true;
	}
	
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // the format of your date
		sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // give a timezone reference for formating (see comment at the bottom		
		DecimalFormat df = new DecimalFormat("0.00");		
		StringBuilder sb = new StringBuilder();
		sb.append("\nRESULT");
		sb.append("\n--------\n");
		sb.append("Input File Name: " + this._inputFileFullPath+"\n");
		sb.append("Serial Number: " + this._serialNumber+"\n");
		sb.append("Firmware: "+this._firmware+"\n");
		sb.append("Board Version: "+this._boardRevision+"\n");
		sb.append("Battery Voltage: "+df.format(this._batteryVoltage)+"\n");
		sb.append("Sample Rate: "+this._sampleRate+ "Hz\n");
		sb.append("Start Time (UTC): "+ sdf.format(new Date(this._startDate))+"\n");
		sb.append("Download Time (UTC): "+sdf.format(new Date(this._downloadDate))+"\n");
		sb.append("mHealth Timezone: "+this._timeZoneOffsetMHealth+"\n");

		if(this._deviceVersion.equals(DeviceVersion.V2)) {
			sb.append("Timezone: " + this._timeZone + "\n");
			sb.append("Device Type: " + this._deviceType +"\n");
			sb.append("Last Sample Time (UTC): " + sdf.format(new Date(this._lastSampleTime)) +"\n");
			sb.append("Acceleration Scale: " + this._accelerationScale +"\n");
			sb.append("Acceleration Min: " + this._accelerationMin +"\n");
			sb.append("Acceleration Max: " + this._accelerationMax +"\n");
		}

		sb.append("Lux Scale Factor: "+df.format(this._luxScaleFactor)+"\n");
		sb.append("Lux Max Value: "+df.format(this._luxMaxValue)+"\n");		
		sb.append("Total Bytes: "+this._totalBytes+"\n");
		sb.append("--------");

		return sb.toString();
	}
	
	/*
	 * This method checks if a file is of GT3X format version 1 (NHANES) or version 2 and initializes the object
	 * Note: callbackMetadata is a metadata holder.
	 */
	public boolean init(final String[] params, final Map<String, Object> callbackMetadata) {
		if(_inputFileFullPath == null) {
			if(_debug) System.out.println("Usage: Input file full path not provided.");
			return false;
		}
		
		// Check and initialize parameters/options
		if(params.length != 3) {
			if(_debug) System.out.println("Usage: Incorrect number of parameters. Expected: [G_VALUE/ADC_VALUE],[WITH_TIMESTAMP/WITHOUT_TIMESTAMP],[SPLIT/NO_SPLIT].");
			return false;
		}
		
		if((!params[0].equals("G_VALUE")) && (!params[0].equals("ADC_VALUE"))) {
			if(_debug) System.out.println("Usage: Incorrect output value option. Use G_VALUE or ADC_VALUE.");
			return false;
		}
		this._optionInGAcceleration = params[0].equals("G_VALUE") ? true : false;
		
		if((!params[1].equals("WITHOUT_TIMESTAMP")) && (!params[1].equals("WITH_TIMESTAMP"))) {
			if(_debug) System.out.println("Usage: Incorrect timestamp option. Use WITH_TIMESTAMP or WITHOUT_TIMESTAMP.");
			return false;
		}
		this._optionWithTimestamp = params[1].equals("WITH_TIMESTAMP") ? true : false;
		
		if((!params[2].equals("NO_SPLIT")) && (!params[2].equals("SPLIT"))) {
			if(_debug) System.out.println("Usage: Incorrect mHealth output split option. Use NO_SPLIT or SPLIT.");
			return false;
		}
		this._optionSplit = params[2].equals("SPLIT") ? true : false;
		
		// Check input file validity and update the inputFileFullPath to the full path
		File inFile = new File(_inputFileFullPath);
		if(!inFile.exists()) {
			if(_debug) System.out.println("Error: "+_inputFileFullPath+" doesn't exist!");
			return false;
		}
		if(inFile.isDirectory()) {
			if(_debug) System.out.println("Error: "+_inputFileFullPath+" is a directory!");
			return false;
		}
		if(!inFile.canRead()) {
			if(_debug) System.out.println("Error: "+_inputFileFullPath+" could not be read!");
			return false;
		}
		if(inFile.length() < 4) {
			if(_debug) System.out.println("Error: "+_inputFileFullPath+" is not a valid file!");
		}
		
		if(this._debug) {
			System.out.println("Debug mode ON!");
		}
		
		if(this._createSummaryFilesOn) {
			System.out.println("Create summary files (activity counts) ON!");
		}
		
		try {
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(inFile)));
			int test = in.readInt();
			in.close();
			boolean isZipFile = (test == ZIP_INDICATOR);
			if (!isZipFile) {
				return false;
			}
			this._inputFileFullPath = inFile.getAbsolutePath(); // Update the inputFilePath with its absolute file path
			this._mHealthUtils = new MHealthUtils(_outputDataType);
			this._sourceGt3x = new ZipFile(inFile);
			this._callbackMetadata = callbackMetadata;
			
			// Check if the file contains the necessary Actigraph files
			boolean hasInfoData = false; // V1 and V2
			boolean hasActivityData = false; // V1 only
			boolean hasLuxData = false; // V1 only
			boolean hasLogData = false; // V2 only
			
			ZipEntry entry = null;
			this._bytesOfUncompressedContent = 0;
			for (Enumeration<?> e = _sourceGt3x.entries(); e.hasMoreElements();) {
				entry = (ZipEntry) e.nextElement();
				_bytesOfUncompressedContent += entry.getSize();
				if(entry.toString().equals("info.txt")) {
					hasInfoData = true;
					processInfoFile(entry);
				}
				if(entry.toString().equals("activity.bin")) {
					this._activityData = entry;
					hasActivityData = true;
				}
				if(entry.toString().equals("lux.bin")) {
					hasLuxData = true;
				}
				if(entry.toString().equals("log.bin")) {
					this._logData = entry;
					hasLogData = true;
				}
			}
			
			if(!hasInfoData) {
				if(_debug) System.out.println("Error: "+_inputFileFullPath+" is not a valid GT3X file. No info metadata file.");
				return false;
			}
			
			if(hasActivityData && hasLuxData) {
				return true;
			} else if(hasLogData) {
				return true;
			} else {
				_sourceGt3x.close();
				if(_debug) System.out.println("Error: "+_inputFileFullPath+" is not a valid GT3X file. Unknown file content detected.");
				return false;
			}
		} catch(IOException e) {
			if(_debug) System.out.println("Error: "+_inputFileFullPath+" is not a valid GT3X file. Unknown error while trying to read file.");
			return false;
		}
	}
	
	public boolean convertToMHealth(GT3XFileProcessingListener listener) throws IOException {
		this.listener = listener;
		
		listener.onProcessingStarted(_inputFileFullPath, "OK", _bytesOfUncompressedContent, _callbackMetadata);
		
		if(_sourceGt3x == null || _deviceVersion == DeviceVersion.UNKNOWN) {
			if(this.listener != null) {
				listener.onProcessingFinished(_inputFileFullPath, "Error processing. Source GT3X file or its version is unknown.", _totalBytes, _callbackMetadata);
			}
			return false;
		}
		
		switch(_deviceVersion) {
		case V1:
			return processGT3XV1();
		case V2:
			return processGT3XV2();
		default:
			return false;
		}
	}
	
	private void processInfoFile(final ZipEntry infoTxtZipEntry) throws IOException {
		if(_sourceGt3x == null || infoTxtZipEntry == null) {
			return;
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(_sourceGt3x.getInputStream(infoTxtZipEntry)));
		while(in.ready()) {
			String line = in.readLine();
			if(line != null){
				String[] tokens=line.split(":");
				if ((tokens != null)  && (tokens.length == 2)){
					if (tokens[0].trim().equals("Serial Number")){
						this._serialNumber=tokens[1].trim();
						this._deviceType = GT3XUtils.GetDeviceType(_serialNumber);								  
						this._luxScaleFactor = (_deviceType == DeviceType.ACTISLEEPPLUS ? 3.25f : 1.25f);
						this._luxMaxValue = (_deviceType == DeviceType.ACTISLEEPPLUS ? 6000 : 2500);
					}
					else if (tokens[0].trim().equals("Firmware"))
						this._firmware = tokens[1].trim(); 
					else if (tokens[0].trim().equals("Battery Voltage"))
						this._batteryVoltage = Double.parseDouble(tokens[1].trim());							  							  
					else if (tokens[0].trim().equals("Sample Rate")) {
						this._sampleRate = Integer.parseInt(tokens[1].trim());
					}
					else if (tokens[0].trim().equals("Start Date"))
						this._startDate=GT3XUtils.FromTickToMillisecond(Long.parseLong(tokens[1].trim()));
					else if (tokens[0].trim().equals("Download Date"))
						this._downloadDate=GT3XUtils.FromTickToMillisecond(Long.parseLong(tokens[1].trim()));
					else if (tokens[0].trim().equals("Board Revision"))
						this._boardRevision=Integer.parseInt(tokens[1].trim());
					// Version 2 only
					else if (tokens[0].trim().equals("Last Sample Time"))
						this._lastSampleTime=GT3XUtils.FromTickToMillisecond(Long.parseLong(tokens[1].trim()));
					else if (tokens[0].trim().equals("Acceleration Scale"))
						this._accelerationScale=Double.parseDouble(tokens[1].trim());
					else if (tokens[0].trim().equals("Acceleration Min"))
						this._accelerationMin=Double.parseDouble(tokens[1].trim());
					else if (tokens[0].trim().equals("Acceleration Max"))
						this._accelerationMax=Double.parseDouble(tokens[1].trim());

					// Determine device version (V1/V2)
					if(_deviceVersion.equals(DeviceVersion.UNKNOWN) 
							&& (_serialNumber!=null) 
							&& (_firmware!=null)) {
						this._deviceVersion = GT3XUtils.GetDeviceVersion(_serialNumber, _firmware);
					}

					// Set timezone offset to server's timezone offset if V1 (no info in info.txt)
					if(_deviceVersion.equals(DeviceVersion.V1) 
							&& (_startDate != -1) 
							&& _timeZoneOffsetMHealth == null) {
						this._timeZoneOffsetMHealth = _mHealthUtils.getTimeZoneMHealth(_startDate);
					}
				} else if(_deviceVersion.equals(DeviceVersion.V2)
						&& (tokens != null) 
						&& (tokens.length == 4)) {
					// Set timezone offset to timezone offset provided in info.txt if V2
					if(tokens[0].trim().equals("TimeZone")) {
						String tz = tokens[1].trim()+":"+tokens[2].trim()+":"+tokens[3].trim();
						this._timeZone = tz;
						this._timeZoneOffsetMHealth = _mHealthUtils.getTimeZoneMHealthFromActigraph(tz);
					}
				}
			}

		}
		in.close();
		
		// Initialize the first output file's name
		this._currMHealthFileName = _mHealthUtils.getMHealthFileName(_startDate, _deviceType.toString(), _firmware, _serialNumber, _timeZoneOffsetMHealth);
		this._currOutputFile = _outputDirectory+_currMHealthFileName;
	}
	
	private boolean processGT3XV1() throws IOException {
		if(_activityData == null) {
			finishProcessing("Activity data not found!", _totalBytes);
			return false;
		}
		//Parse activity.bin
		if(_debug) System.out.println("Parsing activity data for GT3X version 1 format....");
		long startedTs = System.currentTimeMillis();
		InputStream reader =_sourceGt3x.getInputStream(_activityData);								
		//Read 2 XYZ samples at a time, each sample consists of 36 bits ... 2 full samples will be 9 bytes
		byte[] bytes=new byte[9];	
		int i=0;
		int datum=0;
		double timestamp=_startDate;
		
		// Calendar instance for callbacks
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis((long) timestamp);

		// For ACCEL data
		AccelPair twoSamples = new AccelPair(_optionInGAcceleration, _optionWithTimestamp, _sampleRate, _outputDataType);
		AccelPairData accelPairData;
		FileWriter writer = new FileWriter(_currOutputFile);
		BufferedWriter bw = new BufferedWriter(writer);
		bw.append(getAccelFileHeader()); // Add mHealth header
		
		// For Activity Count summary data
		ActivityCountSummaryCreator acSummaryCreator = null;
		FileWriter acWriter = null;
		BufferedWriter acBw = null;
		if(_createSummaryFilesOn) {
			acSummaryCreator = new ActivityCountSummaryCreator();
			this._currOutputSummaryFile = _outputDirectory+_mHealthUtils.getActivityCountMHealthFileName(_currMHealthFileName); 
			acWriter = new FileWriter(_currOutputSummaryFile);
			acBw = new BufferedWriter(acWriter);
			acBw.append(getActivityCountFileHeader());
		}
		
		StringBuilder filenameSb = new StringBuilder();
		while ((datum=reader.read())!=-1){		
			bytes[i] = (byte)datum;
			_totalBytes++;

			if(_optionSplit) {
				_currHourTs = GT3XUtils.GetCurrentHourTimestamp(timestamp);					
				// Create a new file if hour changes...
				if(_prevHourTs != _currHourTs) {
					if(_prevHourTs!=0) {
						// Close the previous file
						bw.close();
						writer.close();
						hourlyFileCreated(_currOutputFile, _totalBytes, cal);
						if(_createSummaryFilesOn) {
							acBw.close();
							acWriter.close();
							hourlyFileCreated(_currOutputSummaryFile, _totalBytes, cal);
						}	
						
						// Create the new file
						_currMHealthFileName = _mHealthUtils.getMHealthFileName(_currHourTs, _deviceType.toString(), _firmware, _serialNumber, _timeZoneOffsetMHealth);
						filenameSb.setLength(0);
						filenameSb.append(_outputDirectory);
						filenameSb.append(_currMHealthFileName);
						_currOutputFile = filenameSb.toString();
						writer = new FileWriter(_currOutputFile);
						bw = new BufferedWriter(writer);
						bw.append(getAccelFileHeader()); // Add mHealth header
						if(_createSummaryFilesOn) {
							this._currOutputSummaryFile = _outputDirectory+_mHealthUtils.getActivityCountMHealthFileName(_currMHealthFileName); 
							acWriter = new FileWriter(_currOutputSummaryFile);
							acBw = new BufferedWriter(acWriter);
							acBw.append(getActivityCountFileHeader());
						}
						
						cal.setTimeInMillis((long)_currHourTs); // Update calendar for callbacks
					}
					_prevHourTs = _currHourTs;
				}
			}

			if (++i==9){						
				twoSamples.setAccelPair(bytes, GT3XFile.ACCELERATION_SCALE_FACTOR_NEO_CLE);
				accelPairData = twoSamples.writeToFile(bw, timestamp, _mHealthUtils.dataSimpleDateFormat());
				timestamp = accelPairData.timestamp();
				if(_createSummaryFilesOn) {
					// Process the data pair for activity count summary calculation
					acSummaryCreator.processNewAccelData(acBw, (long)timestamp, accelPairData.first(), _mHealthUtils.dataSimpleDateFormat());
					acSummaryCreator.processNewAccelData(acBw, (long)timestamp, accelPairData.second(), _mHealthUtils.dataSimpleDateFormat());
				}
				
				_totalBytes+=2;
//				if (_totalBytes%1000==0 && _debug) {
//					System.out.print("\rConverting sample.... "+(_totalBytes/1000)+"K");
//				}
				i=0;				
			}
		}	
		bw.close();
		writer.close();
		reader.close();
		this._doneProcessing = true;
		hourlyFileCreated(_currOutputFile, _totalBytes, cal);
		if(_createSummaryFilesOn) {
			acBw.close();
			acWriter.close();
			hourlyFileCreated(_currOutputSummaryFile, _totalBytes, cal);
		}
		finishProcessing("OK", _totalBytes);
		if(_debug) {
			System.out.println("Done: "+(Math.round((System.currentTimeMillis()-startedTs)/1000))+" seconds. Total bytes processed = "+_totalBytes);
			System.out.println(this.toString());
		}
		return true;
	}
	
	private boolean processGT3XV2() throws IOException {
		if(_logData == null) {
			finishProcessing("Log data not found!", _totalBytes);
			return false;
		}
		//Parse log.bin
		if(_debug) System.out.println("Parsing activity data for GT3X version 2 format....");
		long startedTs = System.currentTimeMillis();
		InputStream reader =_sourceGt3x.getInputStream(_logData);
		byte[] bytes=new byte[LogRecord.HEADER_SIZE];
		int i=0;
		int datum=0;
		
		LogRecord record = null;
		boolean headerRead = false;
		boolean payloadRead = false;
		long logTimestamp = -1;
		
		double timestamp = _startDate;
		this._delta = Math.round(1000.0/_sampleRate * 100d) / 100d; // round the delta to its second decimal
		
		// Calendar instance for callbacks
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis((long) timestamp);
		
		// Set acceleration scale
		double accelerationScale = 0.0;
		if((_serialNumber.startsWith("NEO") || (_serialNumber.startsWith("CLE")))) {
			accelerationScale = GT3XFile.ACCELERATION_SCALE_FACTOR_NEO_CLE;
		} else if(_serialNumber.startsWith("MOS")){
			accelerationScale = GT3XFile.ACCELERATION_SCALE_FACTOR_MOS;
		} else {
			accelerationScale = _accelerationScale;
		}

		// For ACCEL data
		AccelPair twoSamples = new AccelPair(_optionInGAcceleration, _optionWithTimestamp, _sampleRate, _outputDataType);
		AccelPairData accelPairData;
		FileWriter writer = new FileWriter(_currOutputFile);
		BufferedWriter bw = new BufferedWriter(writer);
		bw.append(getAccelFileHeader()); // Add mHealth header
		
		// For Activity Count summary data
		ActivityCountSummaryCreator acSummaryCreator = null;
		FileWriter acWriter = null;
		BufferedWriter acBw = null;
		if(_createSummaryFilesOn) {
			acSummaryCreator = new ActivityCountSummaryCreator();
			this._currOutputSummaryFile = _outputDirectory+_mHealthUtils.getActivityCountMHealthFileName(_currMHealthFileName); 
			acWriter = new FileWriter(_currOutputSummaryFile);
			acBw = new BufferedWriter(acWriter);
			acBw.append(getActivityCountFileHeader());
		}
		
		StringBuilder filenameSb = new StringBuilder();
		while ((datum=reader.read())!=-1){		
			bytes[i] = (byte)datum;
			_totalBytes++;

			// Read header
			if(!headerRead && !payloadRead && (++i==LogRecord.HEADER_SIZE)) {
				record = new LogRecord();
				record.setSeparator((byte)(bytes[0]&0xFF)); // Separator
				record.setType((byte)(bytes[1]&0xFF)); // Type
				logTimestamp = bytes[2]&0xFF;
				logTimestamp |= (bytes[3]&0xFF)<<8;
				logTimestamp |= (bytes[4]&0xFF)<<16;
				logTimestamp |= (bytes[5]&0xFF)<<24;
				record.setTimestamp((long)logTimestamp); // Timestamp
				datum = bytes[6]&0xFF;
				datum |= (bytes[7]&0xFF)<<8;
				record.setPayloadSize((int)datum); // Payload size (little-endian)						

				//System.out.println();
				//System.out.print("Header="+GT3XUtils.bytesToHex(bytes)+". ");
				//System.out.print("Separator="+record.getSeparator()+". ");
				//System.out.print("Type="+record.getType()+". ");
				//System.out.print("Timestamp="+record.getTimestamp()+". ");
				//System.out.print("Payload="+record.getPayloadSize()+" bytes"+".");						

				// Prepare to read payload
				headerRead = true;
				i = -1;
				bytes = new byte[record.getPayloadSize()];
			}
			
			// Read payload
			if(headerRead && !payloadRead && (++i==record.getPayloadSize())) {
				// Metadata: TYPE = 6
				if(record.getType()==LogRecordType.METADATA.getId()) {
					// TODO Take care of metadata?
					//System.out.println("\nMetadata= "+GT3XUtils.bytesToHex(bytes)+"\n");
				}
				if(record.getType()==LogRecordType.PARAMETERS.getId()) {
					// TODO Take care of parameters - look for ACCEL_SCALE?
					//System.out.println("\nParameters= "+GT3XUtils.bytesToHex(bytes)+"\n");
				}

				record.setPayload(bytes); // Copy payload for verifying checksum later

				// Prepare to verify checksum
				payloadRead = true;
				i = -1;
				bytes = new byte[1];
			}
			
			// Read checksum
			if(headerRead && payloadRead && ++i==1) {						
				// Process checksum to verify
				byte chkSum = GT3XUtils.CalculateCheckSum(record, bytes[0]);
				//System.out.print("\n--- Checksum= 0x"+GT3XUtils.bytesToHex(bytes[0])+". Calculated= 0x"+GT3XUtils.byteToHex(chkSum)+"\n\n");

				// Write the data if checksum is verified
				if(chkSum==0x1E) {
					/*
					// Activity2 data: TYPE = 26. For GT9X devices.
					if(record.getType()==LogRecordType.ACTIVITY2.getId()) {
						// TODO Add support for ACTIVITY2 LogRecord data type
					}
					*/
					// Activity data: TYPE = 0
					// Read 2 XYZ samples at a time, each sample consists of 36 bits ... 2 full samples = 9 bytes
					if(record.getType()==LogRecordType.ACTIVITY.getId()) {								
						int byteCounter = 0;
						byte[] payloadBuffer = new byte[9];
						timestamp = (double)(record.getTimestamp()*1000); // Multiply by 1000 to get milliseconds precision								

						// Check for gaps in data and fill them out using the last known data points before those gaps occur
						if(_lastRecordedTs!=0) {
							long diff = (long)timestamp - _lastRecordedTs;
							if(diff>0) {
								long numSamplesMissing = (long)(diff/_delta);
								double tempTimestamp = (double)_lastRecordedTs;
								for(int j=0; j<numSamplesMissing; j++) {
									if(_optionSplit) {
										_currHourTs = GT3XUtils.GetCurrentHourTimestamp(tempTimestamp);
										// Create a new file if hour changes...
										if(_prevHourTs != _currHourTs) {
											if(_prevHourTs!=0) {
												// Fill the data gap for the activitycount file before switching files,
												// otherwise the last activity count data point spills over to the next file
												if(acSummaryCreator != null && acBw != null) {
													acSummaryCreator.processNewAccelData(acBw, (long)tempTimestamp, twoSamples.getLastRecordXYZ(_accelerationScale), _mHealthUtils.dataSimpleDateFormat());
												}
												
												// Close the previous file
												bw.close();
												writer.close();
												hourlyFileCreated(_currOutputFile, _totalBytes, cal);
												if(_createSummaryFilesOn) {
													acBw.close();
													acWriter.close();
													hourlyFileCreated(_currOutputSummaryFile, _totalBytes, cal);
												}
												
												// Create the new file
												_currMHealthFileName = _mHealthUtils.getMHealthFileName(_currHourTs, _deviceType.toString(), _firmware, _serialNumber, _timeZoneOffsetMHealth);
												filenameSb.setLength(0);
												filenameSb.append(_outputDirectory);
												filenameSb.append(_currMHealthFileName);
												_currOutputFile = filenameSb.toString();
												writer = new FileWriter(_currOutputFile);
												bw = new BufferedWriter(writer);
												bw.append(getAccelFileHeader()); // Add mHealth header
												if(_createSummaryFilesOn) {
													this._currOutputSummaryFile = _outputDirectory+_mHealthUtils.getActivityCountMHealthFileName(_currMHealthFileName); 
													acWriter = new FileWriter(_currOutputSummaryFile);
													acBw = new BufferedWriter(acWriter);
													acBw.append(getActivityCountFileHeader());
												}
												
												cal.setTimeInMillis((long)_currHourTs); // Update calendar for callbacks
											}
											_prevHourTs = _currHourTs;
										}
									}
									// Fill the data gap for the activitycount file
									if(acSummaryCreator != null && acBw != null) {
										acSummaryCreator.processNewAccelData(acBw, (long)tempTimestamp, twoSamples.getLastRecordXYZ(_accelerationScale), _mHealthUtils.dataSimpleDateFormat());
									}
									// Fill the data gap for the accel file
									tempTimestamp = fillDataGap(twoSamples, bw, tempTimestamp, _lastRecordedXYZ);
								}										
								//System.out.println("Before "+_mHealthUtils.dataSimpleDateFormat().format((long)timestamp)+". "+(diff/1000)+" sec gap. "+numSamplesMissing+" samples.");
							}
						}

						// Write current payload
						for(int j=0; j<record.getPayload().length; j++) {
							payloadBuffer[byteCounter] = record.getPayload()[j];

							if(_optionSplit) {
								_currHourTs = GT3XUtils.GetCurrentHourTimestamp(timestamp);									
								// Create a new file if hour changes...
								if(_prevHourTs != _currHourTs) {
									if(_prevHourTs!=0) {
										// Close the previous file
										bw.close();
										writer.close();
										hourlyFileCreated(_currOutputFile, _totalBytes, cal);
										if(_createSummaryFilesOn) {
											acBw.close();
											acWriter.close();
											hourlyFileCreated(_currOutputSummaryFile, _totalBytes, cal);
										}
										
										// Create the new file
										_currMHealthFileName = _mHealthUtils.getMHealthFileName(_currHourTs, _deviceType.toString(), _firmware, _serialNumber, _timeZoneOffsetMHealth);
										_currOutputFile = _outputDirectory+_currMHealthFileName;
										writer = new FileWriter(_currOutputFile);
										bw = new BufferedWriter(writer);
										bw.append(getAccelFileHeader()); // Add mHealth header
										if(_createSummaryFilesOn) {
											this._currOutputSummaryFile = _outputDirectory+_mHealthUtils.getActivityCountMHealthFileName(_currMHealthFileName); 
											acWriter = new FileWriter(_currOutputSummaryFile);
											acBw = new BufferedWriter(acWriter);
											acBw.append(getActivityCountFileHeader());
										}
										
										cal.setTimeInMillis((long)_currHourTs); // Update calendar for callbacks
									}
									_prevHourTs = _currHourTs;
								}
							}
							if(++byteCounter==9) {
								// Write the two samples from the current 9 bytes
								twoSamples.setAccelPair(payloadBuffer, accelerationScale);										
								accelPairData = twoSamples.writeToFile(bw, timestamp, _mHealthUtils.dataSimpleDateFormat());
								timestamp = accelPairData.timestamp();
								if(_createSummaryFilesOn) {
									// Process the data pair for activity count summary calculation
									acSummaryCreator.processNewAccelData(acBw, (long)timestamp, accelPairData.first(), _mHealthUtils.dataSimpleDateFormat());
									acSummaryCreator.processNewAccelData(acBw, (long)timestamp, accelPairData.second(), _mHealthUtils.dataSimpleDateFormat());
								}

								// Save last recorded information (in case there is a gap following this data point)
								_lastRecordedTs = (long)timestamp;
								_lastRecordedXYZ = twoSamples.getLastRecordedXYZ(accelerationScale);

								// Print progress
								_totalBytes+=2;
//								if (_totalBytes%1000==0 && _debug) {
//									System.out.print("\rConverting sample.... "+(_totalBytes/1000)+"K");
//								}
								byteCounter = 0;
							}
						}
					}
				}

				// Prepare to read the next log record
				i = 0;
				bytes = new byte[LogRecord.HEADER_SIZE];
				headerRead = false;
				payloadRead = false;
			}
			
		}	
		bw.close();
		writer.close();
		reader.close();
		this._doneProcessing = true;
		hourlyFileCreated(_currOutputFile, _totalBytes, cal);
		if(_createSummaryFilesOn) {
			acBw.close();
			acWriter.close();
			hourlyFileCreated(_currOutputSummaryFile, _totalBytes, cal);
		}
		finishProcessing("OK", _totalBytes);
		if(_debug) {
			System.out.println("Done: "+(Math.round((System.currentTimeMillis()-startedTs)/1000))+" seconds. Total bytes processed = "+_totalBytes);
			System.out.println(this.toString());
		}
		return true;
	}
	
	private double fillDataGap(final AccelPair ap, final BufferedWriter bw, double timestamp, final String lastKnownData) 
			throws IOException {
		if(_optionWithTimestamp) {
			bw.append(_mHealthUtils.dataSimpleDateFormat().format((long)timestamp));
			bw.append(',');
			bw.append(lastKnownData);
			if(_outputDataType == GT3XParserOutputDataType.ACTIGRAPH) bw.append('\r');
			bw.append('\n');
		} else {
			bw.append(lastKnownData);
			if(_outputDataType == GT3XParserOutputDataType.ACTIGRAPH) bw.append('\r');
			bw.append('\n');
		}
		timestamp +=  ap.advanceTimestampHelper();
		return timestamp;
	}
	
	private void hourlyFileCreated(String filePath, long bytesRead, Calendar calendar) {
		if(this.listener != null) {
			listener.onHourlyFileCreated(filePath, "OK", bytesRead, _callbackMetadata, calendar);
		}
	}
	
	private void finishProcessing(String message, long totalBytes) {
		if(this._sourceGt3x != null && this._doneProcessing) {
			try {
				_sourceGt3x.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(this.listener != null) {
			listener.onProcessingFinished(_inputFileFullPath, message, totalBytes, _callbackMetadata);
		}
	}
	
	private String getAccelFileHeader() {
		return _optionWithTimestamp ? "HEADER_TIMESTAMP,X,Y,Z\n" : "X,Y,Z\n";
	}
	
	private String getActivityCountFileHeader() {
		return _optionWithTimestamp ? "HEADER_TIMESTAMP,ACTIVITY_COUNT\n" : "ACTIVITY_COUNT\n";
	}
	
}
