package com.dslab.management;

import java.util.ArrayList;
import java.util.Properties;

import com.dslab.entities.CompanyEntity;
import com.dslab.entities.TaskEntity;

public class ManagementServiceHelper {

	/**
	 * checking the correct login data. 
	 * @param companies
	 * @param company
	 * @param password
	 * @return
	 */
	public static Boolean checkLogin(Properties companies, String company, String password) {
		if(password.equals(companies.getProperty(company)))
			return true;
		else
			return false;
	}
	
	public static CompanyEntity getCompanyForName(String name, ManagementServiceModel model) {
		CompanyEntity e = null;
		for(CompanyEntity c : model.getCompanies()) {
			if(c.getName().equals(name)) {
				e = c;
			}
		}
		return e;
	}
	
	public static TaskEntity getTaskForId(ArrayList<TaskEntity> tasks, int id) {
		TaskEntity e = null;
		for(TaskEntity c : tasks) {
			if(c.getId() == id) {
				e = c;
			}
		}
		return e;
	}
	
	public static double getDiscountForTaskCount(ManagementServiceModel model, CompanyEntity activeCompany) {
		for(int x : model.getPriceSteps()) {
			if(activeCompany.getLowCount() + activeCompany.getMiddleCount() + activeCompany.getHighCount() <= x) {
				return model.getPriceStepsMap().get(x) / 100;
			}
		}
		return model.getPriceStepsMap().get(0) / 100;
	}
}
