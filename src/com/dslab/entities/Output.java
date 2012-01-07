package com.dslab.entities;

import java.io.Serializable;

public class Output implements Serializable {

	private String message;
	private byte[] hash;

	public Output(String message, byte[] hash) {
		this.message = message;
		this.hash = hash;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public byte[] getHash() {
		return hash;
	}

	public void setHash(byte[] hash) {
		this.hash = hash;
	}

}
