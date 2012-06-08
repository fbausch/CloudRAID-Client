package de.dhbw.mannheim.cloudraid.client;

public class HTTPException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8800170061168765042L;

	private int code;
	private String msg;

	public HTTPException(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	public int getHTTPCode() {
		return code;
	}

	public String getHTTPErrorMessage() {
		return msg;
	}
}
