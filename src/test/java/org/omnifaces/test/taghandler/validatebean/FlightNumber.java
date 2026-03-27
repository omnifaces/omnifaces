/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.test.taghandler.validatebean;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

@FlightNumberConstraint
public class FlightNumber implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String airlineDesignator;

    @NotNull
    private Integer flightIdentifier;

    public FlightNumber() {
        //
    }

    public FlightNumber(String airlineDesignator, Integer flightIdentifier) {
        this.airlineDesignator = airlineDesignator;
        this.flightIdentifier = flightIdentifier;
    }

    public String getAirlineDesignator() {
        return airlineDesignator;
    }

    public void setAirlineDesignator(String airlineDesignator) {
        this.airlineDesignator = airlineDesignator;
    }

    public Integer getFlightIdentifier() {
        return flightIdentifier;
    }

    public void setFlightIdentifier(Integer flightIdentifier) {
        this.flightIdentifier = flightIdentifier;
    }

}
