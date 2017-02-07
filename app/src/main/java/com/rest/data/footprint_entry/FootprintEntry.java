package com.rest.data.footprint_entry;

import java.util.LinkedList;
import java.util.List;

public class FootprintEntry{

    private int projectId;
    private Double latitude;
    private Double longitude;
    private Double altitude;
    private List<KdfpEntry> kdfpEntries;
    
    public FootprintEntry(){
    	this.kdfpEntries = new LinkedList<KdfpEntry>();
    }

	public int getProjectId() {
		return projectId;
	}

	public void setProjectId(int projectId) {
		this.projectId = projectId;
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

	public List<KdfpEntry> getKdfpEntries() {
		return kdfpEntries;
	}

	public void setKdfpEntries(List<KdfpEntry> kdfpEntries) {
		this.kdfpEntries = kdfpEntries;
	}
	
	
    
    
}
