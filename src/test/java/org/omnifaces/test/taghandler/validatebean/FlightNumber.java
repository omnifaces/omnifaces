package org.omnifaces.test.taghandler.validatebean;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

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
