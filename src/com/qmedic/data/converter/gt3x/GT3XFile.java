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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.qmedic.data.converter.gt3x.enums.DeviceType;
import com.qmedic.data.converter.gt3x.enums.DeviceVersion;
import com.qmedic.data.converter.gt3x.enums.LogRecordType;

public class GT3XFile {

	private static final int ZIP_INDICATOR = 0x504b0304; // first 4 bytes of all zip file

	public String _InputFileName=null;
	public String _OutputFileName=null;
	public String _OutputFileDirectory=null;
	public DeviceVersion _DeviceVersion=DeviceVersion.UNKNOWN;
	public DeviceType _Device=DeviceType.UNKNOWN;

	// info.txt (both versions 1 & 2)
	public String _SerialNumber=null;
	public String _Firmware=null;
	public double _BatteryVoltage=-1;
	public int _SampleRate=-1;
	public long _StartDate=-1;
	public long _DownloadDate=-1;
	public long _BoardRevision=-1;
	public double _LuxScaleFactor=-1;
	public double _LuxMaxValue=-1;

	// info.txt (version 2 specific)
	public String _DeviceType=null;
	public long _LastSampleTime=-1;
	public double _AccelerationScale=-1;
	public double _AccelerationMin=-1;
	public double _AccelerationMax=-1;
	public String _TimeZone=null;
	public String _TimeZoneOffsetMHealth=null;

	// output file(s) info
	public long _TotalBytes=0;
	public double _Delta=-1;
	public String _MHealthFileName=null;
	public long _LastRecordedTimeStamp=0;
	public String _LastRecordedXYZ=null;
	public long _CurrHourTimestamp=0;
	public long _PrevHourTimestamp=0;
	public Boolean _WithTimestamps=false;
	public Boolean _InGAcceleration=false;
	public Boolean _SplitMHealth=false;

	private final static double ACCELERATION_SCALE_FACTOR_NEO_CLE=341.0;
	private final static double ACCELERATION_SCALE_FACTOR_MOS=256.0;


	/*
	 * A constructor for the GT3XFile class
	 */
	public GT3XFile(final String inputFilename, final String outputFileDirectory){
		if(outputFileDirectory.charAt(outputFileDirectory.length()-1)!='/') {
			this._OutputFileDirectory=outputFileDirectory+"/";
		} else {
			this._OutputFileDirectory=outputFileDirectory;
		}				
		this._InputFileName=inputFilename;
	}

	/*
	 * A helper method to fill in a gap in the data (with the number of samples specified) 
	 * using the last known data, starting from timestamp
	 */
	private double fillDataGap(final FileWriter writer, double timestamp, final String lastKnownData) 
			throws IOException {
		if(this._WithTimestamps) {
			writer.append(GT3XUtils.simpleDateFormatObject(GT3XUtils.MHEALTH_TIMESTAMP_DATA_FORMAT).
					format(new Date((long) GT3XUtils.FixTimeStamp(timestamp, this._Delta,false))) + "," + lastKnownData + "\n");
		} else {
			writer.append(lastKnownData+"\n");
		}
		timestamp = GT3XUtils.FixTimeStamp(timestamp, this._Delta,true);
		return timestamp;
	}


	/*
	 * This method checks if a file is of GT3X format version 1
	 * It returns true if the file is of the correct format otherwise it returns false
	 */
	public static boolean isGT3XV1(final File file) throws IOException {

		if(file.isDirectory()) {
			return false;
		}
		if(!file.canRead()) {
			throw new IOException("Cannot read file "+file.getAbsolutePath());
		}
		if(file.length() < 4) {
			return false;
		}
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		int test = in.readInt();
		in.close();
		boolean isZipFile=(test == ZIP_INDICATOR);
		if (!isZipFile)
			return false;

		//Check if the file contains the necessary Actigraph files
		ZipFile zip = new ZipFile(file);
		boolean hasActivityData=false;
		boolean hasLuxData=false;
		boolean hasInfoData=false;
		for (Enumeration<?> e = zip.entries(); e.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			if (entry.toString().equals("activity.bin"))
				hasActivityData=true;
			if (entry.toString().equals("lux.bin"))
				hasLuxData=true;
			if (entry.toString().equals("info.txt"))
				hasInfoData=true;
		}
		zip.close();

		if (hasActivityData && hasLuxData && hasInfoData)
			return true;

		return  false;
	}


	/*
	 * This method checks if a file is of GT3X format version 2
	 * It returns true if the file is of the correct format otherwise it returns false
	 */
	public static boolean isGT3XV2(final File file) throws IOException {

		if(file.isDirectory()) {
			return false;
		}
		if(!file.canRead()) {
			throw new IOException("Cannot read file "+file.getAbsolutePath());
		}
		if(file.length() < 4) {
			return false;
		}
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		int test = in.readInt();
		in.close();
		boolean isZipFile=(test == ZIP_INDICATOR);
		if (!isZipFile)
			return false;

		//Check if the file contains the necessary Actigraph files
		ZipFile zip = new ZipFile(file);
		boolean hasLogData=false;
		boolean hasInfoData=false;
		for (Enumeration<?> e = zip.entries(); e.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			if (entry.toString().equals("log.bin"))
				hasLogData=true;
			if (entry.toString().equals("info.txt"))
				hasInfoData=true;
		}
		zip.close();

		if (hasLogData && hasInfoData)
			return true;

		return  false;
	}	


	/*
	 * This method is the entry point to convert both Version 1 and Version 2 gt3x file formats.
	 * This is also where the initial file information are set up.
	 */
	public static GT3XFile fromGT3XToCSV(
			final File inFile, 
			final String outFileDirectory, 
			final boolean inGAcceleration, 
			final boolean withTimestamps, 
			final boolean split
			) throws IOException {
		// Check if the file is a valid GT3X V1 file
		if(!isGT3XV1(inFile) && !isGT3XV2(inFile))
			return null;

		GT3XFile gt3xFile = new GT3XFile(inFile.getCanonicalPath(), outFileDirectory);
		// Parse Actigraph metadata from info.txt
		ZipFile zip = new ZipFile(inFile);
		for (Enumeration<?> e = zip.entries(); e.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			if (entry.toString().equals("info.txt"))
			{				
				BufferedReader in = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
				while (in.ready()) {
					String line = in.readLine();
					if (line!=null){
						String[] tokens=line.split(":");
						if ((tokens !=null)  && (tokens.length==2)){
							if (tokens[0].trim().equals("Serial Number")){
								gt3xFile._SerialNumber=tokens[1].trim();
								gt3xFile._Device = GT3XUtils.getDeviceType(gt3xFile._SerialNumber);								  
								gt3xFile._LuxScaleFactor = (gt3xFile._Device == DeviceType.ACTISLEEPPLUS ? 3.25f : 1.25f);
								gt3xFile._LuxMaxValue = (gt3xFile._Device == DeviceType.ACTISLEEPPLUS ? 6000 : 2500);
							}
							else if (tokens[0].trim().equals("Firmware"))
								gt3xFile._Firmware=tokens[1].trim(); 
							else if (tokens[0].trim().equals("Battery Voltage"))
								gt3xFile._BatteryVoltage=Double.parseDouble(tokens[1].trim());							  							  
							else if (tokens[0].trim().equals("Sample Rate"))
								gt3xFile._SampleRate=Integer.parseInt(tokens[1].trim());						 
							else if (tokens[0].trim().equals("Start Date"))
								gt3xFile._StartDate=GT3XUtils.fromTickToMillisecond(Long.parseLong(tokens[1].trim()));
							else if (tokens[0].trim().equals("Download Date"))
								gt3xFile._DownloadDate=GT3XUtils.fromTickToMillisecond(Long.parseLong(tokens[1].trim()));
							else if (tokens[0].trim().equals("Board Revision"))
								gt3xFile._BoardRevision=Integer.parseInt(tokens[1].trim());
							// Version 2 only
							else if (tokens[0].trim().equals("Device Type"))
								gt3xFile._DeviceType=tokens[1].trim();
							else if (tokens[0].trim().equals("Last Sample Time"))
								gt3xFile._LastSampleTime=GT3XUtils.fromTickToMillisecond(Long.parseLong(tokens[1].trim()));
							else if (tokens[0].trim().equals("Acceleration Scale"))
								gt3xFile._AccelerationScale=Double.parseDouble(tokens[1].trim());
							else if (tokens[0].trim().equals("Acceleration Min"))
								gt3xFile._AccelerationMin=Double.parseDouble(tokens[1].trim());
							else if (tokens[0].trim().equals("Acceleration Max"))
								gt3xFile._AccelerationMax=Double.parseDouble(tokens[1].trim());

							// Determine device version (V1/V2)
							if(gt3xFile._DeviceVersion.equals(DeviceVersion.UNKNOWN) 
									&& (gt3xFile._SerialNumber!=null) 
									&& (gt3xFile._Firmware!=null)) {
								gt3xFile._DeviceVersion = GT3XUtils.getDeviceVersion(gt3xFile._SerialNumber, gt3xFile._Firmware);
							}

							// Set timezone offset to server's timezone offset if V1 (no info in info.txt)
							if(gt3xFile._DeviceVersion.equals(DeviceVersion.V1) 
									&& (gt3xFile._StartDate!=-1) 
									&& gt3xFile._TimeZoneOffsetMHealth==null) {
								gt3xFile._TimeZoneOffsetMHealth = GT3XUtils.getTimeZoneMHealth(gt3xFile._StartDate);
							}
						} else if(gt3xFile._DeviceVersion.equals(DeviceVersion.V2) 
								&& (tokens!=null) 
								&& (tokens.length==4)) {
							// Set timezone offset to timezone offset provided in info.txt if V2
							if(tokens[0].trim().equals("TimeZone")) {
								String tz = tokens[1].trim()+":"+tokens[2].trim()+":"+tokens[3].trim();
								gt3xFile._TimeZone = tz;
								gt3xFile._TimeZoneOffsetMHealth = GT3XUtils.getTimeZoneMHealthFromActigraph(tz);
							}
						}
					}

				}
				in.close();

				gt3xFile._MHealthFileName = GT3XUtils.getMHealthFileName(
						gt3xFile._StartDate, 
						"ACCEL", 
						gt3xFile._SerialNumber, 
						gt3xFile._TimeZoneOffsetMHealth);
				gt3xFile._OutputFileName = gt3xFile._OutputFileDirectory+gt3xFile._MHealthFileName;
				gt3xFile._WithTimestamps = withTimestamps;
				gt3xFile._InGAcceleration = inGAcceleration;
				gt3xFile._SplitMHealth = split;

				System.out.println("Parsed file information successfully...");
				
				if(gt3xFile._DeviceVersion == DeviceVersion.V1) {
					return fromGT3XV1ToCSV(zip, gt3xFile);
				} else if(gt3xFile._DeviceVersion == DeviceVersion.V2) {
					return fromGT3XV2ToCSV(zip, gt3xFile);
				}

				zip.close();
			}
		}

		return null;
	}


	/*
	 * This method converts a GT3X file version 1 into a CSV file with timestamp or without timestamps
	 * Version 1 (NHANES) devices are GT3X+ devices with serial numbers starting with "NEO" and ActiSleep+ 
	 * devices with serial numbers starting with "MRA", both with firmware version 2.5.0 or earlier
	 * Acceleration data is either in G or raw ADC
	 */
	public static GT3XFile fromGT3XV1ToCSV(final ZipFile zip, final GT3XFile gt3xFile) throws IOException {
		//Parse activity.bin
		System.out.println("Parsing activity data for GT3X version 1 format....");
		for (Enumeration<?> e = zip.entries(); e.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			if (entry.toString().equals("activity.bin"))
			{
				InputStream reader =zip.getInputStream(entry);								
				//Read 2 XYZ samples at a time, each sample consists of 36 bits ... 2 full samples will be 9 bytes
				byte[] bytes=new byte[9];	
				int i=0;
				int datum=0;
				double timestamp=gt3xFile._StartDate;
				gt3xFile._Delta = Math.round(1000.0/gt3xFile._SampleRate * 100d) / 100d;  // round the delta to its fourth decimal

				FileWriter writer = new FileWriter(gt3xFile._OutputFileName);
				writer.append(gt3xFile._WithTimestamps ? "HEADER_TIME_STAMP,X,Y,Z\n" : "X,Y,Z\n"); // Add mHealth header
				while ((datum=reader.read())!=-1){		
					bytes[i]=(byte)datum;
					gt3xFile._TotalBytes++;

					if(gt3xFile._SplitMHealth) {
						gt3xFile._CurrHourTimestamp = GT3XUtils.getCurrentHourTimestamp(timestamp);					
						// Create a new file if hour changes...
						if(gt3xFile._PrevHourTimestamp != gt3xFile._CurrHourTimestamp) {
							if(gt3xFile._PrevHourTimestamp!=0) {
								writer.close();
								gt3xFile._MHealthFileName = GT3XUtils.getMHealthFileName(
										gt3xFile._CurrHourTimestamp, 
										"ACCEL", 
										gt3xFile._SerialNumber, 
										gt3xFile._TimeZoneOffsetMHealth);
								gt3xFile._OutputFileName = gt3xFile._OutputFileDirectory+gt3xFile._MHealthFileName;
								writer = new FileWriter(gt3xFile._OutputFileName);
								writer.append(gt3xFile._WithTimestamps ? "HEADER_TIME_STAMP,X,Y,Z\n" : "X,Y,Z\n"); // Add mHealth header
							}
							gt3xFile._PrevHourTimestamp = gt3xFile._CurrHourTimestamp;
						}
					}

					if (++i==9){						
						AccelPair twoSamples = new AccelPair(bytes, GT3XFile.ACCELERATION_SCALE_FACTOR_NEO_CLE);
						timestamp = twoSamples.writeToFile(
								writer, 
								timestamp, 
								gt3xFile._Delta, 
								gt3xFile._InGAcceleration, 
								gt3xFile._WithTimestamps);

						gt3xFile._TotalBytes+=2;
						if (gt3xFile._TotalBytes%1000==0)
							System.out.print("\rConverting sample.... "+(gt3xFile._TotalBytes/1000)+"K");
						i=0;				
					}
				}	
				writer.close();
				reader.close();  
			}
		}
		System.out.println("");
		System.out.println("Done");

		return gt3xFile;
	}


	/*
	 * This method converts a GT3X file version 1 into a CSV file with timestamp or without timestamps
	 * Version 2 devices are GT3X+ firmware 3.0+ (serial: NEO), ActiSleep+ firmware 3.0+ (serial MRA),
	 * wGT3X+ (serial: CLE), wGT3X-BT (serial: MOS0,MOS2), wActiSleep+ (serial: MOS3), wActiSleepBT
	 * (serial: MOS4), GT9X Link (serial: TAS).
	 * Acceleration data is either in G or raw ADC
	 */
	public static GT3XFile fromGT3XV2ToCSV(final ZipFile zip, final GT3XFile gt3xFile) throws IOException {
		//Parse log.bin
		System.out.println("Parsing activity data for GT3X version 2 format....");		
		for(Enumeration<?> e = zip.entries(); e.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			if(entry.toString().equals("log.bin")) {
				InputStream reader = zip.getInputStream(entry);				

				// Header format: separator (1byte), type (1byte), timestamp (8bytes,little-endian), payload size (2bytes,little-endian)
				int headerSize = LogRecord.HEADER_SIZE; // bytes				
				byte[] bytes = new byte[headerSize]; // 8 bytes total
				int i = 0;
				int datum = 0;

				LogRecord record = null;
				boolean headerRead = false;
				boolean payloadRead = false;
				long logTimestamp = -1;				

				double timestamp = gt3xFile._StartDate;
				gt3xFile._Delta = Math.round(1000.0/gt3xFile._SampleRate * 100d) / 100d;  // round the delta to its second decimal

				// Set acceleration scale
				double accelerationScale = 0.0;
				if((gt3xFile._SerialNumber.startsWith("NEO") || (gt3xFile._SerialNumber.startsWith("CLE")))) {
					accelerationScale = GT3XFile.ACCELERATION_SCALE_FACTOR_NEO_CLE;
				} else if(gt3xFile._SerialNumber.startsWith("MOS")){
					accelerationScale = GT3XFile.ACCELERATION_SCALE_FACTOR_MOS;
				} else {
					accelerationScale = gt3xFile._AccelerationScale;
				}

				FileWriter writer = new FileWriter(gt3xFile._OutputFileName);	
				writer.append(gt3xFile._WithTimestamps ? "HEADER_TIME_STAMP,X,Y,Z\n" : "X,Y,Z\n"); // Add mHealth header				
				while ((datum=reader.read())!=-1) {					
					bytes[i] = (byte)datum;
					gt3xFile._TotalBytes++;

					// Read header
					if(!headerRead && !payloadRead && (++i==headerSize)) {
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
						byte chkSum = GT3XUtils.calculateCheckSum(record, bytes[0]);
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
								if(gt3xFile._LastRecordedTimeStamp!=0) {
									long diff = (long)timestamp - gt3xFile._LastRecordedTimeStamp;
									if(diff>0) {
										long numSamplesMissing = (long)(diff/gt3xFile._Delta);
										double tempTimestamp = (double)gt3xFile._LastRecordedTimeStamp;
										for(int j=0; j<numSamplesMissing; j++) {
											if(gt3xFile._SplitMHealth) {
												gt3xFile._CurrHourTimestamp = GT3XUtils.getCurrentHourTimestamp(tempTimestamp);
												// Create a new file if hour changes...
												if(gt3xFile._PrevHourTimestamp != gt3xFile._CurrHourTimestamp) {
													if(gt3xFile._PrevHourTimestamp!=0) {
														writer.close();
														gt3xFile._MHealthFileName = GT3XUtils.getMHealthFileName(
																gt3xFile._CurrHourTimestamp, 
																"ACCEL", 
																gt3xFile._SerialNumber, 
																gt3xFile._TimeZoneOffsetMHealth);
														gt3xFile._OutputFileName = gt3xFile._OutputFileDirectory+gt3xFile._MHealthFileName;
														writer = new FileWriter(gt3xFile._OutputFileName);
														writer.append(gt3xFile._WithTimestamps ? "HEADER_TIME_STAMP,X,Y,Z\n" : "X,Y,Z\n"); // Add mHealth header
													}
													gt3xFile._PrevHourTimestamp = gt3xFile._CurrHourTimestamp;
												}
											}
											tempTimestamp = gt3xFile.fillDataGap(writer, tempTimestamp, gt3xFile._LastRecordedXYZ);
										}										
										//System.out.println("Before "+GT3XUtils.SDF.format(new Date(Math.round(timestamp)))+". "+(diff/1000)+" sec gap. "+numSamplesMissing+" samples.");
									}
								}

								// Write current payload
								for(int j=0; j<record.getPayload().length; j++) {
									payloadBuffer[byteCounter] = record.getPayload()[j];

									if(gt3xFile._SplitMHealth) {
										gt3xFile._CurrHourTimestamp = GT3XUtils.getCurrentHourTimestamp(timestamp);									
										// Create a new file if hour changes...
										if(gt3xFile._PrevHourTimestamp != gt3xFile._CurrHourTimestamp) {
											if(gt3xFile._PrevHourTimestamp!=0) {
												writer.close();
												gt3xFile._MHealthFileName = GT3XUtils.getMHealthFileName(
														gt3xFile._CurrHourTimestamp, 
														"ACCEL", 
														gt3xFile._SerialNumber, 
														gt3xFile._TimeZoneOffsetMHealth);
												gt3xFile._OutputFileName = gt3xFile._OutputFileDirectory+gt3xFile._MHealthFileName;
												writer = new FileWriter(gt3xFile._OutputFileName);
												writer.append(gt3xFile._WithTimestamps ? "HEADER_TIME_STAMP,X,Y,Z\n" : "X,Y,Z\n"); // Add mHealth header
											}
											gt3xFile._PrevHourTimestamp = gt3xFile._CurrHourTimestamp;
										}
									}
									if(++byteCounter==9) {
										// Write the two samples from the current 9 bytes
										AccelPair twoSamples = new AccelPair(payloadBuffer, accelerationScale);										
										timestamp = twoSamples.writeToFile(
												writer, 
												timestamp, 
												gt3xFile._Delta, 
												gt3xFile._InGAcceleration, 
												gt3xFile._WithTimestamps);										

										// Save last recorded information (in case there is a gap following this data point)
										gt3xFile._LastRecordedTimeStamp = (long)timestamp;
										if(gt3xFile._InGAcceleration) {
											gt3xFile._LastRecordedXYZ = GT3XUtils.decimalFormatObject().format(twoSamples.gx2)+","+
													GT3XUtils.decimalFormatObject().format(twoSamples.gy2)+","+
													GT3XUtils.decimalFormatObject().format(twoSamples.gz2);
										} else {
											gt3xFile._LastRecordedXYZ = twoSamples.x2+","+twoSamples.y2+","+twoSamples.z2;
										}

										// Print progress
										gt3xFile._TotalBytes+=2;
										if (gt3xFile._TotalBytes%1000==0)
											System.out.print("\rConverting sample.... "+(gt3xFile._TotalBytes/1000)+"K");
										byteCounter = 0;
									}
								}
							}
						}

						// Prepare to read the next log record
						i = 0;
						bytes = new byte[headerSize];
						headerRead = false;
						payloadRead = false;
					}
				}
				writer.close();
				reader.close();				
			}
		}
		System.out.println("");
		System.out.println("Done");

		return gt3xFile;
	}

	public String toString(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // the format of your date
		sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // give a timezone reference for formating (see comment at the bottom		
		DecimalFormat df = new DecimalFormat("0.00");		
		StringBuilder sb = new StringBuilder();
		sb.append("\nRESULT");
		sb.append("\n--------\n");
		sb.append("Input File Name: " + this._InputFileName+"\n");
		sb.append("Ouput File Directory: " + this._OutputFileDirectory+"\n");
		sb.append("Serial Number: " + this._SerialNumber+"\n");
		sb.append("Firmware: "+this._Firmware+"\n");
		sb.append("Board Version: "+this._BoardRevision+"\n");
		sb.append("Battery Voltage: "+df.format(this._BatteryVoltage)+"\n");
		sb.append("Sample Rate: "+this._SampleRate+ "Hz\n");
		sb.append("Start Time (UTC): "+ sdf.format(new Date(this._StartDate))+"\n");
		sb.append("Download Time (UTC): "+sdf.format(new Date(this._DownloadDate))+"\n");
		sb.append("mHealth Timezone: "+this._TimeZoneOffsetMHealth+"\n");

		if(this._DeviceVersion.equals(DeviceVersion.V2)) {
			sb.append("Timezone: " + this._TimeZone + "\n");
			sb.append("Device Type: " + this._DeviceType +"\n");
			sb.append("Last Sample Time (UTC): " + sdf.format(new Date(this._LastSampleTime)) +"\n");
			sb.append("Acceleration Scale: " + this._AccelerationScale +"\n");
			sb.append("Acceleration Min: " + this._AccelerationMin +"\n");
			sb.append("Acceleration Max: " + this._AccelerationMax +"\n");
		}

		//sb.append("Lux Scale Factor: "+df.format(this._LuxScaleFactor)+"\n");
		//sb.append("Lux Max Value: "+df.format(this._LuxMaxValue)+"\n");		
		sb.append("Total Bytes: "+this._TotalBytes+"\n");
		sb.append("--------\n");

		return sb.toString();
	}
}
