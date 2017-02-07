package com.rest.data.response;

public class KnownDeviceResp {
	
	private String deviceId;
    private String type;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double posX;
    private Double posY;
    private Double elevation;
    private int projectId;
    
    public KnownDeviceResp(){
    	this.deviceId = null;
		this.type = null;
		this.latitude = 0.0;
		this.longitude = 0.0;
    }
    
	public KnownDeviceResp(String deviceId, String type, Double latitude, Double longitude) {
		this.deviceId = deviceId;
		this.type = type;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getAltitude() {
		return altitude;
	}

	public void setAltitude(Double altitude) {
		this.altitude = altitude;
	}

	public int getProjectId() {
		return projectId;
	}

	public void setProjectId(int projectId) {
		this.projectId = projectId;
	}

	public Double getPosX() {
		return posX;
	}

	public void setPosX(Double posX) {
		this.posX = posX;
	}

	public Double getPosY() {
		return posY;
	}

	public void setPosY(Double posY) {
		this.posY = posY;
	}

	public Double getElevation() {
		return elevation;
	}

	public void setElevation(Double elevation) {
		this.elevation = elevation;
	}
	
}
