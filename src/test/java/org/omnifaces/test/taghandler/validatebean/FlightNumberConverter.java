package org.omnifaces.test.taghandler.validatebean;

import static java.util.Optional.ofNullable;
import static org.omnifaces.util.Messages.throwConverterException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter(value="flightNumberConverter", forClass=FlightNumber.class)
public class FlightNumberConverter implements Converter<FlightNumber> {

    @Override
    public String getAsString(FacesContext context, UIComponent component, FlightNumber modelValue) {
        if (modelValue == null) {
            return "";
        }

        return ofNullable(modelValue.getAirlineDesignator()).orElse("")
             + ofNullable(modelValue.getFlightIdentifier()).map(String::valueOf).orElse("");
    }

    @Override
    public FlightNumber getAsObject(FacesContext context, UIComponent component, String submittedValue) {
        if (submittedValue == null || submittedValue.trim().isEmpty()) {
            return null;
        }

        String flightNumberAsString = submittedValue.trim().toUpperCase();

        if (!flightNumberAsString.matches("[A-Z]{2}[0-9]{1,4}")) {
            throwConverterException("Flight number must consist of 2-character airline designator and 1-4 digit flight identifier");
        }

        String airlineDesignator = flightNumberAsString.substring(0, 2);
        Integer flightIdentifier = Integer.parseInt(flightNumberAsString.substring(2));

        return new FlightNumber(airlineDesignator, flightIdentifier);
    }

}