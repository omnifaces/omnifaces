/*
 * Copyright 2017 OmniFaces
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

import static java.util.regex.Pattern.quote;
import static org.omnifaces.util.Faces.createConverter;
import static org.omnifaces.util.Reflection.instance;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.isEmpty;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

import javax.el.ValueExpression;
import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import org.omnifaces.el.ExpressionInspector;
import org.omnifaces.util.Faces;

/**
 * <p>
 * The <code>omnifaces.ToCollectionConverter</code> is intented to convert submitted {@link String} values to a Java
 * collection based on a delimiter. Additionally, it trims any whitespace around each delimited submitted value. This is
 * useful for among others comma separated value inputs.
 *
 * <h3>Usage</h3>
 * <p>
 * This converter is available by converter ID <code>omnifaces.ToCollectionConverter</code>. Just specify it in the
 * <code>converter</code> attribute of the component referring the <code>Collection</code> property. For example:
 * <pre>
 * &lt;h:inputText value="#{bean.commaSeparatedValues}" converter="omnifaces.ToCollectionConverter" /&gt;
 * </pre>
 * <p>
 * The default delimiter is comma followed by space <code>, </code> and the default collection type is
 * <code>java.util.LinkedHashSet</code> for a <code>Set</code> property and <code>java.util.ArrayList</code> for anything
 * else, and the default converter for each item will in <code>getAsString()</code> be determined based on item type and
 * in <code>getAsObject()</code> be determined based on generic return type of the getter method.
 * <p>
 * You can use <code>&lt;o:converter&gt;</code> to specify those attributes. The <code>delimiter</code> must be a
 * <code>String</code>, the <code>collectionType</code> must be a FQN and the <code>itemConverter</code> can be
 * anything which is acceptable by {@link Faces#createConverter(Object)}.
 * <pre>
 * &lt;h:inputText value="#{bean.uniqueOrderedSemiColonSeparatedNumbers}"&gt;
 *     &lt;o:converter converterId="omnifaces.ToCollectionConverter"
 *                  delimiter=";"
 *                  collectionType="java.util.TreeSet"
 *                  itemConverter="javax.faces.Integer" &gt;
 * &lt;/h:inputText&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @see TrimConverter
 * @since 2.6
 */
@FacesConverter("omnifaces.ToCollectionConverter")
public class ToCollectionConverter extends TrimConverter {

	private static final String DEFAULT_DELIMITER = ",";
	private static final String DEFAULT_SET_TYPE = "java.util.LinkedHashSet";
	private static final String DEFAULT_COLLECTION_TYPE = "java.util.ArrayList";

	private String delimiter;
	private String collectionType;
	private Object itemConverter;

	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String submittedValue) {
		if (isEmpty(submittedValue)) {
			return null;
		}

		String type = collectionType;
		Converter converter = createConverter(itemConverter);
		ValueExpression valueExpression = component.getValueExpression("value");

		if (valueExpression != null) {
			Method getter = ExpressionInspector.getMethodReference(context.getELContext(), valueExpression).getMethod();
			Class<?> returnType = getter.getReturnType();

			if (!Collection.class.isAssignableFrom(returnType)) {
				throw new IllegalArgumentException(valueExpression.getExpressionString() + " does not resolve to Collection.");
			}

			if (collectionType == null && Set.class.isAssignableFrom(returnType)) {
				type = DEFAULT_SET_TYPE;
			}

			if (converter == null) {
				Type[] actualTypeArguments = ((ParameterizedType) getter.getGenericReturnType()).getActualTypeArguments();

				if (actualTypeArguments.length > 0) {
					Class<?> forClass = (Class<?>) actualTypeArguments[0];
					converter = context.getApplication().createConverter(forClass);
				}
			}
		}

		Collection<Object> collection = instance(coalesce(type, DEFAULT_COLLECTION_TYPE));

		for (String item : submittedValue.split(quote(coalesce(delimiter, DEFAULT_DELIMITER).trim()))) {
			Object trimmed = super.getAsObject(context, component, item);
			collection.add(converter == null ? trimmed : converter.getAsString(context, component, trimmed));
		}

		return collection;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object modelValue) {
		if (isEmpty(modelValue)) {
			return "";
		}

		Application application = context.getApplication();
		StringBuilder builder = new StringBuilder();
		String specifiedDelimiter = coalesce(delimiter, DEFAULT_DELIMITER);
		Converter specifiedConverter = createConverter(itemConverter);
		Class<?> forClass = null;
		Converter converter = specifiedConverter;
		int i = 0;

		for (Object item : (Collection<?>) modelValue) {
			if (i++ > 0) {
				builder.append(specifiedDelimiter);
			}

			if (specifiedConverter == null && item != null && forClass != item.getClass()) {
				forClass = item.getClass();
				converter = application.createConverter(forClass);
			}

			builder.append(converter == null ? super.getAsString(context, component, item) : converter.getAsString(context, component, item));
		}

		return builder.toString();
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public void setCollectionType(String collectionType) {
		this.collectionType = collectionType;
	}

	public void setItemConverter(Object itemConverter) {
		this.itemConverter = itemConverter;
	}

}