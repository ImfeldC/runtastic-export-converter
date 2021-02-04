package me.crespel.runtastic.model;

import java.util.List;

import lombok.Data;

@Data
public class GpsSession {
    
	private String fileName;

    private List<GpsData> gpsData;

}