package com.dslab.management;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import com.dslab.Types.TaskStatusEnum;
import com.dslab.Types.TypeEnum;
import com.dslab.entities.TaskEntity;

public class ManagementTcpEngineWorker implements Runnable {

	private Socket socket;
	private TaskEntity taskToExecute;
	private ManagementServiceModel model;

	public ManagementTcpEngineWorker(Socket socket, TaskEntity taskToExecute, ManagementServiceModel model) {
		this.socket = socket;
		this.taskToExecute = taskToExecute;
		this.model = model;
	}

	@Override
	public void run() {

		if (taskToExecute.getAssignedEngine() != null) {
			try {
				DataOutputStream outToEngine = new DataOutputStream(socket.getOutputStream());
				BufferedReader inFromEngine = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				outToEngine.writeBytes("!executeTask " + taskToExecute.getFileName() + " " + taskToExecute.getType()
						+ " \"" + taskToExecute.getCall() + "\"" + "\n");

				File f = new File(model.getTaskDir(), taskToExecute.getFileName());
				byte[] mybytearray = new byte[(int) f.length()];
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
				bis.read(mybytearray, 0, mybytearray.length);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
				OutputStream os = socket.getOutputStream();
				System.out.println("Sending...");
				os.write(mybytearray, 0, mybytearray.length);
				os.flush();
				System.out.println("Sent...");

				Runnable task = new Charge(taskToExecute, model);
				Thread worker = new Thread(task);
				worker.start();

				String i = inFromEngine.readLine();
				if (i != null) {
					while (!i.equals("finished transaction")) {
						// System.out.println("Server: " + i);
						try {
							i = inFromEngine.readLine();
							taskToExecute.getOutputs().put(0, taskToExecute.getOutputs().get(0) + "\n" + i);
						} catch (IOException e) {
							System.out.println("something went wrong");
							break;
						}
						if (i == null) {
							// System.out.println("something went wrong");
							break;
						}
					}
					taskToExecute.setStatus(TaskStatusEnum.finished);
					taskToExecute.setCosts((int) (taskToExecute.getCosts() - taskToExecute.getCosts()
							* ManagementServiceHelper.getDiscountForTaskCount(model, taskToExecute.getOwnerCompany())));
					if (taskToExecute.getType() == TypeEnum.LOW) {
						taskToExecute.getOwnerCompany().setLowCount(taskToExecute.getOwnerCompany().getLowCount() + 1);
					} else if (taskToExecute.getType() == TypeEnum.MIDDLE) {
						taskToExecute.getOwnerCompany().setMiddleCount(
								taskToExecute.getOwnerCompany().getMiddleCount() + 1);
					} else if (taskToExecute.getType() == TypeEnum.HIGH) {
						taskToExecute.getOwnerCompany()
								.setHighCount(taskToExecute.getOwnerCompany().getHighCount() + 1);
					}
					if (taskToExecute.getOwnerCompany().getCredits() < taskToExecute.getCosts()) {
						taskToExecute
								.getOwnerCompany()
								.getCallback()
								.printInfo(
										"Execution of task " + taskToExecute.getId() + " finished. You need at least "
												+ taskToExecute.getCosts() + " credits to see the results of task "
												+ taskToExecute.getId());
						taskToExecute.setPayed(false);
					} else {
						taskToExecute.getOwnerCompany().getCallback()
								.printInfo("Execution of task " + taskToExecute.getId() + " finished.");
						taskToExecute.getOwnerCompany().setCredits(
								taskToExecute.getOwnerCompany().getCredits() - taskToExecute.getCosts());
						taskToExecute.setPayed(true);
						taskToExecute.getOwnerCompany().setPayedTasksCount(
								taskToExecute.getOwnerCompany().getPayedTasksCount() + 1);
					}

					// taskToExecute.getOwnerCompany().getCallback().printInfo(taskToExecute.getOwnerCompany().getOutputText());
				}
			} catch (UnknownHostException e) {
				System.out.println("Server not responding.");
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		} else {
			System.out.println("Task with Id " + " prepared, but no engine assigned.");
		}

	}

	static class Charge implements Runnable {
		private TaskEntity currentTask;
		private ManagementServiceModel model;

		protected Charge(TaskEntity currentTask, ManagementServiceModel model) {
			this.currentTask = currentTask;
			this.model = model;
		}

		@Override
		public void run() {
			try {
				while (currentTask.getStatus() != TaskStatusEnum.finished && !model.isExit()) {
					currentTask.setCosts(currentTask.getCosts() + 10);
					Thread.sleep(60000);
				}
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}
	}

}
