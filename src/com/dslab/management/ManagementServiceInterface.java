package com.dslab.management;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.dslab.remoteobjects.RemoteObjectInterface;


public interface ManagementServiceInterface extends Remote {
	RemoteObjectInterface authentication(String login, String password) throws RemoteException;
}
