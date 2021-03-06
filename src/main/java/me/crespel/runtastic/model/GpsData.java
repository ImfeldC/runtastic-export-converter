package me.crespel.runtastic.model;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GpsData {

	private Integer version;
	@JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss Z")
	private Date timestamp;
	private BigDecimal longitude;
	private BigDecimal latitude;
	private BigDecimal altitude;
	private Integer accuracyV;
	private Integer accuracyH;
	private BigDecimal speed;
	private Integer duration;
	private Integer distance;
	private Integer elevationGain;
	private Integer elevationLoss;

}
