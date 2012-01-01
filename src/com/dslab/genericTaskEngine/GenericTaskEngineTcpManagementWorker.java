/**
 * Handles the tcp connection to the Client. Receives and executes the task.
 * All the output from the task is sent back to the client.
 */

package com.dslab.genericTaskEngine;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import com.dslab.entities.GenericTaskEngineEntity;
import com.dslab.scheduler.SchedulerHelper;

public class GenericTaskEngineTcpManagementWorker implements Runnable {

	private Socket clientSocket;
	private GenericTaskEngineEntity engine;
	private String input;
	private String taskDir;
	private boolean exit;

	protected GenericTaskEngineTcpManagementWorker(Socket clientSocket, GenericTaskEngineEntity engine, String input,
			String taskDir, boolean exit) {
		this.clientSocket = clientSocket;
		this.engine = engine;
		this.input = input;
		this.taskDir = taskDir;
		this.exit = exit;
	}

	@Override
	public void run() {
		try {
			DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());
			byte[] mybytearray = new byte[160000];
			InputStream is = clientSocket.getInputStream();

			if (!new File(
					taskDir + "/" + clientSocket.getInetAddress().getHostAddress() + "." + clientSocket.getPort(),
					input.split(" ")[1]).exists()) {
				boolean success = (new File(taskDir + "/" + clientSocket.getInetAddress().getHostAddress() + "."
						+ clientSocket.getPort())).mkdirs();
				if (!success) {
					// System.out.println("File);
				}
				File file = new File(taskDir + "/" + clientSocket.getInetAddress().getHostAddress() + "."
						+ clientSocket.getPort() + "/" + input.split(" ")[1]);
				FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				int bytesRead = is.read(mybytearray, 0, mybytearray.length);
				System.out.println("Connecting...");
				bos.write(mybytearray, 0, bytesRead);
				bos.flush();
				bos.close();
			}
			System.out.println(input);
			outToClient.writeBytes(input + "\n");

			String line;
			if (engine.getLoad() + SchedulerHelper.getLoadForString(input.split(" ")[2]) < 100) {
				engine.setLoad(engine.getLoad() + SchedulerHelper.getLoadForString(input.split(" ")[2]));
				String command = input.split("\"")[1].replaceAll(input.split("\"")[1].split(" ")[2],
						taskDir + "/" + clientSocket.getInetAddress().getHostAddress() + "." + clientSocket.getPort()
								+ "/" + input.split("\"")[1].split(" ")[2]);
				System.out.println(command);
				Process p = Runtime.getRuntime().exec(command);

				BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(p.getInputStream()));
				try {
					while ((line = inputBuffer.readLine()) != null && !exit) {
						System.out.println(line);
						outToClient.writeBytes(line + "\n");
					}
				} catch (IOException e) {
					outToClient.writeBytes("something went wrong. Correct call script? \n");
				}

				outToClient.writeBytes("finished transaction" + "\n");
				inputBuffer.close();
				System.out.println("finished");
				engine.setLoad(engine.getLoad() - SchedulerHelper.getLoadForString(input.split(" ")[2]));
			} else {
				outToClient.writeBytes("Not enough capacity. Try again later." + "\n");
			}
		} catch (IOException e) {
			engine.setLoad(engine.getLoad() - SchedulerHelper.getLoadForString(input.split(" ")[2]));
		}

	}
}
