package com.dslab.entities;

import java.util.HashMap;

import com.dslab.Types.TaskStatusEnum;
import com.dslab.Types.TypeEnum;

public class TaskEntity {

	private int id;
	private String taskName;
	private String fileName;
	private TypeEnum type;
	private TaskStatusEnum status;
	private GenericTaskEngineEntity assignedEngine;
	public static int lastTaskId = 0;
	private String call;
	private CompanyEntity ownerCompany;
	private int costs;
	private boolean payed = false;
	private int distributedAmount;
	private HashMap<Integer, String> outputs;

	public TaskEntity(final String taskName, final String fileName, final TypeEnum type, final TaskStatusEnum status) {
		this.taskName = taskName;
		this.fileName = fileName;
		this.type = type;
		this.status = status;
		lastTaskId++;
		this.id = lastTaskId;
		this.assignedEngine = new GenericTaskEngineEntity();
		setOutputs(new HashMap());
	}

	public TaskEntity(final String fileName, final TypeEnum type) {
		this.fileName = fileName;
		this.type = type;
		lastTaskId++;
		this.id = lastTaskId;
		this.assignedEngine = new GenericTaskEngineEntity();
		setOutputs(new HashMap());
	}

	public TaskEntity(int taskId, final String taskName, final String fileName, final TypeEnum type,
			final TaskStatusEnum status) {
		this.taskName = taskName;
		this.fileName = fileName;
		this.type = type;
		this.status = status;
		this.id = taskId;
		this.assignedEngine = new GenericTaskEngineEntity();
		setOutputs(new HashMap());
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public TypeEnum getType() {
		return type;
	}

	public void setType(TypeEnum type) {
		this.type = type;
	}

	public TaskStatusEnum getStatus() {
		return status;
	}

	public void setStatus(TaskStatusEnum status) {
		this.status = status;
	}

	public GenericTaskEngineEntity getAssignedEngine() {
		return assignedEngine;
	}

	public void setAssignedEngine(GenericTaskEngineEntity assignedEngine) {
		this.assignedEngine = assignedEngine;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getCall() {
		return call;
	}

	public void setCall(String call) {
		this.call = call;
	}

	public CompanyEntity getOwnerCompany() {
		return ownerCompany;
	}

	public void setOwnerCompany(CompanyEntity ownerCompany) {
		this.ownerCompany = ownerCompany;
	}

	public int getCosts() {
		return costs;
	}

	public void setCosts(int costs) {
		this.costs = costs;
	}

	public boolean isPayed() {
		return payed;
	}

	public void setPayed(boolean payed) {
		this.payed = payed;
	}

	public int getDistributedAmount() {
		return distributedAmount;
	}

	public void setDistributedAmount(int distributedAmount) {
		this.distributedAmount = distributedAmount;
	}

	public HashMap getOutputs() {
		return outputs;
	}

	public void setOutputs(HashMap outputs) {
		this.outputs = outputs;
	}

}
