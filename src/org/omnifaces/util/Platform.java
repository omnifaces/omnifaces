/*
 * Copyright 2013 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.util;

import static javax.faces.validator.BeanValidator.VALIDATOR_FACTORY_KEY;
import static javax.validation.Validation.buildDefaultValidatorFactory;
import static org.omnifaces.util.Faces.getApplicationAttribute;
import static org.omnifaces.util.Faces.setApplicationAttribute;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * This class provides access to (Java EE) platform services from the view point of JSF.
 * 
 * @author Arjan Tijms
 *
 */
public class Platform {
	
	public static ValidatorFactory getBeanValidatorFactory() {
		
		ValidatorFactory validatorFactory = getApplicationAttribute(VALIDATOR_FACTORY_KEY);
		
		if (validatorFactory == null) {
			validatorFactory = buildDefaultValidatorFactory();
			setApplicationAttribute(VALIDATOR_FACTORY_KEY, validatorFactory);
		}
		
		return validatorFactory;
	}
	
	public static Validator getBeanValidator() {
		return getBeanValidatorFactory().getValidator();
	}

}
