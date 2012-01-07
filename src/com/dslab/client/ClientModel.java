package com.dslab.client;

import java.net.Socket;
import java.util.ArrayList;

import com.dslab.entities.TaskEntity;

public class ClientModel {

	private String input;
	private String schedulerHost;
	private int schedulerTCPPort;
	private String taskDir;
	private ArrayList<TaskEntity> preparedTasks;
	private Boolean registered = false;
	private Boolean exit = false;
	private Socket schedulerSocket;
	private boolean admin;
	private String lookup;
	private String secretKeyPath;

	protected ClientModel() {

	}

	public String getSchedulerHost() {
		return schedulerHost;
	}

	public void setSchedulerHost(String schedulerHost) {
		this.schedulerHost = schedulerHost;
	}

	public int getSchedulerTCPPort() {
		return schedulerTCPPort;
	}

	public void setSchedulerTCPPort(int schedulerTCPPort) {
		this.schedulerTCPPort = schedulerTCPPort;
	}

	public String getTaskDir() {
		return taskDir;
	}

	public void setTaskDir(String taskDir) {
		this.taskDir = taskDir;
	}

	public ArrayList<TaskEntity> getPreparedTasks() {
		return preparedTasks;
	}

	public void setPreparedTasks(ArrayList<TaskEntity> preparedTasks) {
		this.preparedTasks = preparedTasks;
	}

	public Boolean getRegistered() {
		return registered;
	}

	public void setRegistered(Boolean registered) {
		this.registered = registered;
	}

	public Socket getSchedulerSocket() {
		return schedulerSocket;
	}

	public void setSchedulerSocket(final Socket schedulerSocket) {
		this.schedulerSocket = schedulerSocket;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public Boolean getExit() {
		return exit;
	}

	public void setExit(Boolean exit) {
		this.exit = exit;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public String getLookup() {
		return lookup;
	}

	public void setLookup(String lookup) {
		this.lookup = lookup;
	}

	public String getSecretKeyPath() {
		return secretKeyPath;
	}

	public void setSecretKeyPath(String secretKeyPath) {
		this.secretKeyPath = secretKeyPath;
	}
}
