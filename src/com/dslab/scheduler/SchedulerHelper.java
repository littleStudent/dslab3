/**
 * Helper class for various things.
 */

package com.dslab.scheduler;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.dslab.Types.GenericTaskEngineStatusEnum;
import com.dslab.Types.TypeEnum;
import com.dslab.entities.ClientEntity;
import com.dslab.entities.GenericTaskEngineEntity;

public class SchedulerHelper {

	/**
	 * checking the correct login data.
	 * 
	 * @param companies
	 * @param company
	 * @param password
	 * @return
	 */
	public Boolean checkLogin(Properties companies, String company, String password) {
		if (password.equals(companies.getProperty(company)))
			return true;
		else
			return false;
	}

	/**
	 * sets the status and port for the company
	 * 
	 * @param clients
	 * @param name
	 * @param status
	 * @param port
	 */
	public void setCompanyStatusforName(ArrayList<ClientEntity> clients, String name, Boolean status, int port) {
		for (ClientEntity c : clients) {
			if (c.getName().equals(name)) {
				c.setActive(status);
				c.setPort(port);
			}
		}
	}

	/**
	 * sending load requests to all engines updating the engine list (load, min, max)
	 * 
	 * @param model
	 */
	public static void refreshAllEngineStates(SchedulerModel model) {
		ExecutorService executor = Executors.newFixedThreadPool(20);
		for (GenericTaskEngineEntity e : model.getEngines()) {
			if (e.getState() != GenericTaskEngineStatusEnum.offline) {
				Runnable worker = new SchedulerTcpEngineLoadStatusWorker(e);
				executor.execute(worker);
			}
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * checks if the Engine was already connected once
	 * 
	 * @param port
	 * @param engines
	 * @return
	 */
	public static boolean checkAliveAlreadyRunningForPort(int port, ArrayList<GenericTaskEngineEntity> engines) {
		for (GenericTaskEngineEntity e : engines) {
			if (port == e.getTcpPort() && !e.isTimeoutCheckRunning()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * sets the received engine status
	 * 
	 * @param engine
	 * @param data
	 */
	public static void fillEngineWithStateRequestData(GenericTaskEngineEntity engine, String data) {
		if (data != null && data.split(" ").length == 3) {
			engine.setLoad(Integer.parseInt(data.split(" ")[0]));
			engine.setMinConsumption(Integer.parseInt(data.split(" ")[1]));
			engine.setMaxConsumption(Integer.parseInt(data.split(" ")[2]));
		}
	}

	/**
	 * returns the most efficient engine for a task
	 * 
	 * @param engines
	 * @param type
	 * @return
	 */
	public static GenericTaskEngineEntity getMostEfficientEngine(ArrayList<GenericTaskEngineEntity> engines,
			TypeEnum type) {
		GenericTaskEngineEntity bestEngine = engines.get(0);
		for (GenericTaskEngineEntity e : engines) {
			if (type == TypeEnum.LOW) {
				if (e.getLoad() + 33 < 100) {
					bestEngine = e;
				}
			} else if (type == TypeEnum.MIDDLE) {
				if (e.getLoad() + 66 < 100) {
					bestEngine = e;
				}
			} else {
				if (e.getLoad() + 99 < 100) {
					bestEngine = e;
				}
			}
		}
		for (GenericTaskEngineEntity e : engines) {

			if (type == TypeEnum.LOW) {
				if (e.getLoad() + 33 < 100) {
					if (e.getMaxConsumption() - e.getMinConsumption() < bestEngine.getMaxConsumption()
							- bestEngine.getMinConsumption())
						bestEngine = e;
				}
			} else if (type == TypeEnum.MIDDLE) {
				if (e.getLoad() + 66 < 100) {
					if (e.getMaxConsumption() - e.getMinConsumption() < bestEngine.getMaxConsumption()
							- bestEngine.getMinConsumption())
						bestEngine = e;
				}
			} else {
				if (e.getLoad() + 99 < 100) {
					if (e.getMaxConsumption() - e.getMinConsumption() < bestEngine.getMaxConsumption()
							- bestEngine.getMinConsumption())
						bestEngine = e;
				}
			}
		}
		return bestEngine;
	}

	/**
	 * returns all currently active engines
	 * 
	 * @param engines
	 * @return
	 */
	public static ArrayList<GenericTaskEngineEntity> getActiveEngines(ArrayList<GenericTaskEngineEntity> engines) {
		ArrayList<GenericTaskEngineEntity> activeEngines = new ArrayList<GenericTaskEngineEntity>();
		for (GenericTaskEngineEntity e : engines) {
			if (e.getState() == GenericTaskEngineStatusEnum.online)
				activeEngines.add(e);
		}
		return activeEngines;
	}

	/**
	 * returns all currently active engines with no load
	 * 
	 * @param engines
	 * @return
	 */
	public static ArrayList<GenericTaskEngineEntity> getActiveNotLoadedEngines(
			ArrayList<GenericTaskEngineEntity> engines) {
		ArrayList<GenericTaskEngineEntity> activeEngines = new ArrayList<GenericTaskEngineEntity>();
		for (GenericTaskEngineEntity e : engines) {
			if (e.getState() == GenericTaskEngineStatusEnum.online && e.getLoad() == 0)
				activeEngines.add(e);
		}
		return activeEngines;
	}

	/**
	 * returns all currently suspended engines
	 * 
	 * @param engines
	 * @return
	 */
	public static ArrayList<GenericTaskEngineEntity> getSuspendedEngines(ArrayList<GenericTaskEngineEntity> engines) {
		ArrayList<GenericTaskEngineEntity> suspendedEngines = new ArrayList<GenericTaskEngineEntity>();
		for (GenericTaskEngineEntity e : engines) {
			if (e.getState() == GenericTaskEngineStatusEnum.suspended)
				suspendedEngines.add(e);
		}
		return suspendedEngines;
	}

	/**
	 * returns all currently offline engines
	 * 
	 * @param engines
	 * @return
	 */
	public static ArrayList<GenericTaskEngineEntity> getInactiveEngines(ArrayList<GenericTaskEngineEntity> engines) {
		ArrayList<GenericTaskEngineEntity> inactiveEngines = new ArrayList<GenericTaskEngineEntity>();
		for (GenericTaskEngineEntity e : engines) {
			if (e.getState() == GenericTaskEngineStatusEnum.offline)
				inactiveEngines.add(e);
		}
		return inactiveEngines;
	}

	/**
	 * checks if there exist active engines with load < 66
	 * 
	 * @param activeEngines
	 * @return
	 */
	public static boolean percentLoadCheckRule(ArrayList<GenericTaskEngineEntity> activeEngines) {
		for (GenericTaskEngineEntity e : activeEngines) {
			if (e.getLoad() < 66)
				return false;
		}
		return true;
	}

	/**
	 * returns the best task for reactivation
	 * 
	 * @param suspendedEngines
	 * @return
	 */
	public static GenericTaskEngineEntity getTaskToActivate(ArrayList<GenericTaskEngineEntity> suspendedEngines) {
		GenericTaskEngineEntity response = suspendedEngines.get(0);
		for (GenericTaskEngineEntity e : suspendedEngines) {
			if (response.getMinConsumption() > e.getMinConsumption())
				response = e;
		}
		return response;
	}

	/**
	 * checks if there exist at least two 0% load engines
	 * 
	 * @param activeEngines
	 * @return
	 */
	public static boolean zeroLoadCheckRule(ArrayList<GenericTaskEngineEntity> activeEngines) {
		int i = 0;
		for (GenericTaskEngineEntity e : activeEngines) {
			if (e.getLoad() == 0)
				i++;
		}
		if (i > 1)
			return false;
		else
			return true;
	}

	/**
	 * returns the best task for suspending
	 * 
	 * @param activeEngines
	 * @return
	 */
	public static GenericTaskEngineEntity getTaskToSuspend(ArrayList<GenericTaskEngineEntity> activeEngines) {
		GenericTaskEngineEntity response = activeEngines.get(0);
		for (GenericTaskEngineEntity e : activeEngines) {
			if (response.getMinConsumption() < e.getMinConsumption()) {
				response = e;
			} else if (response.getMinConsumption() > e.getMinConsumption()) {
			} else if ((response.getMaxConsumption() - response.getMinConsumption()) * (response.getLoad() + 1)
					+ response.getMinConsumption() < (e.getMaxConsumption() - e.getMinConsumption())
					* (e.getLoad() + 1) + e.getMinConsumption()) {
				response = e;
			}
		}
		return response;
	}

	/**
	 * returns the integer load for a string
	 * 
	 * @param typ
	 * @return
	 */
	public static int getLoadForString(String typ) {
		if (typ.equals("LOW"))
			return 33;
		if (typ.equals("MIDDLE"))
			return 66;
		if (typ.equals("HIGH"))
			return 99;
		return 0;
	}

}
