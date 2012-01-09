/**
 * Handles the udp connection to the engines
 */

package com.dslab.scheduler;

import java.net.DatagramPacket;

import com.dslab.Types.GenericTaskEngineStatusEnum;
import com.dslab.entities.GenericTaskEngineEntity;

public class SchedulerUdpEngineWorker implements Runnable {

	private SchedulerModel model;
	private DatagramPacket receivePacket;
	private static int timeout;

	public SchedulerUdpEngineWorker(final SchedulerModel model, final DatagramPacket receivePacket, final int timeout) {
		this.model = model;
		this.receivePacket = receivePacket;
		SchedulerUdpEngineWorker.timeout = timeout;
	}

	@Override
	public void run() {
		Boolean checkEngine = model.checkIfEngineAlreadyExists(Integer.parseInt(new String(receivePacket.getData())
				.split(" ")[1].trim()), receivePacket.getAddress().getHostAddress());
		GenericTaskEngineEntity engine = model.getEngineForPort(Integer.parseInt(new String(receivePacket.getData())
				.split(" ")[1].trim()), receivePacket.getAddress().getHostAddress());
		if (engine.getState() != GenericTaskEngineStatusEnum.suspended && !checkEngine) {
			if (engine.getIp().equals("")) {
				Runnable task = new CheckAlive(engine, model);
				Thread worker = new Thread(task);
				worker.start();
				engine.setTcpPort(Integer.parseInt(new String(receivePacket.getData()).split(" ")[1].trim()));
				engine.setUdpPort(receivePacket.getPort());
				engine.setIp(receivePacket.getAddress().getHostAddress());
				engine.setTimeoutCheck(true);
			} else {
				engine.setTimeoutCheck(true);
			}
		}
	}

	/**
	 * Running once for each engine checks if the engine is offline
	 * 
	 * @author thomassattlecker
	 * 
	 */
	static class CheckAlive implements Runnable {
		private GenericTaskEngineEntity e;
		private SchedulerModel model;

		protected CheckAlive(GenericTaskEngineEntity e, SchedulerModel model) {
			this.e = e;
			this.model = model;
		}

		@Override
		public void run() {
			try {
				while (!model.isExit()) {
					Thread.sleep(timeout);
					if (e.isTimeoutCheck() == false && e.getState() != GenericTaskEngineStatusEnum.suspended) {
						e.setState(GenericTaskEngineStatusEnum.offline);
					} else if (e.getState() != GenericTaskEngineStatusEnum.suspended) {
						e.setTimeoutCheck(false);
						e.setState(GenericTaskEngineStatusEnum.online);
					}
				}
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}
	}

}
