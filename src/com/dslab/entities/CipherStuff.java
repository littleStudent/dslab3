package com.dslab.entities;

public class CipherStuff {

	private byte[] managerChallenge;
	private byte[] schedulerChallenge;
	private byte[] secretKey;
	private byte[] ivParameter;

	public byte[] getManagerChallenge() {
		return managerChallenge;
	}

	public void setManagerChallenge(byte[] managerChallenge) {
		this.managerChallenge = managerChallenge;
	}

	public byte[] getSchedulerChallenge() {
		return schedulerChallenge;
	}

	public void setSchedulerChallenge(byte[] schedulerChallenge) {
		this.schedulerChallenge = schedulerChallenge;
	}

	public byte[] getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(byte[] secretKey) {
		this.secretKey = secretKey;
	}

	public byte[] getIvParameter() {
		return ivParameter;
	}

	public void setIvParameter(byte[] ivParameter) {
		this.ivParameter = ivParameter;
	}

}
