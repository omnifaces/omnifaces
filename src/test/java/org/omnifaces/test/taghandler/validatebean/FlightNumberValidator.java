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

import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FlightNumberValidator implements ConstraintValidator<FlightNumberConstraint, FlightNumber> {

    private static final Set<Integer> AA_DISALLOWED_FLIGHT_IDENTIFIERS = Set.of(11, 77);

    @Override
    public void initialize(FlightNumberConstraint constraintAnnotation) {
        //
    }

    @Override
    public boolean isValid(FlightNumber flightNumber, ConstraintValidatorContext context) {
        if ("AA".equals(flightNumber.getAirlineDesignator())) {
            return !AA_DISALLOWED_FLIGHT_IDENTIFIERS.contains(flightNumber.getFlightIdentifier());
        }

        return true;
    }

}
