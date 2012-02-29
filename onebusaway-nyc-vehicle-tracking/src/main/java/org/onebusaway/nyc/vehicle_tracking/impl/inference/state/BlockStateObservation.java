package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.VehicleStateLibrary;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class represents the combination of an observation and a BlockState.
 * Specifically, it contains information about the BlockState that is
 * conditional on when it was observed.
 * 
 * @author bwillard
 * 
 */
public final class BlockStateObservation implements
    Comparable<BlockStateObservation> {
  final private BlockState _blockState;

  private final Boolean _isOpAssigned;

  private final Boolean _isRunReported;

  private final Boolean _isRunReportedAssignedMismatch;
  
  private final boolean _isAtPotentialLayoverSpot;

  private VehicleStateLibrary _vehicleStateLibrary;

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }
  
  public BlockStateObservation(BlockState blockState, Observation obs, 
      boolean isAtPotentialLayoverSpot) {

    Preconditions.checkNotNull(obs);
    _blockState = Preconditions.checkNotNull(blockState);

    final String runId = blockState.getRunId();
    _isOpAssigned = obs.getOpAssignedRunId() != null
        ? obs.getOpAssignedRunId().equals(runId) : null;
    _isRunReported = (obs.getBestFuzzyRunIds() != null && !obs.getBestFuzzyRunIds().isEmpty())
        ? obs.getBestFuzzyRunIds().contains(runId) : null;
    _isRunReportedAssignedMismatch = _isOpAssigned != null
        && _isRunReported != null ? _isOpAssigned && !_isRunReported : null;
    _isAtPotentialLayoverSpot = isAtPotentialLayoverSpot;
  }

  private BlockStateObservation(BlockState blockState, Boolean isRunReported,
      Boolean isUTSassigned, Boolean isRunAM, boolean isAtLayoverSpot) {
    _blockState = blockState;
    this._isRunReported = isRunReported;
    this._isOpAssigned = isUTSassigned;
    this._isRunReportedAssignedMismatch = isRunAM;
    this._isAtPotentialLayoverSpot = isAtLayoverSpot;
  }

  public BlockStateObservation(BlockStateObservation state) {
    this(state._blockState, state._isRunReported, state._isOpAssigned,
        state._isRunReportedAssignedMismatch, state._isAtPotentialLayoverSpot);
  }

  public BlockState getBlockState() {
    return _blockState;
  }

  public Boolean getRunReported() {
    return _isRunReported;
  }

  public Boolean getOpAssigned() {
    return _isOpAssigned;
  }

  public Boolean isRunReportedAssignedMismatch() {
    return _isRunReportedAssignedMismatch;
  }

  @Override
  public int compareTo(BlockStateObservation rightBs) {

    if (this == rightBs)
      return 0;

    final int res = ComparisonChain.start().compare(
        this._isRunReportedAssignedMismatch,
        rightBs.isRunReportedAssignedMismatch(), Ordering.natural().nullsLast()).compare(
        this._isRunReported, rightBs.getRunReported(),
        Ordering.natural().nullsLast()).compare(this._isOpAssigned,
        rightBs.getOpAssigned(), Ordering.natural().nullsLast()).compare(
        this._blockState, rightBs._blockState).result();
    return res;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper("BlockStateObservation")
        .addValue(_blockState)
        .add("isOpAssigned", _isOpAssigned)
        .add("isRunReported", _isRunReported)
        .toString();
  }

  private int _hash = 0;

  @Override
  public int hashCode() {
    if (_hash != 0)
      return _hash;

    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((_blockState == null) ? 0 : _blockState.hashCode());
    result = prime * result
        + ((_isOpAssigned == null) ? 0 : _isOpAssigned.hashCode());
    result = prime * result
        + ((_isRunReported == null) ? 0 : _isRunReported.hashCode());
    result = prime
        * result
        + ((_isRunReportedAssignedMismatch == null) ? 0
            : _isRunReportedAssignedMismatch.hashCode());
    _hash = result;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof BlockStateObservation)) {
      return false;
    }
    final BlockStateObservation other = (BlockStateObservation) obj;
    if (_blockState == null) {
      if (other._blockState != null) {
        return false;
      }
    } else if (!_blockState.equals(other._blockState)) {
      return false;
    }
    if (_isOpAssigned == null) {
      if (other._isOpAssigned != null) {
        return false;
      }
    } else if (!_isOpAssigned.equals(other._isOpAssigned)) {
      return false;
    }
    if (_isRunReported == null) {
      if (other._isRunReported != null) {
        return false;
      }
    } else if (!_isRunReported.equals(other._isRunReported)) {
      return false;
    }
    if (_isRunReportedAssignedMismatch == null) {
      if (other._isRunReportedAssignedMismatch != null) {
        return false;
      }
    } else if (!_isRunReportedAssignedMismatch.equals(other._isRunReportedAssignedMismatch)) {
      return false;
    }
    return true;
  }

  public boolean isAtPotentialLayoverSpot() {
    return _isAtPotentialLayoverSpot;
  }
}