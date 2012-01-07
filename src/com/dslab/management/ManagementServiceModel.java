package com.dslab.management;

import java.net.Socket;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dslab.entities.CompanyEntity;
import com.dslab.entities.TaskEntity;

public class ManagementServiceModel {

	private static java.util.Properties companiesProperty;
	private ArrayList<CompanyEntity> companies;
	private ArrayList<TaskEntity> tasks;
	private boolean exit;
	private static String bindingName;
	private static String schedulerHost;
	private static int schedulerTCPPort;
	private static int preparationCosts;
	private static String taskDir;
	private Socket engineSocket;
	private Socket schedulerSocket;
	private static TaskEntity currentRequestedTask;
	private HashMap<Integer, Double> priceStepsMap = new HashMap<Integer, Double>();
	private ArrayList<Integer> priceSteps = new ArrayList<Integer>();
	private ExecutorService executorTcp;
	private String schedulerKeyPub;
	private String managerKeyPri;
	private String hmacKeyPath;
	private PrivateKey privateKey;
	private PublicKey publicKey;

	protected ManagementServiceModel() {
		setTasks(new ArrayList<TaskEntity>());
		setExecutorTcp(Executors.newFixedThreadPool(20));

	}

	public static java.util.Properties getCompaniesProperty() {
		return companiesProperty;
	}

	public static void setCompaniesProperty(java.util.Properties companiesProperty) {
		ManagementServiceModel.companiesProperty = companiesProperty;
	}

	public synchronized ArrayList<CompanyEntity> getCompanies() {
		return companies;
	}

	public synchronized void setCompanies(ArrayList<CompanyEntity> companies) {
		this.companies = companies;
	}

	/**
	 * refreshes all companies
	 */
	public synchronized void refreshCompanies() {
		setCompanies(new ArrayList<CompanyEntity>());
		java.util.Set<String> companyNames = companiesProperty.stringPropertyNames(); // get all company names
		for (String companyName : companyNames) {
			if (companyName.contains(".")) {
				if (getCompanyForName(companyName.split("\\.")[0]) != null) {
					if (companyName.split("\\.")[1].equals("admin")) {
						if (companiesProperty.getProperty(companyName).equals("true")) {
							getCompanyForName(companyName.split("\\.")[0]).setAdmin(true);
						} else {
							getCompanyForName(companyName.split("\\.")[0]).setAdmin(false);
						}
					} else {
						getCompanyForName(companyName.split("\\.")[0]).setCredits(
								Integer.parseInt(companiesProperty.getProperty(companyName)));
					}
				} else {
					companies.add(new CompanyEntity(companyName.split("\\.")[0], ""));
					if (companyName.split("\\.")[1].equals("admin")) {
						if (companiesProperty.getProperty(companyName).equals("true")) {
							getCompanyForName(companyName.split("\\.")[0]).setAdmin(true);
						} else {
							getCompanyForName(companyName.split("\\.")[0]).setAdmin(false);
						}
					} else {
						getCompanyForName(companyName.split("\\.")[0]).setCredits(
								Integer.parseInt(companiesProperty.getProperty(companyName)));
					}
				}
			} else {
				if (getCompanyForName(companyName) == null) {
					companies.add(new CompanyEntity(companyName, companiesProperty.getProperty(companyName)));
				} else {
					getCompanyForName(companyName).setPassword(companiesProperty.getProperty(companyName));
				}
			}
		}
	}

	private synchronized CompanyEntity getCompanyForName(String name) {
		for (CompanyEntity company : companies) {
			if (company.getName().equals(name)) {
				return company;
			}
		}
		return null;
	}

	public synchronized boolean isExit() {
		return exit;
	}

	public synchronized void setExit(boolean exit) {
		this.exit = exit;
	}

	public synchronized static String getBindingName() {
		return bindingName;
	}

	public synchronized static void setBindingName(String bindingName) {
		ManagementServiceModel.bindingName = bindingName;
	}

	public synchronized static String getSchedulerHost() {
		return schedulerHost;
	}

	public synchronized static void setSchedulerHost(String schedulerHost) {
		ManagementServiceModel.schedulerHost = schedulerHost;
	}

	public synchronized static int getSchedulerTCPPort() {
		return schedulerTCPPort;
	}

	public synchronized static void setSchedulerTCPPort(int schedulerTCPPort) {
		ManagementServiceModel.schedulerTCPPort = schedulerTCPPort;
	}

	public synchronized String getTaskDir() {
		return taskDir;
	}

	public synchronized static void setTaskDir(String taskDir) {
		ManagementServiceModel.taskDir = taskDir;
	}

	public synchronized static int getPreparationCosts() {
		return preparationCosts;
	}

	public synchronized static void setPreparationCosts(int preparationCosts) {
		ManagementServiceModel.preparationCosts = preparationCosts;
	}

	public synchronized ArrayList<TaskEntity> getTasks() {
		return tasks;
	}

	public synchronized void setTasks(ArrayList<TaskEntity> tasks) {
		this.tasks = tasks;
	}

	public synchronized Socket getEngineSocket() {
		return engineSocket;
	}

	public synchronized void setEngineSocket(Socket engineSocket) {
		this.engineSocket = engineSocket;
	}

	public synchronized Socket getSchedulerSocket() {
		return schedulerSocket;
	}

	public synchronized void setSchedulerSocket(Socket schedulerSocket) {
		this.schedulerSocket = schedulerSocket;
	}

	public synchronized static TaskEntity getCurrentRequestedTask() {
		return currentRequestedTask;
	}

	public synchronized static void setCurrentRequestedTask(TaskEntity currentRequestedTask) {
		ManagementServiceModel.currentRequestedTask = currentRequestedTask;
	}

	public synchronized HashMap<Integer, Double> getPriceStepsMap() {
		return priceStepsMap;
	}

	public synchronized void setPriceStepsMap(HashMap<Integer, Double> priceStepsMap) {
		this.priceStepsMap = priceStepsMap;
	}

	public synchronized ArrayList<Integer> getPriceSteps() {
		return priceSteps;
	}

	public synchronized void setPriceSteps(ArrayList<Integer> priceSteps) {
		this.priceSteps = priceSteps;
	}

	public synchronized ExecutorService getExecutorTcp() {
		return executorTcp;
	}

	public synchronized void setExecutorTcp(ExecutorService executorTcp) {
		this.executorTcp = executorTcp;
	}

	public String getSchedulerKeyPub() {
		return schedulerKeyPub;
	}

	public void setSchedulerKeyPub(String schedulerKeyPub) {
		this.schedulerKeyPub = schedulerKeyPub;
	}

	public String getManagerKeyPri() {
		return managerKeyPri;
	}

	public void setManagerKeyPri(String managerKeyPri) {
		this.managerKeyPri = managerKeyPri;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public String getHmacKeyPath() {
		return hmacKeyPath;
	}

	public void setHmacKeyPath(String hmacKeyPath) {
		this.hmacKeyPath = hmacKeyPath;
	}

}
