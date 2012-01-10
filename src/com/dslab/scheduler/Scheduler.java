/**
 * the main scheduler class. Manages all sockets, threads and console inputs
 */

package com.dslab.scheduler;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

import com.dslab.Cipher.KeyWorker;
import com.dslab.Types.GenericTaskEngineStatusEnum;
import com.dslab.entities.ClientEntity;
import com.dslab.entities.GenericTaskEngineEntity;

public class Scheduler {

	private static int tcpPort;
	private static int udpPort;
	private static int min;
	private static int max;
	private static int timeout;
	private static int checkPeriod;
	private static SchedulerModel model;
	private static DatagramSocket serverSocket;
	private static ServerSocket schedulerSocket;
	private static Socket clientSocket;
	private static ExecutorService executorTcp;
	private static ExecutorService executorUdp;
	private static BufferedReader commandIn = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		model = new SchedulerModel();
		model.setExit(false);
		if (args.length != 5) {
			System.err.println("Usage: java Client <tcpPort> <udpPort> <min> <max> <timeout> <checkPeriod>");
			System.exit(1);
		} else {
			udpPort = Integer.parseInt(args[0]);
			min = Integer.parseInt(args[1]);
			max = Integer.parseInt(args[2]);
			timeout = Integer.parseInt(args[3]);
			checkPeriod = Integer.parseInt(args[4]);
		}
		System.out.println("Scheduler running, ready for action:");
		initScheduler();
	}

	/**
	 * initializing the scheduler. Called only one time. start tcp socket in a thread. start udp socket in a thread.
	 * listen to the console in a thread.
	 */
	private static void initScheduler() {

		BufferedInputStream inReg;
		try {
			inReg = new BufferedInputStream(new FileInputStream("src/scheduler.properties"));
			if (inReg != null) {
				java.util.Properties props = new java.util.Properties();
				try {
					props.load(inReg);
					tcpPort = Integer.parseInt(props.getProperty("tcp.port"));
					model.setSchedulerPrivateKeyPath(props.getProperty("key.de"));
					model.setManagementPublicKeyPath(props.getProperty("key.en"));
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

		try {
			serverSocket = new DatagramSocket(udpPort);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			System.out.println("Error: unexpected Error occured");
		}
		try {
			model.setPrivateKey(KeyWorker.readPrivateKey(model.getSchedulerPrivateKeyPath()));
			model.setPublicKey(KeyWorker.readPublicKey(model.getManagementPublicKeyPath()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		authentication();
		startTcpSocket();
		startUdpSocket();
		checkConsoleInput();
		startDynamicCloudCheck();
	}

	private static void authentication() {
		// TODO Auto-generated method stub
		try {
			ServerSocket authenticationServerSocket = new ServerSocket(tcpPort);
			Socket authenticationSocket = authenticationServerSocket.accept();
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(
					authenticationSocket.getInputStream()));
			DataOutputStream outToClient = new DataOutputStream(authenticationSocket.getOutputStream());

			InputStream is = authenticationSocket.getInputStream();
			byte[] inputBytes = new byte[684];

			int input = is.read(inputBytes, 0, inputBytes.length);
			inputBytes = Base64.decode(inputBytes);
			inputBytes = KeyWorker.getCipherForAlgorithm("RSA/NONE/OAEPWithSHA256AndMGF1Padding", false,
					model.getPrivateKey(), null).doFinal(inputBytes);
			byte[] managerChallenge = new byte[inputBytes.length - 7];
			System.arraycopy(inputBytes, 7, managerChallenge, 0, inputBytes.length - 7);
			model.getCipherStuff().setManagerChallenge(Base64.decode(managerChallenge));

			byte[] schedulerChallange = KeyWorker.createSecureRandomNumber(32);
			model.getCipherStuff().setSchedulerChallenge(schedulerChallange);
			schedulerChallange = Base64.encode(schedulerChallange);
			byte[] secretKey = KeyWorker.createSecureRandomNumber(32);
			model.getCipherStuff().setSecretKey(secretKey);
			secretKey = Base64.encode(secretKey);
			byte[] ivParameter = KeyWorker.createSecureRandomNumber(16);
			model.getCipherStuff().setIvParameter(ivParameter);
			ivParameter = Base64.encode(ivParameter);

			byte[] okMessage = new byte["!ok    ".getBytes().length + managerChallenge.length
					+ schedulerChallange.length + secretKey.length + ivParameter.length];

			System.arraycopy("!ok ".getBytes(), 0, okMessage, 0, "!ok ".getBytes().length);
			System.arraycopy(managerChallenge, 0, okMessage, "!ok ".getBytes().length, managerChallenge.length);
			System.arraycopy(" ".getBytes(), 0, okMessage, "!ok ".getBytes().length + managerChallenge.length,
					" ".getBytes().length);

			System.arraycopy(schedulerChallange, 0, okMessage, "!ok  ".getBytes().length + managerChallenge.length,
					schedulerChallange.length);
			System.arraycopy(" ".getBytes(), 0, okMessage, "!ok  ".getBytes().length + managerChallenge.length
					+ schedulerChallange.length, " ".getBytes().length);

			System.arraycopy(secretKey, 0, okMessage, "!ok   ".getBytes().length + managerChallenge.length
					+ schedulerChallange.length, secretKey.length);
			System.arraycopy(" ".getBytes(), 0, okMessage, "!ok   ".getBytes().length + managerChallenge.length
					+ schedulerChallange.length + secretKey.length, " ".getBytes().length);

			System.arraycopy(ivParameter, 0, okMessage, "!ok    ".getBytes().length + managerChallenge.length
					+ schedulerChallange.length + secretKey.length, ivParameter.length);

			okMessage = KeyWorker.getCipherForAlgorithm("RSA/NONE/OAEPWithSHA256AndMGF1Padding", true,
					model.getPublicKey(), null).doFinal(okMessage);

			okMessage = Base64.encode(okMessage);

			DataOutputStream outToServer;
			try {
				outToServer = new DataOutputStream(authenticationSocket.getOutputStream());
				outToServer.write(okMessage);

				byte[] inputBytes2 = new byte[60];
				input = is.read(inputBytes2, 0, inputBytes2.length);
				inputBytes2 = Base64.decode(inputBytes2);
				inputBytes2 = KeyWorker.getCipherForAlgorithm("AES/CTR/NoPadding", false,
						new SecretKeySpec(model.getCipherStuff().getSecretKey(), "AES"),
						model.getCipherStuff().getIvParameter()).doFinal(inputBytes2);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			authenticationSocket.close();
			authenticationServerSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	/**
	 * starts the cloud check every 'checkperiod' the scheduler has to decide if another engine has to be suspended or
	 * started suspend or activate messages are sent over udp
	 */
	private static void startDynamicCloudCheck() {
		new Thread() {
			public void run() {
				while (!model.isExit()) {
					try {
						Thread.sleep(checkPeriod);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					SchedulerHelper.refreshAllEngineStates(model);

					final ArrayList<GenericTaskEngineEntity> activeEngines = SchedulerHelper.getActiveEngines(model
							.getEngines());
					final ArrayList<GenericTaskEngineEntity> activeNotLoadedEngines = SchedulerHelper
							.getActiveNotLoadedEngines(model.getEngines());
					final ArrayList<GenericTaskEngineEntity> inactiveEngines = SchedulerHelper.getInactiveEngines(model
							.getEngines());
					final ArrayList<GenericTaskEngineEntity> suspendedEngines = SchedulerHelper
							.getSuspendedEngines(model.getEngines());

					if ((SchedulerHelper.percentLoadCheckRule(activeEngines) || activeEngines.size() < min)
							&& !suspendedEngines.isEmpty() && activeEngines.size() < max) {
						GenericTaskEngineEntity activateTask = SchedulerHelper.getTaskToActivate(suspendedEngines);
						byte[] sendData = new byte[1024];
						String sendString = "activate";
						sendData = sendString.getBytes();
						DatagramPacket sendPacket;
						try {
							sendPacket = new DatagramPacket(sendData, sendData.length,
									InetAddress.getByName(activateTask.getIp()), activateTask.getUdpPort());
							serverSocket.send(sendPacket);
							activateTask.setState(GenericTaskEngineStatusEnum.online);
						} catch (UnknownHostException e) {
							break;
						} catch (IOException e) {
							break;
						}
					}

					if (!SchedulerHelper.zeroLoadCheckRule(activeEngines) && activeEngines.size() > min) {
						GenericTaskEngineEntity suspendTask = SchedulerHelper.getTaskToSuspend(activeNotLoadedEngines);
						byte[] sendData = new byte[1024];
						String sendString = "suspend";
						sendData = sendString.getBytes();
						DatagramPacket sendPacket;
						try {
							sendPacket = new DatagramPacket(sendData, sendData.length,
									InetAddress.getByName(suspendTask.getIp()), suspendTask.getUdpPort());
							serverSocket.send(sendPacket);
							suspendTask.setState(GenericTaskEngineStatusEnum.suspended);
						} catch (UnknownHostException e) {
							System.out.println(e.getMessage());
						} catch (IOException e) {
							System.out.println(e.getMessage());
						}
					}
				}

			}
		}.start();
	}

	/**
	 * checking the console for input all the time in a thread
	 * 
	 * @param callback
	 */
	private static void checkConsoleInput() {
		new Thread() {
			public void run() {
				InputStreamReader converter = new InputStreamReader(System.in);
				commandIn = new BufferedReader(converter);
				while (!model.isExit()) {
					final String input;
					try {
						input = commandIn.readLine();
						if (input.equals("!exit"))
							model.setExit(true);
						new Thread() {
							public void run() {
								handleInput(input);
							}
						}.start();
						Thread.yield();
					} catch (IOException e) {
						System.out.println(e.getMessage());
					}
				}
			}
		}.start();
	}

	/**
	 * Handles the connection to the Client
	 */
	private static void startTcpSocket() {
		new Thread() {
			public void run() {
				try {
					executorTcp = Executors.newFixedThreadPool(20);
					schedulerSocket = new ServerSocket(tcpPort);
					while (!model.isExit()) {
						clientSocket = schedulerSocket.accept();
						Runnable worker = new SchedulerTcpManagementWorker(model, clientSocket);
						executorTcp.execute(worker);
					}
				} catch (UnknownHostException e) {
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			}
		}.start();
	}

	/**
	 * Handles the isAlive messages over udp from the engines.
	 */
	private static void startUdpSocket() {
		new Thread() {
			public void run() {
				byte[] receiveData = new byte[1024];
				executorUdp = Executors.newFixedThreadPool(100);
				while (!model.isExit()) {
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					try {
						serverSocket.receive(receivePacket);
						if (!model.checkIfEngineAlreadyExists(
								Integer.parseInt(new String(receivePacket.getData()).split(" ")[1].trim()),
								receivePacket.getAddress().getHostAddress())) {
							Runnable socketWorker = new SchedulerUdpEngineWorker(model, receivePacket, timeout);
							executorUdp.execute(socketWorker);
						}

					} catch (IOException e) {
						System.out.println(e.getMessage());
					}
				}
			}
		}.start();
	}

	/**
	 * handles the console input. For different commands, different methods have to be called
	 * 
	 * @param input
	 */
	private static void handleInput(String input) {
		if (input.substring(0, input.length()).equals("!companies")) {
			listAllCompanies();
		} else if (input.substring(0, input.length()).equals("!exit")) {
			exitScheduler();
		} else if (input.substring(0, input.length()).equals("!engines")) {
			listAllEngines();
		} else {
			System.out.println("INCORRECT INPUT");
		}
	}

	/**
	 * requests the load state from every engine and prints it.
	 */
	private static void listAllEngines() {
		SchedulerHelper.refreshAllEngineStates(model);

		for (GenericTaskEngineEntity e : model.getEngines()) {
			System.out.println("IP:" + e.getIp() + ", Tcp:" + e.getTcpPort() + ", Udp:" + e.getUdpPort() + ", "
					+ e.getState() + ", Energy Signature: min " + e.getMinConsumption() + "W, max "
					+ e.getMaxConsumption() + "W, Load: " + e.getLoad() + "%");
		}

	}

	/**
	 * exits the scheduler
	 */
	private static void exitScheduler() {
		model.setExit(true);
		try {
			if (schedulerSocket != null) {
				schedulerSocket.close();
			}
			if (serverSocket != null) {
				serverSocket.close();
			}
			if (clientSocket != null) {
				clientSocket.close();
			}
			if (commandIn != null) {
				commandIn.close();
			}
			if (executorTcp != null) {
				executorTcp.shutdown();
			}
			if (executorUdp != null) {
				executorUdp.shutdown();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error: unexpected Error occured");
		}

	}

	/**
	 * prints all Companies. Includeng the actual state and all tasks
	 */
	private static void listAllCompanies() {
		int count = 1;
		for (ClientEntity c : model.getClients()) {
			if (c.getActive()) {
				System.out.println(count + ". " + c.getName() + " (online): LOW " + c.getLowCount() + ", MIDDLE "
						+ c.getMiddleCount() + ", HIGH " + c.getHighCount());
			} else {
				System.out.println(count + ". " + c.getName() + " (offline): LOW " + c.getLowCount() + ", MIDDLE "
						+ c.getMiddleCount() + ", HIGH " + c.getHighCount());
			}
			count++;
		}

	}
}
