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
package org.omnifaces.test.cdi.param;

import static java.util.Arrays.asList;
import static org.omnifaces.util.Faces.isValidationFailed;

import java.time.LocalDate;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.convert.DateTimeConverter;
import jakarta.faces.validator.LengthValidator;
import jakarta.faces.validator.LongRangeValidator;
import jakarta.inject.Named;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.omnifaces.cdi.Param;
import org.omnifaces.cdi.param.Attribute;

@Named
@RequestScoped
public class ParamITBean {

	@Param @Size(min = 2)
	private String stringParam;

	@Param(
		validatorClasses = LengthValidator.class,
		validatorAttributes = @Attribute(name = "minimum", value = "2"),
		label = "String param array")
	private String[] stringParamArray;

	@Param(
		validatorClasses = LengthValidator.class,
		validatorAttributes = @Attribute(name = "minimum", value = "2"),
		validatorMessage = "Invalid length")
	private List<String> stringParamList;

	@Param @Min(value = 42, message = "Invalid range")
	private Long longParam;

	@Param(
		validatorClasses = LongRangeValidator.class,
		validatorAttributes = @Attribute(name = "minimum", value="42"),
		validatorMessage = "Invalid range")
	private Long[] longParamArray;

	@Param(
		validatorClasses = LongRangeValidator.class,
		validatorAttributes = @Attribute(name = "minimum", value="42"),
		label = "Long param list")
	private List<Long> longParamList;

	@Param(
	    converterClass = DateTimeConverter.class,
	    converterAttributes = { @Attribute(name = "type", value = "localDate"), @Attribute(name = "pattern", value = "yyyyMMdd") },
	    converterMessage = "{1}: \"{0}\" is not the date format we had in mind! Please use the format yyyyMMdd.")
	private LocalDate dateParam;

	@Param(validatorClasses = ParamITEntityValidator.class)
	private ParamITEntity entityParam;

	private ParamITEntity entityViewParam;

	private String initResult;

	@PostConstruct
	public void init() {
		if (isValidationFailed()) {
			initResult = "initValidationFailed";
		}
		else {
			initResult = "initSuccess";
		}
	}

	public String getStringParam() {
		return stringParam;
	}

	public List<String> getStringParamArray() {
		return stringParamArray == null ? null : asList(stringParamArray); // Just for friendly toString in EL.
	}

	public List<String> getStringParamList() {
		return stringParamList;
	}

	public Long getLongParam() {
		return longParam;
	}

	public List<Long> getLongParamArray() {
		return longParamArray == null ? null : asList(longParamArray); // Just for friendly toString in EL.
	}

	public List<Long> getLongParamList() {
		return longParamList;
	}

	public LocalDate getDateParam() {
		return dateParam;
	}

	public ParamITEntity getEntityParam() {
		return entityParam;
	}

	public String getInitResult() {
		return initResult;
	}

	public ParamITEntity getEntityViewParam() {
		return entityViewParam;
	}

	public void setEntityViewParam(ParamITEntity entityViewParam) {
		this.entityViewParam = entityViewParam;
	}

}