package com.dslab.remoteobjects;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import org.omg.CORBA.Object;

import com.dslab.Types.TaskStatusEnum;
import com.dslab.Types.TypeEnum;
import com.dslab.client.ClientCallbackRemoteObjectInterface;
import com.dslab.entities.CompanyEntity;
import com.dslab.entities.GenericTaskEngineEntity;
import com.dslab.entities.TaskEntity;
import com.dslab.management.ManagementServiceHelper;
import com.dslab.management.ManagementServiceModel;
import com.dslab.management.ManagementTcpEngineWorker;

public class ClientRemoteObject extends UnicastRemoteObject implements RemoteObjectInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3584053989293557335L;
	private ManagementServiceModel model;
	private CompanyEntity activeCompany;

	public ClientRemoteObject(ManagementServiceModel model, CompanyEntity activeCompany) throws RemoteException {
		super();
		this.model = model;
		this.activeCompany = activeCompany;
		// TODO Auto-generated constructor stub
	}

	@Override
	public int logout() throws RemoteException {
		activeCompany.setActive(false);
		return 0;
	}

	public int getCurrentCredits(String client) {
		return 0;
	}

	public void executeTask(Object callback) {

	}

	public TaskEntity getTaskInfo(int taskId) {
		return null;

	}

	public String getTaskOutput(int taskId) {
		return null;

	}

	@Override
	public boolean isAdmin() throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getPricingCurve() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int prepareTask(String name, TypeEnum type, byte[] bytes) throws RemoteException {
		if (activeCompany.getCredits() >= model.getPreparationCosts()) {

			boolean success = new File(model.getTaskDir()).mkdirs();
			if (!success) {
				// System.out.println("File);
			}

			File file = new File(model.getTaskDir(), name);
			BufferedOutputStream output;
			try {
				output = new BufferedOutputStream(new FileOutputStream(file));
				output.write(bytes, 0, bytes.length);
				output.flush();
				output.close();
			} catch (FileNotFoundException e) {
				System.out.println("Task not found.");
			} catch (IOException e) {
				System.out.println("Error: unexpected Error occured");
			}

			activeCompany.setCredits(activeCompany.getCredits()
					- (int) (model.getPreparationCosts() - model.getPreparationCosts()
							* ManagementServiceHelper.getDiscountForTaskCount(model, activeCompany)));
			TaskEntity t = new TaskEntity(name, type);
			t.setStatus(TaskStatusEnum.prepared);
			activeCompany.getPreparedTasks().add(t);
			t.setOwnerCompany(activeCompany);
			model.getTasks().add(t);
			return t.getId();
		} else {
			return 0;
		}
	}

	@Override
	public TaskEntity getPreparedTaskForId(int id) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	private void startSchedulerListener() {
		// Runnable task = new ManagementTcpSchedulerListenerWorker(model);
		// model.getExecutorTcp().execute(task);
	}

	@Override
	public synchronized String executeTaskForId(int id, ClientCallbackRemoteObjectInterface callback, String call)
			throws RemoteException {
		if (model.getSchedulerSocket() == null) {
			try {
				model.setSchedulerSocket(new Socket(model.getSchedulerHost(), model.getSchedulerTCPPort()));
				startSchedulerListener();
			} catch (UnknownHostException e) {
				return "Error: Scheduler is not reachable";
			} catch (IOException e) {
				return "Error: Scheduler is not reachable";
			}
		}
		TaskEntity currentTask = ManagementServiceHelper.getTaskForId(model.getTasks(), id);
		if (currentTask == null) {
			return "Error: Task " + id + " does not exist.";
		} else if (currentTask.getStatus() == TaskStatusEnum.executing) {
			return "Error: Execution has already been started.";
		} else if (currentTask.getOwnerCompany() != activeCompany) {
			throw new RemoteException();
		} else {
			currentTask.setCall(call);
			currentTask.setStatus(TaskStatusEnum.executing);
			currentTask.setCosts(0);
			try {
				// clientModel.setInput(input);
				activeCompany.setCallback(callback);
				model.setCurrentRequestedTask(currentTask);
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(model.getSchedulerSocket()
						.getInputStream()));
				DataOutputStream outToServer = new DataOutputStream(model.getSchedulerSocket().getOutputStream());
				outToServer.writeBytes("!requestEngine " + currentTask.getType().name() + " " + activeCompany.getName()
						+ "\n");

				String response = inFromServer.readLine();
				if (response.split(" ")[0].equals("!requestEngine")) {
					model.getCurrentRequestedTask().setAssignedEngine(
							new GenericTaskEngineEntity(response.split(" ")[1],
									Integer.parseInt(response.split(" ")[2])));
					ManagementServiceHelper.getCompanyForName(response.split("#")[1], model).getCallback()
							.printInfo("Execution for task " + model.getCurrentRequestedTask().getId() + " started.");

					model.setSchedulerSocket(new Socket(model.getCurrentRequestedTask().getAssignedEngine().getIp(),
							model.getCurrentRequestedTask().getAssignedEngine().getTcpPort()));
					Runnable worker = new ManagementTcpEngineWorker(model.getSchedulerSocket(),
							model.getCurrentRequestedTask(), model);
					model.getExecutorTcp().execute(worker);
				} else {
					// System.out.println("Server: " + response);
					model.getCurrentRequestedTask().setStatus(TaskStatusEnum.prepared);
					ManagementServiceHelper.getCompanyForName(response.split("#")[1], model).getCallback()
							.printInfo(response.split("#")[0]);
				}

			} catch (UnknownHostException e) {
				System.out.println("Server not responding.");
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		return "";
	}

	@Override
	public String getTaskInfoForId(int id) throws RemoteException {
		TaskEntity currentTask = ManagementServiceHelper.getTaskForId(model.getTasks(), id);
		String returnValue = "";
		if (currentTask == null) {
			return "Error: Task " + id + " does not exist.";
		} else if (currentTask.getOwnerCompany() != activeCompany) {
			throw new RemoteException();
		}
		if (currentTask != null) {
			returnValue = "Task " + id + " (" + currentTask.getFileName() + ")\n";
			returnValue = returnValue + "Type: " + currentTask.getType() + "\n";
			if (currentTask.getAssignedEngine().getTcpPort() == 0) {
				returnValue = returnValue + "Assigned engine: none\n";
			} else {
				returnValue = returnValue + "Assigned engine: " + currentTask.getAssignedEngine().getIp() + ":"
						+ currentTask.getAssignedEngine().getTcpPort() + "\n";
			}
			returnValue = returnValue + "Status: " + currentTask.getStatus() + "\n";
			if (currentTask.getStatus() == TaskStatusEnum.finished) {
				returnValue = returnValue + "Costs: " + currentTask.getCosts();
			} else {
				returnValue = returnValue + "Costs: unknown";
			}
		} else {
			return "task id not available.";
		}
		return returnValue;
	}

	@Override
	public String getOutputForId(int id) throws RemoteException {
		String returnValue = "";
		if (ManagementServiceHelper.getTaskForId(model.getTasks(), id) == null) {
			return "Error: Task " + id + " does not exist.";
		} else if (ManagementServiceHelper.getTaskForId(model.getTasks(), id).getOwnerCompany() != activeCompany) {
			throw new RemoteException();
		}
		if (activeCompany == ManagementServiceHelper.getTaskForId(model.getTasks(), id).getOwnerCompany()) {
			if (ManagementServiceHelper.getTaskForId(model.getTasks(), id).getStatus() != TaskStatusEnum.finished) {
				returnValue = "Error: Task " + id + " has not been finished yet.";
			} else if (!ManagementServiceHelper.getTaskForId(model.getTasks(), id).isPayed()) {
				if (activeCompany.getCredits() < ManagementServiceHelper.getTaskForId(model.getTasks(), id).getCosts()) {
					returnValue = "Error: You do not have enough credits to pay this execution. (Costs: "
							+ ManagementServiceHelper.getTaskForId(model.getTasks(), id).getCosts()
							+ " credits) Buy new credits for retrieving the output.";

				} else {
					returnValue = ManagementServiceHelper.getTaskForId(model.getTasks(), id).getOutputText();
					ManagementServiceHelper.getTaskForId(model.getTasks(), id).setPayed(true);
					activeCompany.setCredits(activeCompany.getCredits()
							- ManagementServiceHelper.getTaskForId(model.getTasks(), id).getCosts());
					activeCompany.setPayedTasksCount(activeCompany.getPayedTasksCount() + 1);
				}

			} else {
				returnValue = ManagementServiceHelper.getTaskForId(model.getTasks(), id).getOutputText();
			}
		} else {
			returnValue = "Error: Task " + id + " does not belong to your company.";
		}

		return returnValue;
	}

	@Override
	public String buyCredits(int amount) throws RemoteException {
		// TODO Auto-generated method stub
		if (amount < 0) {
			throw new RemoteException();
		} else {
			activeCompany.setCredits(activeCompany.getCredits() + amount);
			return "Successfully bought credits. You have " + activeCompany.getCredits() + " credits left.";
		}
	}

	@Override
	public String getCredits() throws RemoteException {
		return "You have " + activeCompany.getCredits() + " credits left.";
	}

	@Override
	public String setPricingCurve(int taskCount, double percent) throws RemoteException {
		return null;
	}

	@Override
	public String executeDistributedForId(int id, int amount, ClientCallbackRemoteObjectInterface callback, String call)
			throws RemoteException {
		if (model.getSchedulerSocket() == null) {
			try {
				model.setSchedulerSocket(new Socket(model.getSchedulerHost(), model.getSchedulerTCPPort()));
				startSchedulerListener();
			} catch (UnknownHostException e) {
				return "Error: Scheduler is not reachable";
			} catch (IOException e) {
				return "Error: Scheduler is not reachable";
			}
		}
		TaskEntity currentTask = ManagementServiceHelper.getTaskForId(model.getTasks(), id);
		if (currentTask == null) {
			return "Error: Task " + id + " does not exist.";
		} else if (currentTask.getStatus() == TaskStatusEnum.executing) {
			return "Error: Execution has already been started.";
		} else if (currentTask.getOwnerCompany() != activeCompany) {
			throw new RemoteException();
		} else {
			currentTask.setCall(call);
			currentTask.setStatus(TaskStatusEnum.executing);
			currentTask.setCosts(0);
			try {
				ArrayList<GenericTaskEngineEntity> engines = new ArrayList<GenericTaskEngineEntity>();

				activeCompany.setCallback(callback);
				ManagementServiceModel.setCurrentRequestedTask(currentTask);
				currentTask.setDistributedAmount(amount);
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(model.getSchedulerSocket()
						.getInputStream()));
				DataOutputStream outToServer = new DataOutputStream(model.getSchedulerSocket().getOutputStream());
				for (int i = 0; i < amount; i++) {
					outToServer.writeBytes("!requestEngine " + currentTask.getType().name() + " "
							+ activeCompany.getName() + "\n");

					String response = inFromServer.readLine();
					if (response.split(" ")[0].equals("!requestEngine")) {
						engines.add(new GenericTaskEngineEntity(response.split(" ")[1], Integer.parseInt(response
								.split(" ")[2])));
					}
				}

				for (int i = 0; i < amount; i++) {
					model.setSchedulerSocket(new Socket(ManagementServiceModel.getCurrentRequestedTask()
							.getAssignedEngine().getIp(), ManagementServiceModel.getCurrentRequestedTask()
							.getAssignedEngine().getTcpPort()));
					Runnable worker = new ManagementTcpEngineWorker(new Socket(engines.get(i).getIp(), engines.get(i)
							.getTcpPort()), ManagementServiceModel.getCurrentRequestedTask(), model);
					model.getExecutorTcp().execute(worker);
					// } else {
					// ManagementServiceModel.getCurrentRequestedTask().setStatus(TaskStatusEnum.prepared);
					// ManagementServiceHelper.getCompanyForName(response.split("#")[1], model).getCallback()
					// .printInfo(response.split("#")[0]);
					// }
				}
				ManagementServiceHelper
						.getCompanyForName(response.split("#")[1], model)
						.getCallback()
						.printInfo(
								"Execution for task " + ManagementServiceModel.getCurrentRequestedTask().getId()
										+ " started.");

			} catch (UnknownHostException e) {
				System.out.println("Server not responding.");
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		return "";
	}
}
