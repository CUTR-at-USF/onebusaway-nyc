package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;

public class RawRunData {

  private String run1;
  private String run2;
  private ServiceCode serviceCode;
  private String nextRun;
  private String serviceId;
  private String block;
  private String depotCode;

  public RawRunData(String run1, String run2, String nextRun, String serviceId, ServiceCode serviceCode, String block, String depotCode) {
    this.run1 = run1;
    this.run2 = run2;
    this.nextRun = nextRun;
    this.serviceId = serviceId;
    this.serviceCode = serviceCode;
    this.block = block;
    this.depotCode = depotCode;
  }

  public String getRun1() {
    return run1;
  }

  public void setRun1(String run1) {
    this.run1 = run1;
  }

  public String getRun2() {
    return run2;
  }

  public void setRun2(String run2) {
    this.run2 = run2;
  }

  public ServiceCode getServiceCode() {
    return serviceCode;
  }

  public String getNextRun() {
    return nextRun;
  }

  public String getServiceId() {
    return serviceId;
  }

  public String getBlock() {
    return block;
  }

  public void setBlock(String block) {
    this.block = block;
  }

  public String getDepotCode() {
    return depotCode;
  }

  public void setDepotCode(String depotCode) {
    this.depotCode = depotCode;
  }
}
