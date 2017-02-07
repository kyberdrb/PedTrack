package com.rest.data.response;

public class ProjectResp {
	
	private int projectId;
    private String name;
    private Double defaultPsPosX;
    private Double defaultPsPosY;
    private Double elevation;
	
    public int getProjectId() {
		return projectId;
	}
	
	public void setProjectId(int projectId) {
		this.projectId = projectId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Double getDefaultPsPosX() {
		return defaultPsPosX;
	}
	
	public void setDefaultPsPosX(Double defaultPsPosX) {
		this.defaultPsPosX = defaultPsPosX;
	}
	
	public Double getDefaultPsPosY() {
		return defaultPsPosY;
	}
	
	public void setDefaultPsPosY(Double defaultPsPosY) {
		this.defaultPsPosY = defaultPsPosY;
	}

	public Double getElevation() {
		return elevation;
	}

	public void setElevation(Double elevation) {
		this.elevation = elevation;
	}
    
}
