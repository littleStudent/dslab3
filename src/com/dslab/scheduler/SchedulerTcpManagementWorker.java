/**
 * Handles the tcp connection to the Clients.
 */

package com.dslab.scheduler;

import java.io.DataOutputStream;
import java.io.IOException;
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
import com.dslab.Types.TypeEnum;
import com.dslab.entities.GenericTaskEngineEntity;

public class SchedulerTcpManagementWorker implements Runnable {

	private static SchedulerModel model;
	private Socket tcpSocket;

	public SchedulerTcpManagementWorker(SchedulerModel model, Socket tcpSocket) {
		this.model = model;
		this.tcpSocket = tcpSocket;
	}

	@Override
	public void run() {
		try {
			DataOutputStream outToClient = new DataOutputStream(tcpSocket.getOutputStream());
			while (true) {
				String input = new String(receiveEncrypted());
				if (input != null) {
					if (input.split(" ")[0].equals("!requestEngine")) {
						if (SchedulerHelper.getActiveEngines(model.getEngines()).size() > 0) {
							GenericTaskEngineEntity bestEngine = SchedulerHelper.getMostEfficientEngine(
									SchedulerHelper.getActiveEngines(model.getEngines()),
									TypeEnum.valueOf(input.split(" ")[1]));
							if (TypeEnum.valueOf(input.split(" ")[1]) == TypeEnum.LOW) {
								if (bestEngine.getLoad() + 33 > 99) {
									sendEncrypted(("Error: No engine available for execution. Please try again later.#"
											+ input.split(" ")[2] + "#").getBytes());
								} else {
									bestEngine.setLoad(bestEngine.getLoad() + 33);
									sendEncrypted(("!requestEngine " + bestEngine.getIp() + " "
											+ bestEngine.getTcpPort() + " #" + input.split(" ")[2] + "#").getBytes());
								}
							} else if (TypeEnum.valueOf(input.split(" ")[1]) == TypeEnum.MIDDLE) {
								if (bestEngine.getLoad() + 66 > 99) {
									sendEncrypted(("Error: No engine available for execution. Please try again later.#"
											+ input.split(" ")[2] + "#").getBytes());
								} else {
									bestEngine.setLoad(bestEngine.getLoad() + 66);
									sendEncrypted(("!requestEngine " + bestEngine.getIp() + " "
											+ bestEngine.getTcpPort() + " #" + input.split(" ")[2] + "#").getBytes());
								}
							} else if (TypeEnum.valueOf(input.split(" ")[1]) == TypeEnum.HIGH) {
								if (bestEngine.getLoad() + 99 > 99) {
									sendEncrypted(("Error: No engine available for execution. Please try again later.#"
											+ input.split(" ")[2] + "#").getBytes());
								} else {
									bestEngine.setLoad(bestEngine.getLoad() + 99);
									sendEncrypted(("!requestEngine " + bestEngine.getIp() + " "
											+ bestEngine.getTcpPort() + " #" + input.split(" ")[2] + "#").getBytes());
								}
							}

						} else {
							sendEncrypted(("Error: No engine available for execution. Please try again later.#"
									+ input.split(" ")[2] + "#\n").getBytes());
						}
					} else if (input.split(" ")[0].equals("!requestEngines")) {
						if (SchedulerHelper.getActiveEngines(model.getEngines()).size() > 0) {
							GenericTaskEngineEntity bestEngine = SchedulerHelper.getMostEfficientEngineDistributed(
									SchedulerHelper.getActiveEngines(model.getEngines()),
									TypeEnum.valueOf(input.split(" ")[1]));
							if (TypeEnum.valueOf(input.split(" ")[1]) == TypeEnum.LOW) {
								if (bestEngine.getLoad() + 33 > 99) {
									sendEncrypted(("Error: No engine available for execution. Please try again later.#"
											+ input.split(" ")[2] + "#").getBytes());
								} else {
									bestEngine.setLoad(bestEngine.getLoad() + 33);
									sendEncrypted(("!requestEngines " + bestEngine.getIp() + " "
											+ bestEngine.getTcpPort() + " #" + input.split(" ")[2] + "#").getBytes());
								}
							} else if (TypeEnum.valueOf(input.split(" ")[1]) == TypeEnum.MIDDLE) {
								if (bestEngine.getLoad() + 66 > 99) {
									sendEncrypted(("Error: No engine available for execution. Please try again later.#"
											+ input.split(" ")[2] + "#").getBytes());
								} else {
									bestEngine.setLoad(bestEngine.getLoad() + 66);
									sendEncrypted(("!requestEngines " + bestEngine.getIp() + " "
											+ bestEngine.getTcpPort() + " #" + input.split(" ")[2] + "#").getBytes());
								}
							} else if (TypeEnum.valueOf(input.split(" ")[1]) == TypeEnum.HIGH) {
								if (bestEngine.getLoad() + 99 > 99) {
									sendEncrypted(("Error: No engine available for execution. Please try again later.#"
											+ input.split(" ")[2] + "#").getBytes());
								} else {
									bestEngine.setLoad(bestEngine.getLoad() + 99);
									sendEncrypted(("!requestEngines " + bestEngine.getIp() + " "
											+ bestEngine.getTcpPort() + " #" + input.split(" ")[2] + "#").getBytes());
								}
							}

						} else {
							sendEncrypted(("Error: No engine available for execution. Please try again later.#"
									+ input.split(" ")[2] + "#").getBytes());
						}
					}
				} else {
					System.out.println("management service shut down.");
					break;
				}
			}
		} catch (IOException e) {

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
		byte[] inputBytes2 = new byte[300];
		int result = tcpSocket.getInputStream().read(inputBytes2, 0, inputBytes2.length);
		byte[] receivedBytes = new byte[result];
		System.arraycopy(inputBytes2, 0, receivedBytes, 0, result);
		receivedBytes = Base64.decode(receivedBytes);
		receivedBytes = KeyWorker.getCipherForAlgorithm("AES/CTR/NoPadding", false,
				new SecretKeySpec(model.getCipherStuff().getSecretKey(), "AES"),
				model.getCipherStuff().getIvParameter()).doFinal(receivedBytes);
		receivedBytes = Base64.decode(receivedBytes);
		return receivedBytes;
	}

	private void sendEncrypted(byte[] message) throws IllegalBlockSizeException, BadPaddingException,
			InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IOException {
		byte[] outBytes = new byte[684];
		outBytes = KeyWorker.getCipherForAlgorithm("AES/CTR/NoPadding", true,
				new SecretKeySpec(model.getCipherStuff().getSecretKey(), "AES"),
				model.getCipherStuff().getIvParameter()).doFinal(Base64.encode(message));
		outBytes = Base64.encode(outBytes);
		tcpSocket.getOutputStream().write(outBytes);
	}

}
