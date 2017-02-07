package com.rest.data.request;

import com.rest.data.request.position_entry.BeaconEntry;
import com.rest.data.request.position_entry.GpsEntry;
import com.rest.data.request.position_entry.WifiEntry;

import java.util.HashSet;
import java.util.Set;

public class MobileDeviceEntry {
	
	private Double posX;
    private Double posY;
    private long timeOfMeasurement;
    private Double elevation;
    private Set<BeaconEntry> beaconDatas;
    private Set<WifiEntry> wifiDatas;
    private Set<GpsEntry> gpsDatas;
    private String mobileDeviceId;
    private int projectId;
	
    public MobileDeviceEntry() {
		this.beaconDatas = new HashSet<BeaconEntry>();
		this.wifiDatas = new HashSet<WifiEntry>();
		this.gpsDatas = new HashSet<GpsEntry>();
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
	
    public long getTimeOfMeasurement() {
		return timeOfMeasurement;
	}
	
    public void setTimeOfMeasurement(long timeOfMeasurement) {
		this.timeOfMeasurement = timeOfMeasurement;
	}
	
    public Double getElevation() {
		return elevation;
	}
	
    public void setElevation(Double elevation) {
		this.elevation = elevation;
	}
	
    public Set<BeaconEntry> getBeaconDatas() {
		return beaconDatas;
	}
	
    public void setBeaconDatas(Set<BeaconEntry> beaconDatas) {
		this.beaconDatas = beaconDatas;
	}
	
    public Set<WifiEntry> getWifiDatas() {
		return wifiDatas;
	}
	
    public void setWifiDatas(Set<WifiEntry> wifiDatas) {
		this.wifiDatas = wifiDatas;
	}

	public Set<GpsEntry> getGpsDatas() {
		return gpsDatas;
	}

	public void setGpsDatas(Set<GpsEntry> gpsDatas) {
		this.gpsDatas = gpsDatas;
	}

	public String getMobileDeviceId() {
		return mobileDeviceId;
	}

	public void setMobileDeviceId(String mobileDeviceId) {
		this.mobileDeviceId = mobileDeviceId;
	}

	public int getProjectId() {
		return projectId;
	}

	public void setProjectId(int projectId) {
		this.projectId = projectId;
	}
    
    
    
}
