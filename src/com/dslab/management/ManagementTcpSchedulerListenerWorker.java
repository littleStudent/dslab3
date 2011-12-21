/**
 * Listens and reacts to the tcp connection Client - Scheduler
 */

package com.dslab.management;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.dslab.Types.TaskStatusEnum;
import com.dslab.client.ClientModel;
import com.dslab.entities.GenericTaskEngineEntity;
import com.dslab.entities.TaskEntity;
import com.dslab.scheduler.SchedulerTcpManagementWorker;

public class ManagementTcpSchedulerListenerWorker implements Runnable {
	private ManagementServiceModel model;
	private Socket schedulerSocket;

	public ManagementTcpSchedulerListenerWorker(ManagementServiceModel model) {
		this.model = model;
	}
	
	private TaskEntity getTaskById(int input) {

		for( TaskEntity t : model.getTasks()) {
			if(t.getId() == input)
				return t;
		}
		return null;
	}

	@Override
	public void run() {
		try {
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(model.getSchedulerSocket().getInputStream()));
			while(!model.isExit()) {
				String response = inFromServer.readLine();
				//System.out.println(response);
				if(response != null) {
					 if(response.equals("finished transaction")) {
						System.out.println("finished task");
						model.getCurrentRequestedTask().setStatus(TaskStatusEnum.finished);
					} else if(response.split(" ")[0].equals("!requestEngine")) {
						model.getCurrentRequestedTask().setAssignedEngine(new GenericTaskEngineEntity(response.split(" ")[1], Integer.parseInt(response.split(" ")[2])));
						ManagementServiceHelper.getCompanyForName(response.split("#")[1], model).getCallback().printInfo("Execution for task " + model.getCurrentRequestedTask().getId() + " started.");
						
						schedulerSocket = new Socket(model.getCurrentRequestedTask().getAssignedEngine().getIp(), model.getCurrentRequestedTask().getAssignedEngine().getTcpPort());
						Runnable worker = new ManagementTcpEngineWorker(schedulerSocket, model.getCurrentRequestedTask(), model);
						model.getExecutorTcp().execute(worker);	
					}
					else {
						//System.out.println("Server: " + response);
						model.getCurrentRequestedTask().setStatus(TaskStatusEnum.prepared);
						ManagementServiceHelper.getCompanyForName(response.split("#")[1], model).getCallback().printInfo(response.split("#")[0]);
						
						
					}
				}
				else {
					schedulerSocket.close();
					System.out.println("logged out from server.");
					break;
				}
			}
		} catch (IOException e) {
			//Thread.
			try {
				schedulerSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println("logged out from server.");
		}
	}
}