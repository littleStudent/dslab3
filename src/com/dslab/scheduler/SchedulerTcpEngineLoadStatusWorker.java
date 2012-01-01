/**
 * requests the load State for the specific engine
 */

package com.dslab.scheduler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import com.dslab.Types.GenericTaskEngineStatusEnum;
import com.dslab.entities.GenericTaskEngineEntity;

public class SchedulerTcpEngineLoadStatusWorker implements Runnable {

	private GenericTaskEngineEntity engine;

	public SchedulerTcpEngineLoadStatusWorker(GenericTaskEngineEntity e) {
		this.engine = e;
	}

	@Override
	public void run() {
		try {
			System.out.println(engine.getIp());
			System.out.println(engine.getTcpPort());
			Socket engineTcpSocket = new Socket(engine.getIp(), engine.getTcpPort());
			DataOutputStream outToServer = new DataOutputStream(engineTcpSocket.getOutputStream());
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(engineTcpSocket.getInputStream()));
			outToServer.writeBytes("!loadState" + "\n");
			SchedulerHelper.fillEngineWithStateRequestData(engine, inFromServer.readLine());
			// engine.setLoad(Integer.parseInt(inFromServer.readLine()));

		} catch (UnknownHostException e1) {
			System.out.println("Server does not respond");
		} catch (IOException e2) {
			engine.setState(GenericTaskEngineStatusEnum.offline);
		}
	}
}
