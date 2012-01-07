package com.dslab.remoteobjects;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

import com.dslab.Types.TypeEnum;
import com.dslab.client.ClientCallbackRemoteObjectInterface;
import com.dslab.entities.CompanyEntity;
import com.dslab.entities.Output;
import com.dslab.entities.TaskEntity;
import com.dslab.management.ManagementServiceModel;

public class AdminRemoteObject extends UnicastRemoteObject implements RemoteObjectInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1051662394022896272L;
	private ManagementServiceModel model;
	private CompanyEntity activeAdmin;

	public AdminRemoteObject(ManagementServiceModel model, CompanyEntity activeAdmin) throws RemoteException {
		super();
		this.setModel(model);
		this.activeAdmin = activeAdmin;
		// TODO Auto-generated constructor stub
	}

	public synchronized String getPricingCurve() {
		String returnValue = "";
		Object[] key = model.getPriceStepsMap().keySet().toArray();
		Arrays.sort(key);

		for (int i = 0; i < key.length; i++) {
			returnValue = returnValue + key[i] + " | " + model.getPriceStepsMap().get(key[i]) + " %\n";
		}
		return returnValue;
	}

	public void setPriceStepForCurve() {

	}

	@Override
	public int logout() throws RemoteException {
		activeAdmin.setActive(false);
		return 0;
	}

	@Override
	public boolean isAdmin() throws RemoteException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public int prepareTask(String name, TypeEnum type, byte[] bytes) throws RemoteException {
		throw new RemoteException();
	}

	@Override
	public TaskEntity getPreparedTaskForId(int id) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String executeTaskForId(int id, ClientCallbackRemoteObjectInterface callback, String call)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTaskInfoForId(int id) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Output getOutputForId(int id) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String buyCredits(int amount) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCredits() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public ManagementServiceModel getModel() {
		return model;
	}

	public synchronized void setModel(ManagementServiceModel model) {
		this.model = model;
	}

	@Override
	public synchronized String setPricingCurve(int taskCount, double percent) throws RemoteException {
		if (taskCount < 0) {
			return "Error: Invalid task count!";
		} else if (percent < 0 || percent > 100) {
			return "Error: Invalid percentage!";
		} else {
			boolean t = false;
			for (int x : model.getPriceSteps()) {
				if (x == taskCount) {
					t = true;
				}
			}
			if (!t) {
				model.getPriceSteps().add(taskCount);
			}
			model.getPriceStepsMap().put(taskCount, percent);
			return "Successfully inserted price step.";
		}
	}

	@Override
	public String executeDistributedForId(int id, int amount, ClientCallbackRemoteObjectInterface callback, String call)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

}
