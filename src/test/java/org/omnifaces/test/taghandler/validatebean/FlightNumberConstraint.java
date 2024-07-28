package org.omnifaces.test.taghandler.validatebean;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Constraint(validatedBy = { FlightNumberValidator.class })
@Documented
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface FlightNumberConstraint {
    String message() default "Invalid flight number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}