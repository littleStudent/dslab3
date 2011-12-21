/**
 * sends "isAlive" messages to the server over udp
 */

package com.dslab.genericTaskEngine;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class GenericTaskEngineUdpWorker implements Runnable {

	private DatagramSocket schedulerSocket;
	private String schedulerHost;
	private int schedulerUDPPort;
	private int tcpPort;
	private int alivePeriod;
	
	protected GenericTaskEngineUdpWorker(DatagramSocket socket, String schedulerHost, int schedulerUDPPort, int tcpPort, int alivePeriod) {
		this.schedulerSocket = socket;
		this.schedulerHost = schedulerHost;
		this.schedulerUDPPort = schedulerUDPPort;
		this.tcpPort = tcpPort;
		this.alivePeriod = alivePeriod;
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				byte[] sendData = new byte[1024];
				String sendString = "isAlive " + tcpPort;
				sendData = sendString.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(schedulerHost), schedulerUDPPort);
				schedulerSocket.send(sendPacket);
				Thread.sleep(alivePeriod);
			} catch (SocketException e1) {
				break;
				//e1.printStackTrace();
			} catch (UnknownHostException e) {
				break;
			} catch (IOException e) {
				break;
			} catch (InterruptedException e) {
				break;
			}
		}
		
	}

}
