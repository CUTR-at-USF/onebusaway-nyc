package org.onebusaway.nyc.transit_data_federation.services.nyc;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.collect.TreeMultimap;

public interface RunService {
  public String getInitialRunForTrip(AgencyAndId trip);

  public String getReliefRunForTrip(AgencyAndId trip);

  public int getReliefTimeForTrip(AgencyAndId trip);

  public Collection<RunTripEntry> getRunTripEntriesForRun(String runId);

  public RunTripEntry getActiveRunTripEntryForRunAndTime(String runId,
      long time);

  public List<RunTripEntry> getActiveRunTripEntriesForAgencyAndTime(String agencyId,
      long time);

  public RunTripEntry getPreviousEntry(RunTripEntry entry, long date);

  public RunTripEntry getNextEntry(RunTripEntry entry, long date);

  public RunTripEntry getActiveRunTripEntryForBlockInstance(
      BlockInstance blockInstance, int scheduleTime);

  public ScheduledBlockLocation getSchedBlockLocForRunTripEntryAndTime(
      RunTripEntry runTrip, long timestamp);

  public BlockInstance getBlockInstanceForRunTripEntry(RunTripEntry rte,
      Date serviceDate);

  public RunTripEntry getRunTripEntryForTripAndTime(TripEntry trip, int scheduledTime);

  public boolean isValidRunNumber(String runNumber);

  public TreeMultimap<Integer, String> getBestRunIdsForFuzzyId(
      String runAgencyAndId) throws IllegalArgumentException;

  public Collection<? extends AgencyAndId> getTripIdsForRunId(String runId);

  public boolean isValidRunId(String runId);

  public Set<AgencyAndId> getRoutesForRunId(String opAssignedRunId);

  public Collection<RunTripEntry> getRunTripsForTrip(TripEntry trip); 
  public Set<String> getRunIdsForTrip(TripEntry trip);

}
