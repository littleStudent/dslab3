/**
 * Handles the tcp connection to the Clients.
 */

package com.dslab.scheduler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import com.dslab.Types.*;
import com.dslab.entities.ClientEntity;
import com.dslab.entities.GenericTaskEngineEntity;

public class SchedulerTcpManagementWorker implements Runnable {

	private SchedulerModel model;
	private Socket tcpSocket;

	public SchedulerTcpManagementWorker(SchedulerModel model, Socket tcpSocket) {
		this.model = model;
		this.tcpSocket = tcpSocket;
	}

	@Override
	public void run() {
		try {
			BufferedReader inFromClient = new BufferedReader(
					new InputStreamReader(tcpSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(
					tcpSocket.getOutputStream());
			while (true) {
				String input = inFromClient.readLine();
				if (input != null) {
					if (input.split(" ")[0].equals("!requestEngine")) {
						if (SchedulerHelper
								.getActiveEngines(model.getEngines()).size() > 0) {
							GenericTaskEngineEntity bestEngine = SchedulerHelper
									.getMostEfficientEngine(SchedulerHelper
											.getActiveEngines(model
													.getEngines()), TypeEnum
											.valueOf(input.split(" ")[1]));
							if (TypeEnum.valueOf(input.split(" ")[1]) == TypeEnum.LOW) {
								if (bestEngine.getLoad() + 33 > 99) {
									outToClient
											.writeBytes("Error: No engine available for execution. Please try again later.#"
											+ input.split(" ")[2] + "#\n");
								} else {
									bestEngine
											.setLoad(bestEngine.getLoad() + 33);
									outToClient.writeBytes("!requestEngine "
											+ bestEngine.getIp() + " "
											+ bestEngine.getTcpPort() + " #"
											+ input.split(" ")[2] + "#\n");
								}
							} else if (TypeEnum.valueOf(input.split(" ")[1]) == TypeEnum.MIDDLE) {
								if (bestEngine.getLoad() + 66 > 99) {
									outToClient
											.writeBytes("Error: No engine available for execution. Please try again later.#"
											+ input.split(" ")[2] + "#\n");
								} else {
									bestEngine
											.setLoad(bestEngine.getLoad() + 66);
									outToClient.writeBytes("!requestEngine "
											+ bestEngine.getIp() + " "
											+ bestEngine.getTcpPort() + " #"
											+ input.split(" ")[2] + "#\n");
								}
							} else if (TypeEnum.valueOf(input.split(" ")[1]) == TypeEnum.HIGH) {
								if (bestEngine.getLoad() + 99 > 99) {
									outToClient
											.writeBytes("Error: No engine available for execution. Please try again later.#"
											+ input.split(" ")[2] + "#\n");
								} else {
									bestEngine
											.setLoad(bestEngine.getLoad() + 99);
									outToClient.writeBytes("!requestEngine "
											+ bestEngine.getIp() + " "
											+ bestEngine.getTcpPort() + " #"
											+ input.split(" ")[2] + "#\n");
								}
							}

						} else {
							outToClient
									.writeBytes("Error: No engine available for execution. Please try again later.#"
											+ input.split(" ")[2] + "#\n");
						}
					}
				} else {
					System.out.println("management service shut down.");
					break;
				}
			}
		} catch (IOException e) {

		}
	}

}
