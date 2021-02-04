package me.crespel.runtastic.model;

import java.util.List;

import lombok.Data;

@Data
public class HeartRateSession {

    private String fileName;

    private List<HeartRateData> heartRateData;

}