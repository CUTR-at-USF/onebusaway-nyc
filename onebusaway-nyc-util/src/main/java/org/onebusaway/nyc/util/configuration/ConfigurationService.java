package org.onebusaway.nyc.util.configuration;

import java.util.Map;

/**
 * Service interface for getting configuration parameters from a centrally-distributed
 * configuration service.
 * 
 * @author jmaki
 */
public interface ConfigurationService {

  /**
   * Get a value for the given configuration key as a string.
   * 
   * @param configurationItemKey The configuration item key.
   * @param defaultValue The value to return if a value for the configurationItemKey 
   * 		  is not found.
   */
  public String getConfigurationValueAsString(String configurationItemKey,
      String defaultValue);

  public Float getConfigurationValueAsFloat(String configurationItemKey,
      Float defaultValue);
  
  public Integer getConfigurationValueAsInteger(String configurationItemKey,
	      Integer defaultValue);
  
  /**
   * Set a value for the given configuration key as a string.
   * 
   * @param component The component to which this key value pair belongs
   * @param configurationItemKey The configuration item key.
   * @param value The value to set the configuration param to.
   */  
  public void setConfigurationValue(String component, String configurationItemKey, 
		  String value) throws Exception;
  
  /**
   * Get a collection of all key value pairs stored in tdm_config.xml on TDM server
   * @return collection of all config key value pairs
   */
  public Map<String, String> getConfiguration();
}
