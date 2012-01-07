/**
 * The Client class manages the whole Client process. It starts with the specified arguments and
 * initializes the socket connections. It also handles the console input.
 */

package com.dslab.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import com.dslab.Cipher.KeyWorker;
import com.dslab.Types.TypeEnum;
import com.dslab.entities.Output;
import com.dslab.entities.TaskEntity;
import com.dslab.management.ManagementServiceInterface;
import com.dslab.remoteobjects.RemoteObjectInterface;

public class Client {

	private static ClientModel clientModel;
	private static BufferedReader commandIn = null;
	private static BufferedReader inFromEngine = null;
	private static RemoteObjectInterface remoteObject;
	private static ClientCallbackRemoteObjectInterface callback;
	private static String name;

	protected Client() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length != 2) {
			System.err.println("Usage: java Client <lookup> <tasks>");
		} else {
			clientModel = new ClientModel();
			clientModel.setLookup(args[0]);
			clientModel.setTaskDir(args[1]);
			try {
				callback = new ClientCallbackRemoteObject();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				System.out.println("Error: unexpected Error occured");
				;
			}
		}

		System.out.println("Client started, ready for login ('!login <company> <password>'):");
		initClient();

	}

	/**
	 * initialization is done in this method. its called just ones. Checking the console input in a thread. --useless--
	 * Listening to the Scheduler for input in a thread.
	 */
	private static void initClient() {
		clientModel.setPreparedTasks(new ArrayList<TaskEntity>());
		initProperties();
		checkConsoleInput();
	}

	private static void initProperties() {
		BufferedInputStream inReg;
		try {
			inReg = new BufferedInputStream(new FileInputStream("src/client.properties"));
			if (inReg != null) {
				java.util.Properties props = new java.util.Properties();
				try {
					props.load(inReg);
					clientModel.setSecretKeyPath(props.getProperty("keys.dir"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						inReg.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else {
				System.err.println("Properties file not found!");
			}
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * checking the console for input all the time in a thread
	 */
	private static void checkConsoleInput() {
		new Thread() {
			public void run() {
				InputStreamReader converter = new InputStreamReader(System.in);
				commandIn = new BufferedReader(converter);
				while (!clientModel.getExit()) {
					final String input;
					try {
						input = commandIn.readLine();
						if (input.equals("!exit")) {
							clientModel.setExit(true);
							cmdExit();
						}
						new Thread() {
							public void run() {
								handleInput(input);
							}
						}.start();

					} catch (IOException e) {
						System.out.println(e.getMessage());
						break;
					}
				}
			}
		}.start();
	}

	/**
	 * This method is called from the checkConsoleInput(running an a thread) handleInput decides, depending on @input,
	 * what to do.
	 * 
	 * @param input
	 */
	private static void handleInput(String input) {
		String command = "";
		if (input.lastIndexOf(" ") != -1) {
			command = input.substring(0, input.split(" ")[0].length());
		} else {
			command = input;
		}
		if (input.lastIndexOf(" ") != -1 && command.equals("!login") && getCharCount(input, " ") == 2
				&& clientModel.getRegistered() != true) {
			cmdRegisterClient(input);
		} else if (input.lastIndexOf(" ") == -1 && command.equals("!logout") && clientModel.getRegistered() == true) {
			cmdLogoutClient(input);
		} else if (input.lastIndexOf(" ") == -1 && command.equals("!getPricingCurve")
				&& clientModel.getRegistered() == true) {
			cmdGetPricingCurve();
		} else if (input.lastIndexOf(" ") != -1 && command.equals("!setPriceStep")
				&& clientModel.getRegistered() == true) {
			cmdSetPriceCurve(input);
		} else if (input.lastIndexOf(" ") == -1 && command.equals("!list") && clientModel.getRegistered() == true) {
			cmdListAllTasks();
		} else if (input.lastIndexOf(" ") != -1 && command.equals("!buy") && clientModel.getRegistered() == true) {
			cmdBuyCredits(input);
		} else if (input.lastIndexOf(" ") == -1 && command.equals("!credits") && clientModel.getRegistered() == true) {
			cmdCredits();
		} else if (input.lastIndexOf(" ") != -1 && command.equals("!prepare") && getCharCount(input, " ") == 2
				&& clientModel.getRegistered() == true) {
			cmdPrepareTask(input);
		} else if (input.lastIndexOf(" ") != -1 && command.equals("!executeTask")
				&& clientModel.getRegistered() == true) {
			cmdExecuteTask(input);
		} else if (input.lastIndexOf(" ") != -1 && command.equals("!executeDistributed")
				&& clientModel.getRegistered() == true) {
			cmdExecuteDistributed(input);
		} else if (input.lastIndexOf(" ") != -1 && command.equals("!getOutput") && clientModel.getRegistered() == true) {
			cmdGetOutput(input);
		} else if (input.lastIndexOf(" ") != -1 && command.equals("!info") && getCharCount(input, " ") == 1
				&& clientModel.getRegistered() == true) {
			cmdShowTaskInfo(Integer.parseInt(input.substring(input.split(" ")[0].length() + 1)));
		} else if (command.equals("!exit")) {
			cmdExit();
		} else {
			if (clientModel.getRegistered() == true)
				System.out.println("INCORRECT INPUT");
			else
				System.out.println("You have to log in first.");
		}
	}

	private static void cmdExecuteDistributed(String input) {
		try {
			String output = remoteObject.executeDistributedForId(Integer.parseInt(input.split(" ")[1]),
					Integer.parseInt(input.split(" ")[2]), callback, input.split("\"")[1]);
			if (output == null) {
				System.out.println("Command not allowed. You are not a company.");
			} else {
				System.out.println(output);
			}
		} catch (NumberFormatException e) {
			System.out.println("input error. try again");
		} catch (RemoteException e) {
			System.out.println("Error: Task " + input.split(" ")[1] + " does not belong to your company.");
		}
	}

	private static void cmdSetPriceCurve(String input) {
		try {
			String output = remoteObject.setPricingCurve(Integer.parseInt(input.split(" ")[1]),
					Double.parseDouble(input.split(" ")[2]));
			if (output == null) {
				System.out.println("Command not allowed. You are not a company.");
			} else {
				System.out.println(output);
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			System.out.println("Error: unexpected Error occured");
			;
		} catch (RemoteException e) {
			System.out.println("Error");
		}
	}

	private static void cmdCredits() {
		try {
			String output = remoteObject.getCredits();
			if (output == null) {
				System.out.println("Command not allowed. You are not a company.");
			} else {
				System.out.println(output);
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			System.out.println("Error: unexpected Error occured");
			;
		} catch (RemoteException e) {
			System.out.println("Error: Invalid amount of credits");
		}
	}

	private static void cmdBuyCredits(String input) {
		try {
			String output = remoteObject.buyCredits(Integer.parseInt(input.split(" ")[1]));
			if (output == null) {
				System.out.println("Command not allowed. You are not a company.");
			} else {
				System.out.println(output);
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			System.out.println("Error: unexpected Error occured");
			;
		} catch (RemoteException e) {
			System.out.println("Error: Invalid amount of credits");
		}
	}

	private static void cmdGetOutput(String input) {
		try {
			Output output = remoteObject.getOutputForId(Integer.parseInt(input.split(" ")[1]));
			String message = output.getMessage();
			if (KeyWorker.verifyHash(
					KeyWorker.createHashMac(KeyWorker.readSharedSecretKey(clientModel.getSecretKeyPath() + "/" + name
							+ ".key", "HmacSHA256"), "HmacSHA256", message), output.getHash())) {
				if (output == null) {
					System.out.println("Command not allowed. You are not a company.");
				} else {
					System.out.println(output.getMessage());
				}
			} else {
				System.out.println("It seems like the message was modified, lets try again");
				output = remoteObject.getOutputForId(Integer.parseInt(input.split(" ")[1]));
				message = output.getMessage();
				if (KeyWorker.verifyHash(
						KeyWorker.createHashMac(KeyWorker.readSharedSecretKey(clientModel.getSecretKeyPath() + "/"
								+ name + ".key", "HmacSHA256"), "HmacSHA256", message), output.getHash())) {
					if (output == null) {
						System.out.println("Command not allowed. You are not a company.");
					} else {
						System.out.println(output.getMessage());
					}
				} else {
					System.out.println("It seems like the message was modified again!");

				}
			}

		} catch (NumberFormatException e) {
			System.out.println("Error: unexpected Error occured");
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("Error: Task " + input.split(" ")[1] + " does not belong to your company.");
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void cmdGetPricingCurve() {
		try {
			String output = remoteObject.getPricingCurve();
			if (output == null) {
				System.out.println("Command not allowed. You are not a company.");
			} else {
				System.out.println(output);
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			System.out.println("Error: unexpected Error occured");
			;
		}

	}

	/**
	 * command !exit, closes all sockets and exits the Client
	 */
	private static void cmdExit() {
		try {
			if (remoteObject != null)
				remoteObject.logout();
			clientModel.setRegistered(false);
			System.out.println("Successfully logged out.");
			UnicastRemoteObject.unexportObject(callback, true);
			clientModel.setExit(true);
			if (clientModel.getSchedulerSocket() != null)
				clientModel.getSchedulerSocket().close();
			commandIn.close();
		} catch (IOException e) {

		}

	}

	/**
	 * command !login, registers the client to the scheduler. Checks the username and password, serverside
	 * 
	 * @param input
	 */
	private static void cmdRegisterClient(String input) {
		try {
			name = input.split(" ")[1];
			remoteObject = (RemoteObjectInterface) getRemoteService().authentication(input.split(" ")[1],
					input.split(" ")[2]);
			if (remoteObject != null) {
				clientModel.setRegistered(true);
				if (remoteObject.isAdmin()) {
					clientModel.setAdmin(true);
					System.out.println("Successfully logged in. Using admin mode.");
				} else {
					clientModel.setAdmin(false);
					System.out.println("Successfully logged in. Using company mode.");
				}
			} else {
				clientModel.setRegistered(false);
				System.out.println("Wrong name or password.");
			}
		} catch (RemoteException e1) {
			System.out.println("Management not available.");
		}
	}

	/**
	 * logging out the Client. Sending !logout to the Server
	 * 
	 * @param logout
	 */
	private static void cmdLogoutClient(String input) {

		try {
			remoteObject.logout();
			clientModel.setRegistered(false);
			System.out.println("Successfully logged out.");
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * Command, lists all tasks available in the task directory
	 */
	private static void cmdListAllTasks() {
		if (clientModel.isAdmin()) {
			System.out.println("Command not allowed. You are not a company.");
		} else {
			File dir = new File(clientModel.getTaskDir());
			String[] children = dir.list();
			if (children == null) {
			} else {
				for (int i = 0; i < children.length; i++) {
					String filename = children[i];
					System.out.println(filename);
				}
			}
		}
	}

	/*
	 * private static TaskEntity getTaskForId(ClientModel model) {
	 * 
	 * }
	 */

	/**
	 * Command, shows the info of a specific task
	 * 
	 * @param taskId
	 */
	private static void cmdShowTaskInfo(int taskId) {
		try {
			String output = remoteObject.getTaskInfoForId(taskId);
			if (output == null) {
				System.out.println("Command not allowed. You are not a company.");
			} else {
				System.out.println(output);
			}
		} catch (RemoteException e) {
			System.out.println("Error: Task " + taskId + " does not belong to your company.");
		}

	}

	/**
	 * Command, adds the input task to the prepared Task list
	 * 
	 * @param input
	 */
	private static void cmdPrepareTask(String input) {
		try {
			File file = new File(clientModel.getTaskDir(), input.split(" ")[1]);
			if (!file.exists()) {
				System.out.println("Task not found.");
			} else {

				byte buffer[] = new byte[(int) file.length()];
				BufferedInputStream buffered = new BufferedInputStream(new FileInputStream(file));
				buffered.read(buffer, 0, buffer.length);
				buffered.close();
				int x = remoteObject.prepareTask(input.split(" ")[1], TypeEnum.valueOf(input.split(" ")[2]), buffer);
				if (x == 0) {
					System.out.println("Not enough credits to prepare a task.");
				} else if (x == -1) {
					System.out.println("Scheduler not reachable.");
				} else {
					System.out.println("Task with id " + x + " prepared.");
				}
			}
		} catch (RemoteException e) {
			System.out.println("Command not allowed. You are not a company.");
		} catch (IllegalArgumentException e) {
			System.out.println(input.split(" ")[2] + " is not a valid Task type");
		} catch (IOException e) {
			System.out.println("Error: unexpected Error occured");
		}

	}

	/**
	 * Command, sends the task file to the engine for execution
	 * 
	 * @param input
	 */
	private static void cmdExecuteTask(String input) {
		try {
			String output = remoteObject.executeTaskForId(Integer.parseInt(input.split(" ")[1]), callback,
					input.split("\"")[1]);
			if (output == null) {
				System.out.println("Command not allowed. You are not a company.");
			} else {
				System.out.println(output);
			}
		} catch (NumberFormatException e) {
			System.out.println("input error. try again");
		} catch (RemoteException e) {
			System.out.println("Error: Task " + input.split(" ")[1] + " does not belong to your company.");
		}
	}

	/**
	 * count the characters in a specific string
	 * 
	 * @param text
	 * @param ch
	 * @return
	 */
	private static int getCharCount(String text, String ch) {
		int count = 0;
		String temp = text;
		while (temp.lastIndexOf(ch) != -1) {
			count++;
			temp = temp.substring(temp.indexOf(ch) + 1);
		}
		return count;
	}

	private static ManagementServiceInterface getRemoteService() {

		try {
			String registryHost = null;
			int registryPort = 0;
			BufferedInputStream in;
			in = new BufferedInputStream(new FileInputStream("src/registry.properties"));
			if (in != null) {
				java.util.Properties props = new java.util.Properties();
				try {
					props.load(in);
					registryHost = props.getProperty("registry.host");
					registryPort = Integer.parseInt(props.getProperty("registry.port"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Error: unexpected Error occured");
					;
				} finally {
					try {
						in.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						System.out.println("Error: unexpected Error occured");
						;
					}
				}
			} else {
				System.err.println("Properties file not found!");
			}
			Registry registry = java.rmi.registry.LocateRegistry.getRegistry(registryHost, registryPort);

			return (ManagementServiceInterface) registry.lookup(clientModel.getLookup());
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Error: unexpected Error occured");
			;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Error: unexpected Error occured");
			;
		}
		return null;

	}

}
