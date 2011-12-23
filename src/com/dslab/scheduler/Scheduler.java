/**
 * the main scheduler class. Manages all sockets, threads and console inputs
 */

package com.dslab.scheduler;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		startTcpSocket();
		startUdpSocket();
		checkConsoleInput();
		startDynamicCloudCheck();
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
						Runnable socketWorker = new SchedulerUdpEngineWorker(model, receivePacket, timeout);
						executorUdp.execute(socketWorker);
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