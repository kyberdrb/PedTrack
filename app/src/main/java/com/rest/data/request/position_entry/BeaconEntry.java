package com.rest.data.request.position_entry;

public class BeaconEntry {
    private Double signalStrength;
    private String knownDeviceId;
	
    public Double getSignalStrength() {
		return signalStrength;
	}
	
    public void setSignalStrength(Double signalStrength) {
		this.signalStrength = signalStrength;
	}

	public String getKnownDeviceId() {
		return knownDeviceId;
	}

	public void setKnownDeviceId(String knownDeviceId) {
		this.knownDeviceId = knownDeviceId;
	}
    
    

}
