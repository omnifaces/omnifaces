/*
 * Copyright 2020 OmniFaces
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

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidateBeanITEntityValidator implements ConstraintValidator<ValidateBeanITEntityConstraint, ValidateBeanITEntity> {

	@Override
	public void initialize(ValidateBeanITEntityConstraint constraint) {
		//
	}

	@Override
	public boolean isValid(ValidateBeanITEntity entity, ConstraintValidatorContext context) {
		if (entity.getNumber1() < entity.getNumber2()) {
			return true;
		}

		if (entity.getNumber1() == 2) {
			// Specific case to test targeting a specific property node when using showMessageFor="@violating" so that the message is only shown for number1 and not for both number1 and number2.
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate()).addPropertyNode("number1").addConstraintViolation();
		}

		return false;
	}

}