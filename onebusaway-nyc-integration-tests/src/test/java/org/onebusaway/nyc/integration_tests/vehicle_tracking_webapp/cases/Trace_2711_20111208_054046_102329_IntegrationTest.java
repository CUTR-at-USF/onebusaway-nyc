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
package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

public class Trace_2711_20111208_054046_102329_IntegrationTest extends AbstractTraceRunner {

  public Trace_2711_20111208_054046_102329_IntegrationTest() throws Exception {
    super("2711_20111208_054046_102329.csv.gz");
    setBundle("s54", "2011-12-09T00:00:00EDT");
  }
}