# mHealth-GT3X Converter
A standalone app written in Java to convert raw Accelerometer data from GT3X File Format into mHealth format. Supports both the NHANES-GT3X format (V1) and the newest GT3X-File format (V2).


Description
-----------
The zip file contains Java source code, executables and sample gt3x data to convert a gt3x file into an mHealth-compliant CSV file.
The parser currently only accounts for ACTIVITY-type data (accelerometer - **ACCEL** in mHealth).

Folders description:
- **sample-data/v1**: Contains 3 sample Version1 gt3x files from Actigraph devices with serial numbers starting with **NEO**. Version1 of the gt3x file format specifications can be found here: [GitHub Link](https://github.com/actigraph/NHANES-GT3X-File-Format).
- **sample-data/v2**: Contains 4 sample Version2 gt3x files from Actigraph devices with serial numbers starting with **MOS**. Version2 of the gt3x file format specifications can be found here: [GitHub Link](https://github.com/actigraph/GT3X-File-Format).
- **src/**: Contains an eclipse project with the full source code.
- **GT3XParser.jar**: Is an executable jar file accessible from the command line to convert a gt3x file to mHealth-compliant CSV file.


Requirements
------------
Download and install the latest Java JRE if you do not have it (most systems have JRE). 


Usage
-----
Open a command prompt and type a command with the following usage pattern:

```ShellSession
java -jar GT3XParser.jar [INPUT GT3X FILE] [OUTPUT CSV DIRECTORYPATH] [G_VALUE/ADC_VALUE] [WITH_TIMESTAMP/WITHOUT_TIMESTAMP] [SPLIT/NO_SPLIT]
```

- **[INPUT GT3X FILE]**: (required) Relative or absolute path for a GT3X file.
- **[OUTPUT CSV DIRECTORYPATH]**: (required) Relative or absolute path of the directory for the mHealth CSV output file (Ending the path with a '/' is optional).
- **[G_VALUE/ADC_VALUE]**: (required) Generate acceleration values in g acceleration or analog to digital conversion.
- **[WITH_TIMESTAMP/WITHOUT_TIMESTAMP]**: (required) Generate date with or without timestamps.
- **[SPLIT/NO_SPLIT]**: (required) Generate mHealth output in one big file or in multiple hourly files.


Example Commands
----------------
1- Open a command prompt.

2- Navigate to the directory where the GT3XParser.jar in the provided zip is located.

3- Type the following command: 
```ShellSession
java -jar GT3XParser.jar sample-data/sample1.gt3x /home/user/Documents G_VALUE WITH_TIMESTAMP SPLIT
```

4- A mHealth CSV file should be generated in the directory with decoded GT3X data. For example:
```ShellSession
/home/user/Documents/ACCEL.MOS2A45130448.2015-04-13-15-15-00-000-M0400.csv
```

or in the case where the "SPLIT" option is used, hourly mHealth CSV files will be generated:

```ShellSession
/home/user/Documents/ACCEL.MOS2A45130448.2015-04-13-15-15-00-000-M0400.csv
/home/user/Documents/ACCEL.MOS2A45130448.2015-04-14-00-00-00-000-M0400.csv
/home/user/Documents/ACCEL.MOS2A45130448.2015-04-15-00-00-00-000-M0400.csv
...
```


Notes
-----
For devices whose serial number starts with **TAS**, the accelerometer data are classified as ACTIVITY2 in the GT3X file format.
This parser currently does not support **ACTIVITY2** LogRecord data type yet.

