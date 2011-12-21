package com.dslab.remoteobjects;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.dslab.Types.TypeEnum;
import com.dslab.client.ClientCallbackRemoteObjectInterface;
import com.dslab.entities.TaskEntity;

public interface RemoteObjectInterface extends Remote {
	public int logout() throws RemoteException;

	public boolean isAdmin() throws RemoteException;

	public String getPricingCurve() throws RemoteException;

	public String setPricingCurve(int taskCount, double percent) throws RemoteException;

	public int prepareTask(String name, TypeEnum type, byte[] bytes) throws RemoteException;

	public TaskEntity getPreparedTaskForId(int id) throws RemoteException;

	public String executeTaskForId(int id, ClientCallbackRemoteObjectInterface callback, String call)
			throws RemoteException;

	public String getTaskInfoForId(int id) throws RemoteException;

	public String getOutputForId(int id) throws RemoteException;

	public String buyCredits(int amount) throws RemoteException;

	public String getCredits() throws RemoteException;

	public String executeDistributedForId(int id, int amount, ClientCallbackRemoteObjectInterface callback, String call)
			throws RemoteException;
}
