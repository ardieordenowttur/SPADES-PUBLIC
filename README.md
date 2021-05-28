# mHealth-GT3X Converter
A standalone app written in Java to convert raw Accelerometer data from GT3X File Format into mHealth format. Supports both the NHANES-GT3X format (V1) and the newest GT3X-File format (V2). The JAR file can also serve as a pluggable library module by importing it into Java-based applications.


Description
-----------
This repository contains Java source code, executables and sample gt3x data to convert a GT3X files into mHealth-compliant CSV files. The converter currently only accounts for ACTIVITY-type data (accelerometer - **ACCEL**-type in mHealth).

Content description:
- **sample-data/v1**: Contains 3 sample Version1 gt3x files from Actigraph devices with serial numbers starting with **NEO**. Version1 of the gt3x file format specifications can be found here: [GitHub Link](https://github.com/actigraph/NHANES-GT3X-File-Format).
- **sample-data/v2**: Contains 4 sample Version2 gt3x files from Actigraph devices with serial numbers starting with **MOS**. Version2 of the gt3x file format specifications can be found here: [GitHub Link](https://github.com/actigraph/GT3X-File-Format).
- **src/**: Contains a Java project with the full source code.
- **GT3XParser.jar**: Is an executable jar file accessible from the command line to convert a gt3x file to mHealth-compliant CSV files.


Requirements
------------
To run the executable (GT3XParser.jar), download and install the latest [Java JRE](http://www.oracle.com/technetwork/java/javase/downloads/index.html) if you do not have it (most systems have JRE). 


Usage
-----
Download the GT3XParser.jar file, open a command prompt and type a command with the following usage pattern:
```ShellSession
java -jar GT3XParser.jar [INPUT GT3X FILE] [OUTPUT CSV DIRECTORYPATH] [G_VALUE/ADC_VALUE] [WITH_TIMESTAMP/WITHOUT_TIMESTAMP] [SPLIT/NO_SPLIT] [MHEALTH/ACTIGRAPH] [SUMMARY_ON/SUMMARY_OFF] [DEBUG_ON/DEBUG_OFF]
```

- **[INPUT GT3X FILE]**: (required) Relative or absolute path for a GT3X file.
- **[OUTPUT CSV DIRECTORY PATH]**: (required) Relative or absolute path of the directory for the mHealth CSV output file (Ending the path with a '/' is optional).
- **[G_VALUE/ADC_VALUE]**: (required) Generate acceleration values in g acceleration or analog to digital conversion.
- **[WITH_TIMESTAMP/WITHOUT_TIMESTAMP]**: (required) Generate date with or without timestamps.
- **[SPLIT/NO_SPLIT]**: (required) Generate mHealth output in one big file or in multiple hourly files.
- **[MHEALTH/ACTIGRAPH]**: (required) Specifies the data format of the output files. Defaults to mHealth format. The **ACTIGRAPH** option will create output files with ActiLife's data format.
- **[SUMMARY_ON/SUMMARY_OFF]**: (required) Specifies whether the converter
- **[DEBUG_ON/DEBUG_OFF]**: (required) Specifies whether the converter should print some debug messages to the console.


Example Commands
----------------
1- Open a terminal or command prompt.

2- Navigate to the directory where the downloaded GT3XParser.jar is located.

3- Type the following command (replace "/home/user/" with the appropriate values!): 
```ShellSession
java -jar GT3XParser.jar sample-data/v2/TodaysData/MOS2A45130448.gt3x /home/user/Documents/output/ G_VALUE WITH_TIMESTAMP NO_SPLIT MHEALTH SUMMARY_OFF DEBUG_OFF
```

4- An mHealth CSV file should be generated in the specified output directory with decoded GT3X data. For example:
```ShellSession
/home/user/Documents/WGT3XBT-AccelerationCalibrated-1x5x0.MOS2A45130448.2015-04-09-14-02-00-000-M0400.sensor.csv
```

or in the case where the "SPLIT" option is used, hourly mHealth CSV files will be generated:

```ShellSession
/home/user/Documents/output/WGT3XBT-AccelerationCalibrated-1x5x0.MOS2A45130448.2015-04-09-14-02-00-000-M0400.sensor.csv
/home/user/Documents/output/WGT3XBT-AccelerationCalibrated-1x5x0.MOS2A45130448.2015-04-09-15-00-00-000-M0400.sensor.csv
/home/user/Documents/output/WGT3XBT-AccelerationCalibrated-1x5x0.MOS2A45130448.2015-04-09-16-00-00-000-M0400.sensor.csv
/home/user/Documents/output/WGT3XBT-AccelerationCalibrated-1x5x0.MOS2A45130448.2015-04-09-17-00-00-000-M0400.sensor.csv
...
```


Notes
-----
- For NHANES-GT3X File Format, a single 3-axis sample takes up 36 bits of data (4.5 bytes), which cannot be read as is as an array of bytes. This converter takes converts 72 bits of data at a time (9 bytes), which is a pair of 3-axis samples that can be read as an array of bytes.
- For devices whose serial number starts with **TAS**, the accelerometer data are classified as ACTIVITY2 in the GT3X file format. This converter currently does not support **ACTIVITY2** LogRecord data type yet.


Using the JAR as a Java Library
-------------------------------
The standalone GT3XParser.jar file can also be used as a Java library. Here is an example of a sample class using the GT3XFile.java class:
```ShellSession
/*
 * This example corresponds to the following shell command:
 * java -jar GT3XParser.jar sample-data/v2/TodaysData/MOS2A45130448.gt3x /home/user/Documents/output/ G_VALUE WITH_TIMESTAMP NO_SPLIT MHEALTH SUMMARY_OFF DEBUG_OFF
 *
 * Your class should implement the GT3XFileProcessingListener interface to be able to catch the callbacks from the GT3X File processing.
 */

import com.qmedic.data.converter.gt3x.GT3XFile;
import com.qmedic.data.converter.gt3x.enums.GT3XParserOutputDataType;
import com.qmedic.data.converter.gt3x.iface.GT3XFileProcessingListener;

public class MyConverter implements GT3XFileProcessingListener {

	public MyConverter() {
		String gt3xFilePath = "/home/user/GT3XParser/sample-data/v2/TodaysData/MOS2A45130448.gt3x"; // Sample value
		String outputDirectoryPath = "/home/user/Documents/output/"; // Sample value
	
		// Create a GT3XFile object and emulate command line parameters
		GT3XParserOutputDataType odt = GT3XParserOutputDataType.MHEALTH; // corresponds to the "MHEALTH" command line parameter
		String[] cmdParams = ["G_VALUE", "WITH_TIMESTAMP", "SPLIT"];
		GT3XFile gt3xFile = new GT3XFile(gt3xFilePath, outputDirectoryPath, odt);
		
		// method signature: public boolean GT3XFile.init(String[] params, Map<String, Object> callbackMetadata)
		// Note: callbackMetadata is not used at all within the converter. Its primary purpose is to hold any information needed to map the
		// callback(s) to the original file being processed.
		if(gt3xFile.init(cmdParams, null)) {
			try {
				gt3xFile.setCreateSummaryFilesOn(); // corresponds to the "SUMMARY_ON" command line parameter
				gt3xFile.setDebugOn(); // corresponds to the "DEBUG_ON" command line parameter
				gt3xFile.convertToMHealth(this); // designates this object as the listener for the conversion process
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	// This callback is fired off when a file's processing is started
	public void onProcessingStarted(String inputFileFullPath, String message, long bytesOfUncompressedContent, Map<String,Object> callbackMetadata) {
		System.out.println("Start processing: "+inputFileFullPath+". BytesUncompressedContent: "+bytesOfUncompressedContent+". Message: "+message+".");
	}
	
	@Override
	// This callback is fired off whenever a file was created. The "calendar" value contains a calendar object created and updated during conversion.
	// The calendar value can be used to figure out the date of the data contained within the created file. 
	public void onHourlyFileCreated(String createdFilePath, String message, long bytesRead, Map<String,Object> callbackMetadata, Calendar calendar) {
		System.out.println("Created: "+createdFilePath+". BytesRead: "+bytesRead);
	}

	@Override
	// This callback is fired off when a file's processing is finished
	public void onProcessingFinished(String inputFileFullPath, String message, long bytesReadTotal, Map<String,Object> callbackMetadata) {
		System.out.println("Finished processing: "+inputFileFullPath+". BytesReadTotal: "+bytesReadTotal+". Message: "+message+".");	
	}

}
```


Links
-----
- SPADESLab - http://www.spadeslab.com/
- QMedic - http://www.qmedichealth.com/
- [mHealth File Format Specification](http://spades-documentation.s3-website-us-east-1.amazonaws.com/mhealth-format.html?id=5.1)
