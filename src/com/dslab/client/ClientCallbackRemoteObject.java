package com.dslab.client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientCallbackRemoteObject extends UnicastRemoteObject implements ClientCallbackRemoteObjectInterface {

	protected ClientCallbackRemoteObject() throws RemoteException {
		super();
		// TODO Auto-generated constructor stub
	}

	public void printInfo(String info) {
		System.out.println(info);
	}
	
}
