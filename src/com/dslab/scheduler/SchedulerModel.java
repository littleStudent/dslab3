/**
 * Model that holds the needed info for the scheduler.
 */

package com.dslab.scheduler;

import java.util.ArrayList;

import com.dslab.entities.ClientEntity;
import com.dslab.entities.GenericTaskEngineEntity;

public class SchedulerModel {
	
	private java.util.Properties companies;
	private ArrayList<ClientEntity> clients;
	private ArrayList<GenericTaskEngineEntity> engines;
	private boolean exit;
	
	protected SchedulerModel() {
		this.clients = new ArrayList<ClientEntity>();
		this.engines = new ArrayList<GenericTaskEngineEntity>();
	}
	

	public java.util.Properties getCompanies() {
		return companies;
	}

	public void setCompanies(final java.util.Properties companies) {
		this.companies = companies;
	}

	public ArrayList<ClientEntity> getClients() {
		return clients;
	}

	public void setClients(ArrayList<ClientEntity> clients) {
		this.clients = clients;
	}

	public ArrayList<GenericTaskEngineEntity> getEngines() {
		return engines;
	}

	public void setEngines(ArrayList<GenericTaskEngineEntity> engines) {
		this.engines = engines;
	}

	
	/**
	 * returns the engine for a specific ip
	 * @param ip
	 * @return
	 */
	public GenericTaskEngineEntity getEngineForIp(String ip) {
		for (GenericTaskEngineEntity e : engines) {
			if(e.getIp().equals(ip))
				return e;
		}
		GenericTaskEngineEntity g = new GenericTaskEngineEntity(ip);
		engines.add(g);
		return g;
	}
	
	/**
	 * returns the engine for a specific ip + port
	 * @param port
	 * @param ip
	 * @return
	 */
	public GenericTaskEngineEntity getEngineForPort(int port, String ip) {
		for (GenericTaskEngineEntity e : engines) {
			if(e.getTcpPort() == port && e.getIp().equals(ip))
				return e;
		}
		GenericTaskEngineEntity g = new GenericTaskEngineEntity(port);
		engines.add(g);
		return g;
	}
	
	/**
	 * returns the client for a specific port
	 * @param port
	 * @return
	 */
	public ClientEntity getClientForPort(int port) {
		for(ClientEntity e : clients) {
			if(e.getPort() == port)
				return e;
		}
		return null;
	}
	
	/**
	 * returns the client for a company
	 * @param company
	 * @return
	 */
	public ClientEntity getClientForCompany(String company) {
		for(ClientEntity e : clients) {
			if(e.getName().equals(company))
				return e;
		}
		return null;
	}


	public boolean isExit() {
		return exit;
	}


	public void setExit(boolean exit) {
		this.exit = exit;
	}
}
