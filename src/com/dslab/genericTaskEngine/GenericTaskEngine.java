/**
` * Main class for the generic task engine. Manages all the socket connections and threads needed.
 */

package com.dslab.genericTaskEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dslab.Types.GenericTaskEngineStatusEnum;
import com.dslab.entities.GenericTaskEngineEntity;

public class GenericTaskEngine {

	private static int tcpPort;
	private static String schedulerHost;
	private static int schedulerUDPPort;
	private static int alivePeriod;
	private static int minConsumption;
	private static int maxConsumption;
	private static String taskDir;
	private static ServerSocket schedulerSocket;
	private static DatagramSocket clientSocket;
	private static Socket clientTcpSocket;
	private static boolean interrupted = false;
	private static GenericTaskEngineEntity engine;
	private static BufferedReader commandIn = null;
	private static ExecutorService executor;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 7) {
			System.err
					.println("Usage: java Client <tcpPort> <schedulerHost> <schedulerUDPPort> <alivePeriod> <minConsumption> <maxConsumption> <taskDir>");
			System.exit(1);
		} else {
			tcpPort = Integer.parseInt(args[0]);
			schedulerHost = args[1];
			schedulerUDPPort = Integer.parseInt(args[2]);
			alivePeriod = Integer.parseInt(args[3]);
			minConsumption = Integer.parseInt(args[4]);
			maxConsumption = Integer.parseInt(args[5]);
			taskDir = args[6];
		}
		System.out.println("GTE running, ready for action:");
		engine = new GenericTaskEngineEntity(schedulerHost, tcpPort, schedulerUDPPort, minConsumption, maxConsumption);
		initEngine();
	}

	/**
	 * initialization is done in this method. its called just ones. Start the tcp socket Start the udp socket listen to
	 * the console
	 */
	public static void initEngine() {
		try {
			clientSocket = new DatagramSocket();
		} catch (SocketException e) {
		}
		startTcpSocket();
		startUdpSocket();
		startUdpListener();
		checkConsoleInput();
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
				while (!interrupted) {
					final String input;
					try {
						input = commandIn.readLine();
						if (input.equals("!exit"))
							interrupted = true;
						new Thread() {
							public void run() {
								handleInput(input);
							}
						}.start();
						Thread.yield();
					} catch (IOException e) {
					}
				}
			}
		}.start();
	}

	/**
	 * starts the tcp socket and listens for incoming scheduler / client connections
	 */
	private static void startTcpSocket() {
		new Thread() {
			public void run() {
				try {
					executor = Executors.newFixedThreadPool(20);
					schedulerSocket = new ServerSocket(tcpPort);
					while (!interrupted) {
						clientTcpSocket = schedulerSocket.accept();
						BufferedReader inFromClient = new BufferedReader(new InputStreamReader(
								clientTcpSocket.getInputStream()));
						String input = inFromClient.readLine();
						if (input != null) {
							if (input.split(" ")[0].equals("!loadState") || input.split(" ")[0].equals("!logout")) {
								Runnable worker = new GenericTaskEngineTcpSchedulerWorker(clientTcpSocket, engine,
										input);
								executor.execute(worker);
							} else {
								Runnable worker = new GenericTaskEngineTcpManagementWorker(clientTcpSocket, engine,
										input, taskDir, interrupted);
								executor.execute(worker);
							}
						}
					}
				} catch (UnknownHostException e) {
					System.out.println("Server not responding.");
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			}
		}.start();
	}

	/**
	 * starts the udp socket listener. The scheduler tells the engine to suspend or activate
	 */
	private static void startUdpListener() {
		new Thread() {
			public void run() {
				byte[] receiveData = new byte[1024];
				while (!interrupted) {
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
					try {
						clientSocket.receive(receivePacket);
						String response = new String(receivePacket.getData());
						if (response
								.equals("suspend                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         ")) {
							engine.setState(GenericTaskEngineStatusEnum.suspended);
						} else if (response.equals("activate")) {
							engine.setState(GenericTaskEngineStatusEnum.online);
						}
					} catch (IOException e) {
						// System.out.println(e.getMessage());
						break;
					}
					// System.out.println(engine.getState());
				}
			}
		}.start();
	}

	/**
	 * starts the udp socket for sending and receiving to / from the scheduler
	 */
	private static void startUdpSocket() {
		Runnable task = new GenericTaskEngineUdpWorker(clientSocket, schedulerHost, schedulerUDPPort, tcpPort,
				alivePeriod);
		Thread worker = new Thread(task);
		worker.start();
	}

	/**
	 * handles the console input. depending on the command, different methods have to be called
	 * 
	 * @param input
	 */
	private static void handleInput(String input) {

		if (input.equals("!load")) {
			cmdLoadStatus();
		} else if (input.equals("!exit")) {
			cmdExit();
		}
	}

	/**
	 * killing the Engine after the input "!exit"
	 */
	private static void cmdExit() {
		try {
			interrupted = true;
			schedulerSocket.close();
			executor.shutdown();
			clientSocket.close();
			if (!clientSocket.isClosed())
				clientTcpSocket.close();
		} catch (IOException e) {

		}
		System.out.println("exit successful!");

	}

	/**
	 * write out the current load of the engine.
	 */
	private static void cmdLoadStatus() {

		System.out.println("Current load: " + engine.getLoad() + "% " + engine.getState());

	}

}
