/*
 * Copyright 2018 OmniFaces
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
package org.omnifaces.converter;

import java.util.List;
import java.util.Objects;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.SelectItem;

/**
 * <p>
 * The <code>omnifaces.ListConverter</code> is intented for use in specialized selection components which doesn't
 * use {@link SelectItem}s as the source for their selectable items, but work directly via a {@link List} of entities,
 * and therefore the {@link SelectItemsConverter} isn't usable on them.
 * <p>
 * This converter allows you to populate a selection component with complex Java objects and have JSF convert those
 * automatically back without the need to provide a custom converter which may need to do the job based on possibly
 * expensive service/DAO operations. This converter automatically converts based on the {@link #toString()} of the
 * selected item.
 *
 * <h3>Usage</h3>
 * <p>
 * This converter is available by converter ID <code>omnifaces.ListConverter</code> and should be used in combination
 * with <code>&lt;o:converter&gt;</code> in order to be able to pass the {@link List} source to it, which it can use
 * for conversion. Here's a basic usage example with PrimeFaces <code>&lt;p:pickList&gt;</code>, which is one of the
 * few select components which doesn't use {@link SelectItem}s as the source, but work directly via a {@link List}.
 * <pre>
 * &lt;p:pickList value="#{bean.dualListModel}" var="entity" itemValue="#{entity}" itemLabel="#{entity.someProperty}"&gt;
 *     &lt;o:converter converterId="omnifaces.ListConverter" list="#{bean.dualListModel.source}" /&gt;
 * &lt;/p:pickList&gt;
 * </pre>
 *
 * <h3>Make sure that your entity has a good <code>toString()</code> implementation</h3>
 * <p>
 * For detail, refer the javadoc of {@link SelectItemsConverter} and substitute "<code>SelectItemsConverter</code>" by
 * "<code>ListConverter</code>" and "<code>SelectItemsIndexConverter</code>" by "<code>ListIndexConverter</code>".
 *
 * @since 1.5
 * @author Arjan Tijms
 */
@FacesConverter("omnifaces.ListConverter")
public class ListConverter implements Converter<Object> {

	private List<?> list;

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String value) {
		for (Object listValue : list) {
			String convertedListValue = getAsString(context, component, listValue);
			if (Objects.equals(value, convertedListValue)) {
				return listValue;
			}
		}

		return null;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (value == null) {
			return null;
		}

		return value.toString();
	}

	public void setList(List<?> list) {
		this.list = list;
	}

}