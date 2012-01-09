package com.dslab.remoteobjects;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;
import org.omg.CORBA.Object;

import com.dslab.Cipher.KeyWorker;
import com.dslab.Types.TaskStatusEnum;
import com.dslab.Types.TypeEnum;
import com.dslab.client.ClientCallbackRemoteObjectInterface;
import com.dslab.entities.CompanyEntity;
import com.dslab.entities.GenericTaskEngineEntity;
import com.dslab.entities.Output;
import com.dslab.entities.TaskEntity;
import com.dslab.management.ManagementServiceHelper;
import com.dslab.management.ManagementServiceModel;
import com.dslab.management.ManagementTcpEngineWorker;
import com.dslab.management.ManagementTcpEngineWorkerDistributed;
import com.dslab.management.ManagementTcpSchedulerListenerWorker;

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
		Runnable task = new ManagementTcpSchedulerListenerWorker(model);
		model.getExecutorTcp().execute(task);
	}

	@Override
	public synchronized String executeTaskForId(int id, ClientCallbackRemoteObjectInterface callback, String call)
			throws RemoteException {
		if (model.getSchedulerSocket() == null || model.getSchedulerSocket().isClosed()) {
			try {
				model.setSchedulerSocket(new Socket(model.getSchedulerHost(), model.getSchedulerTCPPort()));
				// startSchedulerListener();
			} catch (UnknownHostException e) {
				return "Error: Scheduler is not reachable";
			} catch (IOException e) {
				return "Error: Scheduler is not reachable";
			}
		}
		if (!model.getAuthenticated()) {
			return "Error: Not correctly authenticated. No secure connection available";
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
				DataOutputStream outToScheduler = new DataOutputStream(model.getSchedulerSocket().getOutputStream());

				byte[] outBytes = sendEncrypted(currentTask, ("!requestEngine " + currentTask.getType().name() + " "
						+ activeCompany.getName() + "\n").getBytes());
				outToScheduler.write(outBytes);

				// outToScheduler.writeBytes("!requestEngine " + currentTask.getType().name() + " "
				// + activeCompany.getName() + "\n");

				String response = new String(receiveEncrypted());

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
				e.printStackTrace();
				System.out.println(e.getMessage());
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return "";
	}

	private byte[] receiveEncrypted() throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IOException {
		byte[] inputBytes2 = new byte[684];
		InputStream is = model.getSchedulerSocket().getInputStream();
		is.read(inputBytes2, 0, inputBytes2.length);
		inputBytes2 = Base64.decode(inputBytes2);
		return KeyWorker.getCipherForAlgorithm("AES/CTR/NoPadding", false,
				new SecretKeySpec(model.getCipherStuff().getSecretKey(), "AES"),
				model.getCipherStuff().getIvParameter()).doFinal(inputBytes2);
	}

	private byte[] sendEncrypted(TaskEntity currentTask, byte[] message) throws IllegalBlockSizeException,
			BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException {
		byte[] outBytes = new byte[684];
		outBytes = KeyWorker.getCipherForAlgorithm("AES/CTR/NoPadding", true,
				new SecretKeySpec(model.getCipherStuff().getSecretKey(), "AES"),
				model.getCipherStuff().getIvParameter()).doFinal(Base64.encode(message));
		outBytes = Base64.encode(outBytes);
		return outBytes;
	}

	@Override
	public String executeDistributedForId(int id, int amount, ClientCallbackRemoteObjectInterface callback, String call)
			throws RemoteException {
		if (model.getSchedulerSocket() == null) {
			try {
				model.setSchedulerSocket(new Socket(model.getSchedulerHost(), model.getSchedulerTCPPort()));
				// startSchedulerListener();
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
				ArrayList<GenericTaskEngineEntity> engines = new ArrayList<GenericTaskEngineEntity>();
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(model.getSchedulerSocket()
						.getInputStream()));
				DataOutputStream outToServer = new DataOutputStream(model.getSchedulerSocket().getOutputStream());
				outToServer.writeBytes("!requestEngine " + currentTask.getType().name() + " " + activeCompany.getName()
						+ "\n");
				String response = null;
				for (int i = 0; i < amount; i++) {
					outToServer.writeBytes("!requestEngine " + currentTask.getType().name() + " "
							+ activeCompany.getName() + "\n");

					response = inFromServer.readLine();
					if (response.split(" ")[0].equals("!requestEngine")) {
						engines.add(new GenericTaskEngineEntity(response.split(" ")[1], Integer.parseInt(response
								.split(" ")[2])));
					}
				}
				model.getCurrentRequestedTask().setDistributedAmount(amount);

				int count = 1;
				for (GenericTaskEngineEntity currentEngine : engines) {
					model.getCurrentRequestedTask().setAssignedEngine(
							new GenericTaskEngineEntity(response.split(" ")[1],
									Integer.parseInt(response.split(" ")[2])));
					Runnable worker = new ManagementTcpEngineWorkerDistributed(new Socket(currentEngine.getIp(),
							currentEngine.getTcpPort()), model.getCurrentRequestedTask(), model, count, amount);
					model.getExecutorTcp().execute(worker);
					count++;
				}
				ManagementServiceHelper.getCompanyForName(response.split("#")[1], model).getCallback()
						.printInfo("Execution for task " + model.getCurrentRequestedTask().getId() + " started.");

			} catch (UnknownHostException e) {
				System.out.println("Server not responding.");
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
		}
		return "";
	}

	// public String dexecuteDistributedForId(int id, int amount, ClientCallbackRemoteObjectInterface callback, String
	// call) throws RemoteException { if (model.getSchedulerSocket() == null) { try {
	// model.setSchedulerSocket(new Socket(model.getSchedulerHost(), model.getSchedulerTCPPort())); //
	// startSchedulerListener(); } catch (UnknownHostException e) { return "Error: Scheduler is not reachable"; } catch
	// (IOException e) { return "Error: Scheduler is not reachable"; } } TaskEntity currentTask =
	// ManagementServiceHelper.getTaskForId(model.getTasks(), id); if (currentTask == null) { return "Error: Task " + id
	// + " does not exist."; } else if (currentTask.getStatus() == TaskStatusEnum.executing) { return
	// "Error: Execution has already been started."; } else if (currentTask.getOwnerCompany() != activeCompany) { throw
	// new RemoteException(); } else { currentTask.setCall(call); currentTask.setStatus(TaskStatusEnum.executing);
	// currentTask.setCosts(0); try { ArrayList<GenericTaskEngineEntity> engines = new
	// ArrayList<GenericTaskEngineEntity>();
	//
	// activeCompany.setCallback(callback); ManagementServiceModel.setCurrentRequestedTask(currentTask);
	// currentTask.setDistributedAmount(amount); currentTask.setOutputs(new HashMap()); BufferedReader inFromServer =
	// new BufferedReader(new InputStreamReader(model.getSchedulerSocket() .getInputStream())); DataOutputStream
	// outToServer = new DataOutputStream(model.getSchedulerSocket().getOutputStream()); for (int i = 0; i < amount;
	// i++) { outToServer.writeBytes("!requestEngine " + currentTask.getType().name() + " " + activeCompany.getName() +
	// "\n");
	//
	// String response = inFromServer.readLine(); if (response.split(" ")[0].equals("!requestEngine")) { engines.add(new
	// GenericTaskEngineEntity(response.split(" ")[1], Integer.parseInt(response .split(" ")[2]))); } //
	// System.out.println(currentEngine.getIp()); // System.out.println(currentEngine.getTcpPort()); Socket s = new
	// Socket(response.split(" ")[1], Integer.parseInt(response.split(" ")[2])); Socket s2 = new Socket("localhost",
	// 13492); // Runnable worker = new ManagementTcpEngineWorkerDistributed(new Socket(currentEngine.getIp(), //
	// currentEngine.getTcpPort()), ManagementServiceModel.getCurrentRequestedTask(), model, part, // amount); //
	// part++; // model.getExecutorTcp().execute(worker); }
	//
	// int part = 1; for (GenericTaskEngineEntity currentEngine : engines) {
	//
	// // } else { // ManagementServiceModel.getCurrentRequestedTask().setStatus(TaskStatusEnum.prepared); //
	// ManagementServiceHelper.getCompanyForName(response.split("#")[1], model).getCallback() //
	// .printInfo(response.split("#")[0]); // } } activeCompany.getCallback().printInfo( "Execution for task " +
	// ManagementServiceModel.getCurrentRequestedTask().getId() + " started.");
	//
	// } catch (UnknownHostException e) { System.out.println("Server not responding."); } catch (IOException e) {
	// System.out.println(e.getMessage()); } } return ""; }

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
	public Output getOutputForId(int id) throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		String returnValue = "";
		if (ManagementServiceHelper.getTaskForId(model.getTasks(), id) == null) {
			return new Output("Error: Task " + id + " does not exist.", null);
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
					returnValue = ManagementServiceHelper.getTaskForId(model.getTasks(), id).getOutputs().get(0)
							.toString();
					ManagementServiceHelper.getTaskForId(model.getTasks(), id).setPayed(true);
					activeCompany.setCredits(activeCompany.getCredits()
							- ManagementServiceHelper.getTaskForId(model.getTasks(), id).getCosts());
					activeCompany.setPayedTasksCount(activeCompany.getPayedTasksCount() + 1);
				}

			} else {
				if (ManagementServiceHelper.getTaskForId(model.getTasks(), id).getOutputs().size() > 1) {
					for (int x = 1; x < ManagementServiceHelper.getTaskForId(model.getTasks(), id).getOutputs().size() + 1; x++) {
						returnValue = returnValue
								+ ManagementServiceHelper.getTaskForId(model.getTasks(), id).getOutputs().get(x)
										.toString();
					}
				} else {
					returnValue = ManagementServiceHelper.getTaskForId(model.getTasks(), id).getOutputs().get(0)
							.toString();
				}
			}
		} else {
			returnValue = "Error: Task " + id + " does not belong to your company.";
		}

		return new Output(returnValue, KeyWorker.createHashMac(
				KeyWorker.readSharedSecretKey(model.getHmacKeyPath() + "/"
						+ model.getCurrentRequestedTask().getOwnerCompany().getName() + ".key", "HmacSHA256"),
				"HmacSHA256", returnValue));
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

}
