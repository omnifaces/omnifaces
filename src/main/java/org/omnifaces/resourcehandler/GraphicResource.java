/*
 * Copyright 2014 OmniFaces.
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
package org.omnifaces.resourcehandler;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Servlets.toQueryString;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isNumber;
import static org.omnifaces.util.Utils.isOneAnnotationPresent;
import static org.omnifaces.util.Utils.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.xml.bind.DatatypeConverter;

import org.omnifaces.component.output.GraphicImage;
import org.omnifaces.el.ExpressionInspector;
import org.omnifaces.el.MethodReference;

/**
 * <p>
 * This {@link Resource} implementation is used by the {@link GraphicImage} component.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public class GraphicResource extends DynamicResource {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String ERROR_INVALID_LASTMODIFIED =
		"o:graphicImage 'lastModified' attribute must be an instance of Number or Date."
			+ " Encountered an invalid value of '%s'.";
	private static final String ERROR_MISSING_VALUE =
		"o:graphicImage 'value' attribute is required.";
	private static final String ERROR_UNKNOWN_METHOD =
		"o:graphicImage 'value' attribute must refer an existing method."
			+ " Encountered an unknown method of '%s'.";
	private static final String ERROR_INVALID_SCOPE =
		"o:graphicImage 'value' attribute must refer an @ApplicationScoped bean."
			+ " Cannot find the right annotation on bean class '%s'.";
	private static final String ERROR_INVALID_RETURNTYPE =
		"o:graphicImage 'value' attribute must represent a method returning an InputStream or byte[]."
			+ " Encountered an invalid return value of '%s'.";
	private static final String ERROR_INVALID_PARAMS =
		"o:graphicImage 'value' attribute must specify valid method parameters."
			+ " Encountered invalid method parameters '%s'.";

	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation>[] REQUIRED_ANNOTATION_TYPES = new Class[] {
		javax.faces.bean.ApplicationScoped.class, javax.enterprise.context.ApplicationScoped.class
	};

	private static final Map<String, MethodReference> ALLOWED_METHODS = new HashMap<>();
	private static final GraphicImage DUMMY_COMPONENT = new GraphicImage();
	private static final String[] EMPTY_PARAMS = new String[0];

	// Variables ------------------------------------------------------------------------------------------------------

	private Object content;
	private String[] params;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new graphic resource which uses the given content as data URI.
	 * @param content The graphic resource content, to be represented as data URI.
	 */
	public GraphicResource(Object content) {
		super("", GraphicResourceHandler.LIBRARY_NAME, "image");
		this.content = content;
	}

	/**
	 * Construct a new graphic resource based on the given name, EL method parameters converted as string, and the
	 * "last modified" representation.
	 * @param name The graphic resource name, usually representing the base and method of EL method expression.
	 * @param params The graphic resource method parameters.
	 * @param lastModified The "last modified" representation of the graphic resource, can be {@link Long} or
	 * {@link Date}, or otherwise an attempt will be made to parse it as {@link Long}.
	 * @throws IllegalArgumentException If "last modified" can not be parsed to a timestamp.
	 */
	public GraphicResource(String name, String[] params, Object lastModified) {
		super(name, GraphicResourceHandler.LIBRARY_NAME, "image");
		this.params = coalesce(params, EMPTY_PARAMS);

		if (lastModified instanceof Long) {
			setLastModified((Long) lastModified);
		}
		else if (lastModified instanceof Date) {
			setLastModified(((Date) lastModified).getTime());
		}
		else if (isNumber(String.valueOf(lastModified))) {
			setLastModified(Long.valueOf(lastModified.toString()));
		}
		else if (lastModified != null) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_LASTMODIFIED, lastModified));
		}
	}

	/**
	 * Create a new graphic resource based on the given graphic image component.
	 * This is called by {@link GraphicImage} component.
	 * @param context The involved faces context.
	 * @param component The graphic image component to create a new graphic resource for.
	 * @return The new graphic resource.
	 * @throws IllegalArgumentException When the "value" attribute of the given component is absent or does not
	 * represent a method expression referring an existing method taking at least one argument.
	 */
	public static GraphicResource create(FacesContext context, GraphicImage component) {
		ValueExpression value = component.getValueExpression("value");

		if (value == null) {
			throw new IllegalArgumentException(ERROR_MISSING_VALUE);
		}

		if (Boolean.valueOf(String.valueOf(component.getAttributes().get("dataURI"))) == TRUE) {
			return new GraphicResource(component.getAttributes().get("value"));
		}

		MethodReference methodReference = ExpressionInspector.getMethodReference(context.getELContext(), value);

		if (methodReference.getMethod() == null) {
			throw new IllegalArgumentException(String.format(ERROR_UNKNOWN_METHOD, value.getExpressionString()));
		}

		String name = getResourceName(methodReference);

		if (!ALLOWED_METHODS.containsKey(name)) { // No need to validate everytime when already known.
			Class<? extends Object> beanClass = methodReference.getBase().getClass();

			if (!isOneAnnotationPresent(beanClass, REQUIRED_ANNOTATION_TYPES)) {
				throw new IllegalArgumentException(String.format(ERROR_INVALID_SCOPE, beanClass));
			}

			ALLOWED_METHODS.put(name, methodReference);
		}

		String[] params = extractParams(value.getExpressionString());
		Class<?>[] paramTypes = methodReference.getMethod().getParameterTypes();
		String[] evaluatedParams = evaluateParams(context, component, params, paramTypes);
		return new GraphicResource(name, evaluatedParams, component.getAttributes().get("lastModified"));
	}

	/**
	 * An override which appends the converted method parameters to the query string.
	 */
	@Override
	public String getRequestPath() {
		if (content != null) {
			return getDataURI();
		}
		else {
			return super.getRequestPath() + (isEmpty(params) ? "" : ("&" + toQueryString(singletonMap("p", asList(params)))));
		}
	}

	/**
	 * Returns the data URI for resource's content.
	 * @return The data URI for resource's content.
	 */
	protected String getDataURI() {
		byte[] bytes = null;

		try {
			if (content instanceof InputStream) {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				stream((InputStream) content, output);
				bytes = output.toByteArray();
			}
			else if (content instanceof byte[]) {
				bytes = (byte[]) content;
			}
			else {
				throw new IllegalArgumentException(String.format(ERROR_INVALID_RETURNTYPE, content));
			}
		}
		catch (Exception e) {
			throw new FacesException(e);
		}

		return "data:image;base64," + DatatypeConverter.printBase64Binary(bytes);
	}

	/**
	 * @throws IllegalArgumentException When the "value" attribute of the initial component does not represent a method
	 * expression returning an {@link InputStream} or <code>byte[]</code>.
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		MethodReference methodReference = ALLOWED_METHODS.get(getResourceName().split("\\.", 2)[0]);

		if (methodReference == null) {
			return null; // Ignore hacker attempts. I'd rather return 400 here, but JSF spec doesn't support it.
		}

		try {
			Method method = methodReference.getMethod();
			Object[] convertedParams = convertParams(getContext(), params, method.getParameterTypes());
			Object content = method.invoke(methodReference.getBase(), convertedParams);

			if (content instanceof InputStream) {
				return (InputStream) content;
			}
			else if (content instanceof byte[]) {
				return new ByteArrayInputStream((byte[]) content);
			}
			else {
				throw new IllegalArgumentException(String.format(ERROR_INVALID_RETURNTYPE, content));
			}
		}
		catch (Exception e) {
			throw new IOException(e); // I'd rather return 400 here, but JSF spec doesn't support it.
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * This must return an unique and URL-safe identifier of the bean+method without any periods.
	 */
	private static String getResourceName(MethodReference methodReference) {
		return methodReference.getBase().getClass().getSimpleName() + "_" + methodReference.getMethod().getName();
	}

	/**
	 * Extract the individual method parameters from the given EL expression.
	 * This returns an empty array when none is found.
	 */
	private static String[] extractParams(String expression) {
		if (expression.matches("#\\{.+\\..+\\(.+\\)\\}")) { // "Does it look like #{bean.method(...)}?". True, not a super exact match, but all others cause EL exception anyway before ever hitting this method.
			return expression.substring(expression.indexOf('(') + 1, expression.lastIndexOf(')')).split("\\s*,\\s*(?![^()]*+\\))"); // "Split on comma as long as comma isn't inside parentheses".
		}
		else {
			return EMPTY_PARAMS;
		}
	}

	/**
	 * Evaluate the given individual method parameters and convert them to HTTP request parameters using converters
	 * registered on given parameter types.
	 * @throws IllegalArgumentException When The length of given parameters doesn't match those of given types.
	 */
	private static String[] evaluateParams(FacesContext context, UIComponent component, String[] params, Class<?>[] paramTypes) {
		if (params.length != paramTypes.length) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_PARAMS, Arrays.toString(params)));
		}

		String[] evaluatedParams = new String[params.length];
		Application application = context.getApplication();

		for (int i = 0; i < params.length; i++) {
			Object value = application.evaluateExpressionGet(context, "#{" + params[i] + "}", paramTypes[i]);
			Converter converter = application.createConverter(paramTypes[i]);
			evaluatedParams[i] = (converter != null)
				? converter.getAsString(context, component, value)
				: (value != null) ? value.toString() : "";
		}

		return evaluatedParams;
	}

	/**
	 * Convert the given HTTP request parameters back to objects using converters registered on given parameter types.
	 * @throws IllegalArgumentException When The length of given parameters doesn't match those of given types.
	 */
	private static Object[] convertParams(FacesContext context, String[] params, Class<?>[] paramTypes) {
		if (params.length != paramTypes.length) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_PARAMS, Arrays.toString(params)));
		}

		Object[] convertedParams = new Object[params.length];
		Application application = context.getApplication();

		for (int i = 0; i < params.length; i++) {
			String param = isEmpty(params[i]) ? null : params[i];
			Converter converter = application.createConverter(paramTypes[i]);
			convertedParams[i] = (converter != null)
				? converter.getAsObject(context, DUMMY_COMPONENT, param)
				: param;
		}

		return convertedParams;
	}

}