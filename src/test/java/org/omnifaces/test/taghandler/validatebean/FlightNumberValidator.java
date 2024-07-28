package org.omnifaces.test.taghandler.validatebean;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class FlightNumberValidator implements ConstraintValidator<FlightNumberConstraint, FlightNumber> {

    private static final Set<Integer> AA_DISALLOWED_FLIGHT_IDENTIFIERS = new HashSet<>(Arrays.asList(11, 77));

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