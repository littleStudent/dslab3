package com.dslab.entities;

public class ClientEntity {

	private String name;
	private Boolean active;
	private int port = 0;
	private int lowCount = 0;
	private int middleCount = 0;
	private int highCount = 0;
	
	public ClientEntity (String name, Boolean active) {
		this.name = name;
		this.active = active;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Boolean getActive() {
		return active;
	}
	public void setActive(Boolean active) {
		this.active = active;
	}

	public int getLowCount() {
		return lowCount;
	}

	public void setLowCount(int lowCount) {
		this.lowCount = lowCount;
	}

	public int getHighCount() {
		return highCount;
	}

	public void setHighCount(int highCount) {
		this.highCount = highCount;
	}

	public int getMiddleCount() {
		return middleCount;
	}

	public void setMiddleCount(int middleCount) {
		this.middleCount = middleCount;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
