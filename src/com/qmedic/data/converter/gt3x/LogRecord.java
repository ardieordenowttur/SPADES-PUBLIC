package com.qmedic.data.converter.gt3x;

public class LogRecord {
	
	public static final int HEADER_SIZE = 8; // bytes
	
	private byte separator = -1;
	private short type = -1;
	private long timestamp = -1;
	private int payloadSize = -1;
	private byte[] payload = null;
	
	public byte getSeparator() {
		return separator;
	}
	public void setSeparator(byte separator) {
		this.separator = separator;
	}
	
	public short getType() {
		return type;
	}
	public void setType(short type) {
		this.type = type;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public int getPayloadSize() {
		return payloadSize;
	}
	public void setPayloadSize(int payloadSize) {
		this.payloadSize = payloadSize;
	}
	
	public byte[] getPayload() {
		return payload;
	}
	public void setPayload(byte[] payload) {
		this.payload = new byte[payloadSize];
		this.payload = payload;
	}
	
}
