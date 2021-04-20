package com.qmedic.data.converter.gt3x.enums;

public enum LogRecordType {
	ACTIVITY(0),
	BATTERY(2),
	HHEART_RATE_BPM(4),
	LUX(5),
	METADATA(6),
	TAG(7),
	HEART_RATE_ANT(11),
	CAPSENSE(13),
	HEART_RATE_BLE(14),
	PARAMETERS(21),
	SENSOR_SCHEMA(24),
	SENSOR_DATA(25),
	ACTIVITY2(26);
	
	private short id;
	
	private LogRecordType(int id) {
		this.id = (short)id;
	}
	
	public short getId() {
		return id;
	}
}
