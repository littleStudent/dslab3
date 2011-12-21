package com.dslab.entities;

import com.dslab.Types.GenericTaskEngineStatusEnum;

public class GenericTaskEngineEntity {

	private String ip = "";
	private int tcpPort = 0;
	private int udpPort = 0;
	private int load = 0;
	private boolean timeoutCheck = false;
	private boolean timeoutCheckRunning = false;
	private int minConsumption;
	private int maxConsumption;
	private GenericTaskEngineStatusEnum state = GenericTaskEngineStatusEnum.online;
	
	public GenericTaskEngineEntity() {
		
	}
	
	public GenericTaskEngineEntity(String ip) {
		this.ip = ip;
	}
	
	public GenericTaskEngineEntity(int port) {
		this.tcpPort = port;
	}
	
	public GenericTaskEngineEntity(String ip, int tcpPort, int udpPort, int minConsumption, int maxConsumption) {
		this.ip = ip;
		this.tcpPort = tcpPort;
		this.udpPort = udpPort;
		this.minConsumption = minConsumption;
		this.maxConsumption = maxConsumption;
	}

	public GenericTaskEngineEntity(String ip, int tcpPort) {
		this.ip = ip;
		this.tcpPort = tcpPort;
	}

	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getTcpPort() {
		return tcpPort;
	}
	public void setTcpPort(int port) {
		this.tcpPort = port;
	}
	public int getLoad() {
		return load;
	}
	public void setLoad(int load) {
		this.load = load;
	}

	public boolean isTimeoutCheck() {
		return timeoutCheck;
	}

	public void setTimeoutCheck(boolean timeoutCheck) {
		this.timeoutCheck = timeoutCheck;
	}

	public boolean isTimeoutCheckRunning() {
		return timeoutCheckRunning;
	}

	public void setTimeoutCheckRunning(boolean timeoutCheckRunning) {
		this.timeoutCheckRunning = timeoutCheckRunning;
	}

	public int getMinConsumption() {
		return minConsumption;
	}

	public void setMinConsumption(int minConsumption) {
		this.minConsumption = minConsumption;
	}

	public int getMaxConsumption() {
		return maxConsumption;
	}

	public void setMaxConsumption(int maxConsumption) {
		this.maxConsumption = maxConsumption;
	}

	public int getUdpPort() {
		return udpPort;
	}

	public void setUdpPort(int udpPort) {
		this.udpPort = udpPort;
	}

	public GenericTaskEngineStatusEnum getState() {
		return state;
	}

	public void setState(GenericTaskEngineStatusEnum state) {
		this.state = state;
	}
	
}
