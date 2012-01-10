/**
 * Listens and reacts to the tcp connection Client - Scheduler
 */

package com.dslab.management;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

import com.dslab.Cipher.KeyWorker;
import com.dslab.Types.TaskStatusEnum;
import com.dslab.entities.GenericTaskEngineEntity;
import com.dslab.entities.TaskEntity;

public class ManagementTcpSchedulerListenerWorker implements Runnable {
	private ManagementServiceModel model;
	private Socket schedulerSocket;

	public ManagementTcpSchedulerListenerWorker(ManagementServiceModel model) {
		this.model = model;
	}

	private TaskEntity getTaskById(int input) {

		for (TaskEntity t : model.getTasks()) {
			if (t.getId() == input)
				return t;
		}
		return null;
	}

	@Override
	public void run() {
		try {
			while (!model.isExit()) {
				String response = new String(receiveEncrypted());
				// System.out.println(response);
				if (response != null) {
					if (response.equals("finished transaction")) {
						System.out.println("finished task");
						model.getCurrentRequestedTask().setStatus(TaskStatusEnum.finished);
					} else if (response.split(" ")[0].equals("!requestEngine")) {
						model.getCurrentRequestedTask().setAssignedEngine(
								new GenericTaskEngineEntity(response.split(" ")[1], Integer.parseInt(response
										.split(" ")[2])));
						ManagementServiceHelper
								.getCompanyForName(response.split("#")[1], model)
								.getCallback()
								.printInfo(
										"Execution for task " + model.getCurrentRequestedTask().getId() + " started.");

						schedulerSocket = new Socket(model.getCurrentRequestedTask().getAssignedEngine().getIp(), model
								.getCurrentRequestedTask().getAssignedEngine().getTcpPort());
						Runnable worker = new ManagementTcpEngineWorker(schedulerSocket,
								model.getCurrentRequestedTask(), model);
						model.getExecutorTcp().execute(worker);
					} else {
						// System.out.println("Server: " + response);
						model.getCurrentRequestedTask().setStatus(TaskStatusEnum.prepared);
						ManagementServiceHelper.getCompanyForName(response.split("#")[1], model).getCallback()
								.printInfo(response.split("#")[0]);

					}
				} else {
					schedulerSocket.close();
					System.out.println("logged out from server.");
					break;
				}
			}
		} catch (IOException e) {
			// Thread.
			try {
				schedulerSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			System.out.println("logged out from server.");
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private byte[] receiveEncrypted() throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IOException {
		byte[] inputBytes2 = new byte[684];
		InputStream is = model.getSchedulerSocket().getInputStream();
		int result = is.read(inputBytes2, 0, inputBytes2.length);
		byte[] receivedBytes = new byte[result];
		System.arraycopy(inputBytes2, 0, receivedBytes, 0, result);
		receivedBytes = Base64.decode(receivedBytes);
		receivedBytes = KeyWorker.getCipherForAlgorithm("AES/CTR/NoPadding", false,
				new SecretKeySpec(model.getCipherStuff().getSecretKey(), "AES"),
				model.getCipherStuff().getIvParameter()).doFinal(receivedBytes);
		receivedBytes = Base64.decode(receivedBytes);
		return receivedBytes;
	}

}