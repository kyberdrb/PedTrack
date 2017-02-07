package com.rest.data.footprint_entry;

public class KdfpEntry {
	private String knownDeviceId;
    private Double wifiSignalStrength;
    private Double btSignalStrength;
    
    public KdfpEntry(){
    	
    }

	public String getKnownDeviceId() {
		return knownDeviceId;
	}

	public void setKnownDeviceId(String knownDeviceId) {
		this.knownDeviceId = knownDeviceId;
	}

	public Double getWifiSignalStrength() {
		return wifiSignalStrength;
	}

	public void setWifiSignalStrength(Double wifiSignalStrength) {
		this.wifiSignalStrength = wifiSignalStrength;
	}

	public Double getBtSignalStrength() {
		return btSignalStrength;
	}

	public void setBtSignalStrength(Double btSignalStrength) {
		this.btSignalStrength = btSignalStrength;
	}
    
    
}
