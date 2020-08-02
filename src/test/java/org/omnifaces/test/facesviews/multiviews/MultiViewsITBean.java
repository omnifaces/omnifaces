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
package org.omnifaces.test.facesviews.multiviews;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import org.omnifaces.cdi.Param;

@Named
@RequestScoped
public class MultiViewsITBean {

	@Param(pathIndex=0, disableBeanValidation=true) // disableBeanValidation is a work around for buggy BVal in TomEE 8.0.0-M2
	private String firstPathParamAsString;

	@Param(pathIndex=1, disableBeanValidation=true) // disableBeanValidation is a work around for buggy BVal in TomEE 8.0.0-M2
	private Integer secondPathParamAsInteger;

	public String getFirstPathParamAsString() {
		return firstPathParamAsString;
	}

	public Integer getSecondPathParamAsInteger() {
		return secondPathParamAsInteger;
	}

}