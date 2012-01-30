/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.queue.QueueListenerTask;

public abstract class InputQueueListenerTask extends QueueListenerTask {

  public InputQueueListenerTask() {
    /*
     * use Jaxb annotation interceptor so we pick up autogenerated annotations
     * from XSDs
     */
    AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
    _mapper.getDeserializationConfig().setAnnotationIntrospector(jaxb);
  }

  public RealtimeEnvelope deserializeMessage(String contents) {
    RealtimeEnvelope message = null;
    try {
      JsonNode wrappedMessage = _mapper.readValue(contents, JsonNode.class);
      String ccLocationReportString = wrappedMessage.get("RealtimeEnvelope").toString();

      message = _mapper.readValue(ccLocationReportString,
          RealtimeEnvelope.class);
    } catch (Exception e) {
      _log.warn("Received corrupted message from queue; discarding: "
          + e.getMessage());
      _log.warn("Contents: " + contents);
    }
    return message;
  }

  @Override
  @Refreshable(dependsOn = {
      "inference-engine.inputQueueHost", "inference-engine.inputQueuePort",
      "inference-engine.inputQueueName"})
  public void startListenerThread() {
    if (_initialized == true) {
      _log.warn("Configuration service tried to reconfigure inference input queue service; this service is not reconfigurable once started.");
      return;
    }

    String host = getQueueHost();
    String queueName = getQueueName();
    Integer port = getQueuePort();

    if (host == null || queueName == null || port == null) {
      _log.info("Inference input queue is not attached; input hostname was not available via configuration service.");
      return;
    }

    _log.info("realtime archive listening on " + host + ":" + port + ", queue="
        + queueName);
    try {
      initializeQueue(host, queueName, port);
    } catch (InterruptedException ie) {
      return;
    }
  }

  @Override
  public String getQueueHost() {
    return _configurationService.getConfigurationValueAsString(
        "inference-engine.inputQueueHost", null);
  }

  @Override
  public String getQueueName() {
    return _configurationService.getConfigurationValueAsString(
        "inference-engine.inputQueueName", null);
  }

  @Override
  public Integer getQueuePort() {
    return _configurationService.getConfigurationValueAsInteger(
        "inference-engine.inputQueuePort", 5563);
  }

  @Override
  @PostConstruct
  public void setup() {
    super.setup();
  }

  @Override
  @PreDestroy
  public void destroy() {
    super.destroy();
  }

}
