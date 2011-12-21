/**
 * Handles the load request from the scheduler
 */
package com.dslab.genericTaskEngine;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.dslab.entities.GenericTaskEngineEntity;
import com.dslab.scheduler.SchedulerHelper;

public class GenericTaskEngineTcpSchedulerWorker implements Runnable {

	private Socket schedulerSocket;
	private GenericTaskEngineEntity engine;
	private String input;
	
	protected GenericTaskEngineTcpSchedulerWorker(Socket socket, GenericTaskEngineEntity engine, String input) {
		this.schedulerSocket = socket;
		this.engine = engine;
		this.input = input;
		
	}
	
	@Override
	public void run() {
		try {
			DataOutputStream outToClient = new DataOutputStream(schedulerSocket.getOutputStream());
			if(input != null) {
				if(input.split(" ")[0].equals("!loadState")) {
					outToClient.writeBytes(engine.getLoad() + " " + engine.getMinConsumption() + " " + engine.getMaxConsumption() + "\n");
				} else if (input.split(" ")[0].equals("!logout")) { 
					outToClient.writeBytes("logout complete\n");
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//System.out.println("Error: unexpected Error occured");;
		}
	}
		
}
