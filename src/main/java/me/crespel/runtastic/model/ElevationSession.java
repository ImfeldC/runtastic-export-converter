package me.crespel.runtastic.model;

import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(of = {"fileName", "elevationData"})
public class ElevationSession {
 
    private String fileName;

	private List<ElevationData> elevationData;

}