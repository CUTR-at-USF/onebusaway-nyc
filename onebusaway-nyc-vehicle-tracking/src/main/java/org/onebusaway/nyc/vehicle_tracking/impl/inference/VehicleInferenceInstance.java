package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilterModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ZeroProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.VehicleLocationManagementRecord;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationDetails;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class VehicleInferenceInstance {

  private static Logger _log = LoggerFactory.getLogger(VehicleInferenceInstance.class);

  private ParticleFilter<Observation> _particleFilter;

  private VehicleTrackingManagementService _managementService;

  private long _automaticResetWindow = 10 * 60 * 1000;

  private boolean _enabled = true;

  private NycVehicleLocationRecord _previousRecord = null;

  private VehicleLocationRecord _vehicleLocationRecord;

  private long _lastUpdateTime = 0;

  private long _lastGpsTime = 0;

  private NycTestLocationRecord _nycTestLocationRecord;

  public void setModel(ParticleFilterModel<Observation> model) {
    _particleFilter = new ParticleFilter<Observation>(model);
  }

  @Autowired
  public void setVehicleTrackingConfigurationService(
      VehicleTrackingManagementService managementService) {
    _managementService = managementService;
  }

  public void setAutomaticResetWindow(long automaticResetWindow) {
    _automaticResetWindow = automaticResetWindow;
  }

  /**
   * 
   * @param record
   * @return true if the resulting inferred location record should be passed on,
   *         otherwise false
   */
  public synchronized boolean handleUpdate(NycVehicleLocationRecord record) {

    // If this record occurs BEFORE the most recent update, we ignore it
    if (record.getTimeReceived() < _particleFilter.getTimeOfLastUpdated())
      return false;

    /**
     * If it's been a while since we've last seen a record, reset the particle
     * filter
     */
    if (_previousRecord != null
        && _previousRecord.getTimeReceived() + _automaticResetWindow < record.getTimeReceived()) {
      _particleFilter.reset();
    }

    /**
     * Recall that a vehicle might send a location update with missing lat-lon
     * if it's sitting at the curb with the engine turned off.
     */
    boolean latlonMissing = record.locationDataIsMissing();

    if (latlonMissing) {

      /**
       * If we don't have a previous record, we can't use the previous lat-lon
       * to replace the missing values
       */
      if (_previousRecord == null)
        return false;

      record.setLatitude(_previousRecord.getLatitude());
      record.setLongitude(_previousRecord.getLongitude());
    }

    /**
     * Sometimes, DSCs take the form "  11", where there is whitespace in there.
     * Let's clean it up.
     */
    String dsc = record.getDestinationSignCode();
    if (dsc != null) {
      dsc = dsc.trim();
      record.setDestinationSignCode(dsc);
    }

    Observation observation = new Observation(record, _previousRecord);

    _previousRecord = record;
    _vehicleLocationRecord = null;
    _nycTestLocationRecord = null;
    _lastUpdateTime = record.getTimeReceived();
    if (!latlonMissing)
      _lastGpsTime = record.getTimeReceived();

    try {
      _particleFilter.updateFilter(record.getTime(), record.getTimeReceived(),
          observation);
    } catch (ZeroProbabilityParticleFilterException ex) {
      /**
       * If the particle filter hangs, we try one hard reset to see if that will
       * fix it
       */
      _log.warn("particle filter crashed for record - attempting reset: time="
          + record.getTime() + " timeReceived=" + record.getTimeReceived()
          + " vehicleId=" + record.getVehicleId());
      _particleFilter.reset();
      try {
        _particleFilter.updateFilter(record.getTime(),
            record.getTimeReceived(), observation);
      } catch (ParticleFilterException ex2) {
        _log.warn("particle filter crashed again: time=" + record.getTime()
            + " timeReceived=" + record.getTimeReceived() + " vehicleId="
            + record.getVehicleId());
        throw new IllegalStateException(ex2);
      }
    } catch (ParticleFilterException ex) {
      throw new IllegalStateException(ex);
    }

    return _enabled;
  }

  /**
   * Her
   * 
   * @param record
   * @return
   */
  public synchronized boolean handleBypassUpdate(VehicleLocationRecord record) {
    _previousRecord = null;
    _vehicleLocationRecord = record;
    _nycTestLocationRecord = null;
    _lastUpdateTime = record.getTimeOfRecord();
    if (record.isCurrentLocationSet())
      _lastGpsTime = record.getTimeOfRecord();

    return _enabled;
  }

  public synchronized boolean handleBypassUpdate(NycTestLocationRecord record) {
    _previousRecord = null;
    _vehicleLocationRecord = null;
    _nycTestLocationRecord = record;
    _lastUpdateTime = record.getTimestamp();
    if (!record.locationDataIsMissing())
      _lastGpsTime = record.getTimestamp();

    return _enabled;
  }

  public synchronized VehicleLocationRecord getCurrentState() {
    if (_previousRecord != null)
      return getMostRecentParticleAsVehicleLocationRecord();
    else if (_vehicleLocationRecord != null)
      return _vehicleLocationRecord;
    else if (_nycTestLocationRecord != null)
      return getNycTestLocationRecordAsVehicleLocationRecord(_nycTestLocationRecord);
    else
      return null;
  }

  public VehicleLocationDetails getDetails() {

    VehicleLocationDetails details = new VehicleLocationDetails();

    NycVehicleLocationRecord lastRecord = _previousRecord;
    if (lastRecord == null && _nycTestLocationRecord != null) {
      lastRecord = new NycVehicleLocationRecord();
      lastRecord.setDestinationSignCode(_nycTestLocationRecord.getDsc());
      lastRecord.setTime(_nycTestLocationRecord.getTimestamp());
      lastRecord.setTimeReceived(_nycTestLocationRecord.getTimestamp());
      lastRecord.setLatitude(_nycTestLocationRecord.getLat());
      lastRecord.setLongitude(_nycTestLocationRecord.getLon());
    } else if (lastRecord == null && _vehicleLocationRecord != null) {
      lastRecord = new NycVehicleLocationRecord();
      lastRecord.setDestinationSignCode(null);
      lastRecord.setTime(_vehicleLocationRecord.getTimeOfRecord());
      lastRecord.setTimeReceived(_vehicleLocationRecord.getTimeOfRecord());
      lastRecord.setLatitude(_vehicleLocationRecord.getCurrentLocationLat());
      lastRecord.setLongitude(_vehicleLocationRecord.getCurrentLocationLon());
    }

    details.setLastObservation(lastRecord);

    List<Particle> particles = getCurrentParticles();
    Collections.sort(particles);
    details.setParticles(particles);

    return details;
  }

  public synchronized VehicleLocationManagementRecord getCurrentManagementState() {

    VehicleLocationManagementRecord record = new VehicleLocationManagementRecord();
    record.setEnabled(_enabled);
    record.setLastUpdateTime(_lastUpdateTime);
    record.setLastGpsTime(_lastGpsTime);

    if (_previousRecord != null) {

      record.setMostRecentDestinationSignCode(_previousRecord.getDestinationSignCode());
      record.setLastGpsLat(_previousRecord.getLatitude());
      record.setLastGpsLon(_previousRecord.getLongitude());

      Particle particle = _particleFilter.getMostLikelyParticle();

      if (particle != null) {
        VehicleState state = particle.getData();
        BlockState blockState = state.getBlockState();
        if (blockState != null)
          record.setInferredDestinationSignCode(blockState.getDestinationSignCode());

      }
    } else if (_vehicleLocationRecord != null) {

      record.setVehicleId(_vehicleLocationRecord.getVehicleId());

    } else if (_nycTestLocationRecord != null) {
      record.setMostRecentDestinationSignCode(_nycTestLocationRecord.getDsc());
      record.setInferredDestinationSignCode(_nycTestLocationRecord.getActualDsc());
    }

    VehicleLocationRecord state = getCurrentState();
    if (state != null) {
      record.setPhase(state.getPhase());
      record.setStatus(state.getStatus());
    }

    return record;
  }

  public synchronized void setVehicleStatus(boolean enabled) {
    _enabled = enabled;
  }

  public synchronized List<Particle> getCurrentParticles() {
    return new ArrayList<Particle>(_particleFilter.getWeightedParticles());
  }

  /****
   * Private Methods
   ****/

  private VehicleLocationRecord getMostRecentParticleAsVehicleLocationRecord() {
    Particle particle = _particleFilter.getMostLikelyParticle();

    VehicleState state = particle.getData();
    EdgeState edgeState = state.getEdgeState();
    MotionState motionState = state.getMotionState();
    JourneyState journeyState = state.getJourneyState();
    BlockState blockState = state.getBlockState();

    CoordinatePoint edgeLocation = edgeState.getLocationOnEdge();

    VehicleLocationRecord record = new VehicleLocationRecord();
    record.setCurrentLocationLat(edgeLocation.getLat());
    record.setCurrentLocationLon(edgeLocation.getLon());

    record.setTimeOfRecord((long) particle.getTimestamp());

    EVehiclePhase phase = journeyState.getPhase();
    record.setPhase(phase);

    Set<String> statusFields = new HashSet<String>();

    if (blockState != null) {

      BlockInstance blockInstance = blockState.getBlockInstance();
      record.setBlockId(blockInstance.getBlock().getBlock().getId());
      record.setServiceDate(blockInstance.getServiceDate());

      ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
      record.setDistanceAlongBlock(blockLocation.getDistanceAlongBlock());

      if (EVehiclePhase.IN_PROGRESS.equals(phase)) {

        double d = SphericalGeometryLibrary.distance(edgeLocation,
            blockLocation.getLocation());
        if (d > _managementService.getVehicleOffRouteDistanceThreshold())
          statusFields.add("deviated");

        int secondsSinceLastMotion = (int) ((particle.getTimestamp() - motionState.getLastInMotionTime()) / 1000);
        if (secondsSinceLastMotion > _managementService.getVehicleStalledTimeThreshold())
          statusFields.add("stalled");
      }

    }

    // Set the status field
    if (!statusFields.isEmpty()) {
      StringBuilder b = new StringBuilder();
      for (String status : statusFields) {
        if (b.length() > 0)
          b.append(',');
        b.append(status);
      }
      record.setStatus(b.toString());
    }

    return record;
  }

  private VehicleLocationRecord getNycTestLocationRecordAsVehicleLocationRecord(
      NycTestLocationRecord record) {

    if (record.getActualBlockId() == null)
      throw new IllegalStateException(
          "expected actualBlockId to be set when running in inference-bypass mode");

    VehicleLocationRecord vlr = new VehicleLocationRecord();
    vlr.setTimeOfRecord(record.getTimestamp());
    vlr.setBlockId(AgencyAndIdLibrary.convertFromString(record.getActualBlockId()));
    vlr.setServiceDate(record.getActualServiceDate());
    vlr.setDistanceAlongBlock(record.getActualDistanceAlongBlock());
    vlr.setCurrentLocationLat(record.getActualLat());
    vlr.setCurrentLocationLon(record.getActualLon());
    vlr.setPhase(EVehiclePhase.valueOf(record.getActualPhase()));
    vlr.setStatus(record.getActualStatus());
    return vlr;
  }

}
