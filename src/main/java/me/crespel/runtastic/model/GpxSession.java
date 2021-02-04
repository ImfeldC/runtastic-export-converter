package me.crespel.runtastic.model;

import com.topografix.gpx._1._1.GpxType;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(of = {"fileName", "gpx"})
public class GpxSession {
    
	private String fileName;

    private GpxType gpx;

}