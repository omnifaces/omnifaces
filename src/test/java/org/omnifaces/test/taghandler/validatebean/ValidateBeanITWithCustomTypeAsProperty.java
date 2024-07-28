package org.omnifaces.test.taghandler.validatebean;

import static org.omnifaces.util.Faces.isValidationFailed;
import static org.omnifaces.util.Messages.addGlobalInfo;
import static org.omnifaces.util.Messages.addGlobalWarn;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.validation.Valid;

import org.omnifaces.cdi.ViewScoped;

@Named
@ViewScoped
public class ValidateBeanITWithCustomTypeAsProperty implements Serializable {

    private static final long serialVersionUID = 1L;

    @Valid
    private List<FlightNumber> flightNumbers;

    @PostConstruct
    public void init() {
        flightNumbers = Arrays.asList(new FlightNumber("AA", 708), null);
    }

    public void action() {
        if (isValidationFailed()) {
            addGlobalWarn(" actionValidationFailed");
        }
        else {
            addGlobalInfo("actionSuccess");
        }
    }

    public List<FlightNumber> getFlightNumbers() {
        return flightNumbers;
    }

    public void setFlightNumbers(List<FlightNumber> flightNumbers) {
        this.flightNumbers = flightNumbers;
    }
}