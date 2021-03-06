/**
 * 
 */
package org.onebusaway.nyc.report_archive.impl;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.report_archive.services.RecordValidationService;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.CcLocationReport;

/**
 * Default implementation of {@link RecordValidationService}
 * @author abelsare
 *
 */
@Component
public class RecordValidationServiceImpl implements RecordValidationService {

	private static final Logger log = LoggerFactory.getLogger(RecordValidationServiceImpl.class);
	
	@Override
	public boolean validateInferenceRecord(NycQueuedInferredLocationBean inferredRecord) {
		boolean isValid = true;
		  
		  String id = inferredRecord.getVehicleId();
		  int index = id.indexOf('_');
		  
		  if(index > -1) {
			  String agency = id.substring(0, index);
			  String vehicleId = id.substring(index + 1);
			  
			  //Check vehicle and agency id
			  if(StringUtils.isBlank(vehicleId)) {
				  log.error("Missing vehicle id for inference record : {}", id);
				  isValid =  false;
			  }
			  if(StringUtils.isBlank(agency)) {
				  log.error("Missing agency id for inference record : {}", id);
				  isValid = false;
			  }
		  } else {
			  log.error("Cannot parse vehicle id for inference record : {}", id);
			  isValid =  false;
		  }
		  
		  //Check time reported and service date
		  if(inferredRecord.getRecordTimestamp() == null) {
			  log.error("Missing time reported for inference record : {}", id);
			  isValid =  false;
		  }
		  
		  if(inferredRecord.getServiceDate() == null) {
			  log.error("Missing service date for inference record : {}", id);
			  isValid = false;
		  }
		  
		  //Check UUID
		  if(StringUtils.isBlank(inferredRecord.getManagementRecord().getUUID())) {
			  log.error("Missing UUID for inference record : {}", id);
			  isValid =  false;
		  }
		  
		  //Check inferred latitude and inferred longitude
		  Double inferredLatitude = inferredRecord.getInferredLatitude();
		  Double inferredLongitude = inferredRecord.getInferredLongitude(); 
		  if((!isValueWithinRange(inferredLatitude, -999.999999, 999.999999)) ||
				  (!isValueWithinRange(inferredLongitude, -999.999999, 999.999999))) {
			  isValid =  false;
		  }
		  
		  return isValid;
	}

	
	@Override
	public boolean validateRealTimeRecord(RealtimeEnvelope realTimeRecord) {
		boolean isValid = true;
		CcLocationReport ccLocationReport = realTimeRecord.getCcLocationReport();
		long vehicleId = ccLocationReport.getVehicle().getVehicleId();
		
		//Check vehicle and agency id
		if(new Long(vehicleId) == null) {
			log.error("Missing vehicle id for real time record");
			isValid =  false;
		}
		
		if(ccLocationReport.getVehicle().getAgencyId() == null) {
			log.error("Missing agency id for real time record : {}", vehicleId);
			isValid = false;
		}

		//Check time reported
		if(StringUtils.isBlank(ccLocationReport.getTimeReported())) {
			log.error("Missing time reported for real time record : {}", vehicleId);
			isValid =  false;
		}

		//Check UUID
		if(StringUtils.isBlank(realTimeRecord.getUUID())) {
			log.error("Missing UUID for real time record : {}", vehicleId);
			isValid =  false;
		}

		//Check latitude and longitude
		BigDecimal latitude = convertMicrodegreesToDegrees(ccLocationReport.getLatitude());
		BigDecimal longitude = convertMicrodegreesToDegrees(ccLocationReport.getLongitude());
		
		if((!isValueWithinRange(latitude.doubleValue(), -999.999999, 999.999999)) ||
			(!isValueWithinRange(longitude.doubleValue(), -999.999999, 999.999999))) {
			isValid =  false;
		}

		//Check direction degree
		BigDecimal directionDegree = ccLocationReport.getDirection().getDeg();
		if((!isValueWithinRange(directionDegree.doubleValue(), -999.99, 999.99)) ||
				(!isValueWithinRange(directionDegree.doubleValue(), -999.99, 999.99))) {
			log.error("Direction degree is either missing or out of range for real time record : {}", vehicleId);
			isValid =  false;
		}
		
		return isValid;
	}

	
	@Override
	public boolean isValueWithinRange(Double value, double lowerBound, double upperBound) {
		//Check for null and the valid range
		if(value == null || Double.isNaN(value) || value < lowerBound || value > upperBound) {
			return false;
		}
		return true;
	}
	
	private BigDecimal convertMicrodegreesToDegrees(int latlong) {
		return new BigDecimal(latlong * Math.pow(10.0, -6));
	}
	
}
