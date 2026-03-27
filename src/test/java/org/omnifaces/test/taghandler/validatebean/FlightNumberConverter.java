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

import static java.util.Optional.ofNullable;
import static org.omnifaces.util.Messages.throwConverterException;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;

@FacesConverter(value = "flightNumberConverter", forClass = FlightNumber.class)
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

        var flightNumberAsString = submittedValue.trim().toUpperCase();

        if (!flightNumberAsString.matches("[A-Z]{2}[0-9]{1,4}")) {
            throwConverterException("Flight number must consist of 2-character airline designator and 1-4 digit flight identifier");
        }

        var airlineDesignator = flightNumberAsString.substring(0, 2);
        var flightIdentifier = Integer.parseInt(flightNumberAsString.substring(2));

        return new FlightNumber(airlineDesignator, flightIdentifier);
    }

}
