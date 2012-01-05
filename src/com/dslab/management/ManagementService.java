package com.dslab.management;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

import com.dslab.entities.CompanyEntity;
import com.dslab.remoteobjects.AdminRemoteObject;
import com.dslab.remoteobjects.ClientRemoteObject;
import com.dslab.remoteobjects.RemoteObjectInterface;

public class ManagementService extends UnicastRemoteObject implements ManagementServiceInterface {

	private static final long serialVersionUID = 6625883990856972736L;
	static ManagementServiceModel model;
	private static BufferedReader commandIn = null;
	private static ManagementServiceInterface remoteObject;
	private static ArrayList<RemoteObjectInterface> remoteList;

	protected ManagementService() throws RemoteException {
		super();
		// RmiStarter rmi = new RmiStarter(FactoryServiceInterface.class);
		try {
			// Registry registry = LocateRegistry.getRegistry("localhost:8089");
			Registry registry = null;
			String registryHost = null;
			int registryPort = 0;
			BufferedInputStream inReg;
			BufferedInputStream inMan;
			inReg = new BufferedInputStream(new FileInputStream("src/registry.properties"));
			inMan = new BufferedInputStream(new FileInputStream("src/manager.properties"));
			if (inReg != null) {
				java.util.Properties props = new java.util.Properties();
				try {
					props.load(inReg);
					registryHost = props.getProperty("registry.host");
					registryPort = Integer.parseInt(props.getProperty("registry.port"));
				} finally {
					inReg.close();
				}
			} else {
				System.err.println("Properties file not found!");
			}

			if (inMan != null) {
				java.util.Properties props = new java.util.Properties();
				try {
					props.load(inMan);
					model.setSchedulerTCPPort(Integer.parseInt(props.getProperty("scheduler.tcp.port")));
					model.setSchedulerKeyPub(props.getProperty("key.en"));
					model.setManagerKeyPri(props.getProperty("key.de"));
				} finally {
					inReg.close();
				}
			} else {
				System.err.println("Properties file not found!");
			}

			try {
				registry = java.rmi.registry.LocateRegistry.createRegistry(registryPort);
			} catch (Exception e) {

				registry = java.rmi.registry.LocateRegistry.getRegistry(registryHost, registryPort);
			}
			registry.rebind(model.getBindingName(), this);
			System.out.println("Management Service registered.");
		} catch (Exception e) {
			System.err.println("Error registering echo service factory: " + e.getMessage());
			System.out.println("Error: unexpected Error occured");
		}
	}

	public static void main(String[] args) {

		if (args.length != 4) {
			System.err.println("Usage: java Management asdfasdfasdf <schedulerHost> <schedulerTCPPort> <taskDir>");
			System.exit(1);
		} else {
			ManagementServiceModel.setBindingName(args[0]);
			ManagementServiceModel.setSchedulerHost(args[1]);
			ManagementServiceModel.setPreparationCosts(Integer.parseInt(args[2]));
			ManagementServiceModel.setTaskDir(args[3]);
			model = new ManagementServiceModel();
			ArrayList<Integer> prices = new ArrayList<Integer>();
			prices.add(0);
			model.setPriceSteps(prices);
			HashMap<Integer, Double> priceSteps = new HashMap<Integer, Double>();
			priceSteps.put(0, 0.00);
			model.setPriceStepsMap(priceSteps);
		}

		try {
			remoteObject = new ManagementService();
			loadUserProperties();
			checkConsoleInput();

		} catch (RemoteException e) {
			System.err.println("Error creating echo service factory: " + e.getMessage());
		}
	}

	public static void loadUserProperties() {
		BufferedInputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream("src/user.properties"));
			if (in != null) {
				model.setCompaniesProperty(new java.util.Properties());
				model.getCompaniesProperty().load(in);
				model.refreshCompanies();
			} else {
				System.out.println("Could not load the property list");
			}
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Error: unexpected Error occured");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error: unexpected Error occured");
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
				while (!model.isExit()) {
					final String input;
					try {
						input = commandIn.readLine();
						if (input.equals("!exit")) {
							model.setExit(true);
							exitManagement();
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

	private static void handleInput(String input) {
		if (input.substring(0, input.length()).equals("!users")) {
			listAllCompanies();
		} else if (input.substring(0, input.length()).equals("!exit")) {
			exitManagement();
		} else {
			System.out.println("INCORRECT INPUT");
		}
	}

	private static void exitManagement() {
		model.setExit(true);
		try {
			UnicastRemoteObject.unexportObject(remoteObject, true);
			if (remoteList != null) {
				for (RemoteObjectInterface temp : remoteList) {
					UnicastRemoteObject.unexportObject(temp, true);
				}
			}
			if (model.getExecutorTcp() != null) {
				model.getExecutorTcp().shutdown();
			}
			if (model.getSchedulerSocket() != null) {
				model.getSchedulerSocket().close();
			}
			if (model.getEngineSocket() != null) {
				model.getEngineSocket().close();
			}
			if (commandIn != null) {
				commandIn.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// System.out.println("Error: unexpected Error occured");
		}
	}

	/**
	 * prints all Companies. Includeng the actual state and all tasks
	 */
	private static void listAllCompanies() {
		int count = 1;
		for (CompanyEntity c : model.getCompanies()) {
			if (c.isAdmin()) {
				if (c.getActive()) {
					System.out.println(count + ". " + c.getName() + " (online)");
				} else {
					System.out.println(count + ". " + c.getName() + " (offline)");
				}
			} else {
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

	@Override
	public RemoteObjectInterface authentication(String login, String password) throws RemoteException {
		if (remoteList == null) {
			remoteList = new ArrayList<RemoteObjectInterface>();
		}
		if (ManagementServiceHelper.checkLogin(model.getCompaniesProperty(), login, password)) {
			if (model.getCompaniesProperty().getProperty(login + ".admin").equals("true")) {
				ManagementServiceHelper.getCompanyForName(login, model).setActive(true);
				RemoteObjectInterface temp = new AdminRemoteObject(model, ManagementServiceHelper.getCompanyForName(
						login, model));
				remoteList.add(temp);
				return temp;
			} else {
				ManagementServiceHelper.getCompanyForName(login, model).setActive(true);
				RemoteObjectInterface temp = new ClientRemoteObject(model, ManagementServiceHelper.getCompanyForName(
						login, model));
				remoteList.add(temp);
				return temp;
			}

		}
		return null;
	}
}