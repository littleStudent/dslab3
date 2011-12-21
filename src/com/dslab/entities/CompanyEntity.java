package com.dslab.entities;

import java.util.ArrayList;

import com.dslab.client.ClientCallbackRemoteObjectInterface;

public class CompanyEntity {

	private String name;
	private String password;
	private boolean loggedIn;
	private boolean admin;
	private int credits;
	private ArrayList<TaskEntity> preparedTasks;
	private ClientCallbackRemoteObjectInterface callback;
	private int lowCount = 0;
	private int middleCount = 0;
	private int highCount = 0;
	private int payedTasksCount = 0;
	private Boolean active = false;
	
	public CompanyEntity(String name, String password){
		this.name = name;
		this.password = password;
		this.preparedTasks = new ArrayList<TaskEntity>();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isLoggedIn() {
		return loggedIn;
	}
	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}
	public boolean isAdmin() {
		return admin;
	}
	public void setAdmin(boolean admin) {
		this.admin = admin;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public int getCredits() {
		return credits;
	}
	public void setCredits(int credits) {
		this.credits = credits;
	}

	public ArrayList<TaskEntity> getPreparedTasks() {
		return preparedTasks;
	}

	public void setPreparedTasks(ArrayList<TaskEntity> preparedTasks) {
		this.preparedTasks = preparedTasks;
	}

	public ClientCallbackRemoteObjectInterface getCallback() {
		return callback;
	}

	public void setCallback(ClientCallbackRemoteObjectInterface callback) {
		this.callback = callback;
	}

	public int getLowCount() {
		return lowCount;
	}

	public void setLowCount(int lowCount) {
		this.lowCount = lowCount;
	}

	public int getMiddleCount() {
		return middleCount;
	}

	public void setMiddleCount(int middleCount) {
		this.middleCount = middleCount;
	}

	public int getHighCount() {
		return highCount;
	}

	public void setHighCount(int highCount) {
		this.highCount = highCount;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public int getPayedTasksCount() {
		return payedTasksCount;
	}

	public void setPayedTasksCount(int payedTasksCount) {
		this.payedTasksCount = payedTasksCount;
	}
}
