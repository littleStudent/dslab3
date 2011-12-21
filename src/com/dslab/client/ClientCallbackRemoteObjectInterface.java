package com.dslab.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallbackRemoteObjectInterface extends Remote {
	public void printInfo(String info) throws RemoteException;
}
