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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Servlets.toQueryString;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isNumber;
import static org.omnifaces.util.Utils.isOneAnnotationPresent;
import static org.omnifaces.util.Utils.toByteArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.Resource;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.xml.bind.DatatypeConverter;

import org.omnifaces.component.output.GraphicImage;
import org.omnifaces.el.ExpressionInspector;
import org.omnifaces.el.MethodReference;
import org.omnifaces.util.Faces;

/**
 * <p>
 * This {@link Resource} implementation is used by the {@link GraphicImage} component.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public class GraphicResource extends DynamicResource {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final String DEFAULT_CONTENT_TYPE = "image";
	private static final Map<String, String> CONTENT_TYPES_BY_BASE64_HEADER = createContentTypesByBase64Header();
	private static final Map<String, MethodReference> ALLOWED_METHODS = new ConcurrentHashMap<>();
	private static final GraphicImage DUMMY_COMPONENT = new GraphicImage();
	private static final String[] EMPTY_PARAMS = new String[0];
	private static final int RESOURCE_NAME_FULL_PARTS_LENGTH = 3;

	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation>[] REQUIRED_ANNOTATION_TYPES = new Class[] {
		javax.faces.bean.ApplicationScoped.class, javax.enterprise.context.ApplicationScoped.class
	};

	private static final String ERROR_INVALID_LASTMODIFIED =
		"o:graphicImage 'lastModified' attribute must be an instance of Long or Date."
			+ " Encountered an invalid value of '%s'.";
	private static final String ERROR_INVALID_TYPE =
		"o:graphicImage 'type' attribute must represent a valid file extension."
			+ " Encountered an invalid value of '%s'.";
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

	private static Map<String, String> createContentTypesByBase64Header() {
		Map<String, String> contentTypesByBase64Header = new HashMap<>();
		contentTypesByBase64Header.put("/9j/", "image/jpeg");
		contentTypesByBase64Header.put("iVBORw", "image/png");
		contentTypesByBase64Header.put("R0lGOD", "image/gif");
		contentTypesByBase64Header.put("AAABAA", "image/x-icon");
		contentTypesByBase64Header.put("PD94bW", "image/svg+xml");
		contentTypesByBase64Header.put("Qk0", "image/bmp");
		contentTypesByBase64Header.put("SUkqAA", "image/tiff");
		contentTypesByBase64Header.put("TU0AKg", "image/tiff");
		return Collections.unmodifiableMap(contentTypesByBase64Header);
	}

	// Variables ------------------------------------------------------------------------------------------------------

	private String base64;
	private String[] params;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new graphic resource which uses the given content as data URI.
	 * @param content The graphic resource content, to be represented as data URI.
	 * @param contentType The graphic resource content type. If this is <code>null</code>, then it will be guessed
	 * based on the content type signature in the content header. So far, JPEG, PNG, GIF, ICO, SVG, BMP and TIFF are
	 * recognized. Else if this represents the file extension, then it will be resolved based on mime mappings.
	 */
	public GraphicResource(Object content, String contentType) {
		super("", GraphicResourceHandler.LIBRARY_NAME, contentType);
		base64 = convertToBase64(content);

		if (contentType == null) {
			setContentType(guessContentType(base64));
		}
		else if (!contentType.contains("/")) {
			setContentType(resolveContentType(contentType));
		}
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
		super(name, GraphicResourceHandler.LIBRARY_NAME, getContentType(name));
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
	 * Create a new graphic resource based on the given value expression.
	 * This is called by {@link GraphicImage} component.
	 * @param context The involved faces context.
	 * @param value The value expression representing content to create a new graphic resource for.
	 * @param type  The image type, represented as file extension. E.g. "jpg", "png", "gif", "ico", "svg", "bmp",
	 * "tiff", etc.
	 * @param lastModified The "last modified" representation of the graphic resource, can be {@link Long} or
	 * {@link Date}, or otherwise an attempt will be made to parse it as {@link Long}.
	 * @return The new graphic resource.
	 * @throws IllegalArgumentException When the "value" attribute of the given component is absent or does not
	 * represent a method expression referring an existing method taking at least one argument. Or, when the "type"
	 * attribute does not represent a valid file extension (you can add unrecognized ones as
	 * <code>&lt;mime-mapping&gt;</code> in <code>web.xml</code>).
	 */
	public static GraphicResource create(FacesContext context, ValueExpression value, String type, Object lastModified) {
		MethodReference methodReference = ExpressionInspector.getMethodReference(context.getELContext(), value);

		if (methodReference.getMethod() == null) {
			throw new IllegalArgumentException(String.format(ERROR_UNKNOWN_METHOD, value.getExpressionString()));
		}

		String name = getResourceName(methodReference, type);

		if (!ALLOWED_METHODS.containsKey(name)) { // No need to validate everytime when already known.
			Class<? extends Object> beanClass = methodReference.getBase().getClass();

			if (!isOneAnnotationPresent(beanClass, REQUIRED_ANNOTATION_TYPES)) {
				throw new IllegalArgumentException(String.format(ERROR_INVALID_SCOPE, beanClass));
			}

			ALLOWED_METHODS.put(name, new MethodReference(methodReference.getBase(), methodReference.getMethod()));
		}

		Object[] params = methodReference.getActualParameters();
		String[] convertedParams = convertToStrings(context, params, methodReference.getMethod().getParameterTypes());
		return new GraphicResource(name, convertedParams, lastModified);
	}

	/**
	 * An override which either returns the data URI or appends the converted method parameters to the query string.
	 */
	@Override
	public String getRequestPath() {
		if (base64 != null) {
			return "data:" + getContentType() + ";base64," + base64;
		}
		else {
			String queryString = isEmpty(params) ? "" : ("&" + toQueryString(singletonMap("p", asList(params))));
			return super.getRequestPath() + queryString;
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		MethodReference methodReference = ALLOWED_METHODS.get(getResourceName().split("\\.", 2)[0]);

		if (methodReference == null) {
			return null; // Ignore hacker attempts. I'd rather return 400 here, but JSF spec doesn't support it.
		}

		Method method = methodReference.getMethod();
		Object[] convertedParams = convertToObjects(getContext(), params, method.getParameterTypes());
		Object content;

		try {
			content = method.invoke(methodReference.getBase(), convertedParams);
		}
		catch (Exception e) {
			throw new FacesException(e);
		}

		if (content == null) {
			return null;
		}
		else if (content instanceof InputStream) {
			return (InputStream) content;
		}
		else if (content instanceof byte[]) {
			return new ByteArrayInputStream((byte[]) content);
		}
		else {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_RETURNTYPE, content));
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * This must return an unique and URL-safe identifier of the bean+method+type without any periods.
	 */
	private static String getResourceName(MethodReference methodReference, String type) {
		return methodReference.getBase().getClass().getSimpleName() + "_" + methodReference.getMethod().getName()
			+ (isEmpty(type) ? "" : ("_" + type));
	}

	/**
	 * This must extract the content type from the resource name, if any, else return the default content type.
	 */
	private static String getContentType(String resourceName) {
		String[] parts = resourceName.split("_");
		return (parts.length == RESOURCE_NAME_FULL_PARTS_LENGTH)
			? resolveContentType(parts[RESOURCE_NAME_FULL_PARTS_LENGTH - 1])
			: DEFAULT_CONTENT_TYPE;
	}

	/**
	 * Guess the image content type based on given base64 encoded content for data URI.
	 */
	private static String guessContentType(String base64) {
		for (Entry<String, String> contentTypeByBase64Header : CONTENT_TYPES_BY_BASE64_HEADER.entrySet()) {
			if (base64.startsWith(contentTypeByBase64Header.getKey())) {
				return contentTypeByBase64Header.getValue();
			}
		}

		return DEFAULT_CONTENT_TYPE;
	}

	/**
	 * Resolve image content type based on given type attribute.
	 * @throws IllegalArgumentException When given type is unrecognized.
	 */
	private static String resolveContentType(String type) {
		String contentType = Faces.getExternalContext().getMimeType("image." + type);

		if (contentType == null) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_TYPE, type));
		}

		return contentType;
	}

	/**
	 * Convert the given resource content to base64 encoded string.
	 * @throws IllegalArgumentException When given content is unrecognized.
	 */
	private static String convertToBase64(Object content) {
		byte[] bytes;

		if (content instanceof InputStream) {
			try {
				bytes = toByteArray((InputStream) content);
			}
			catch (IOException e) {
				throw new FacesException(e);
			}
		}
		else if (content instanceof byte[]) {
			bytes = (byte[]) content;
		}
		else {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_RETURNTYPE, content));
		}

		return DatatypeConverter.printBase64Binary(bytes);
	}

	/**
	 * Convert the given objects to strings using converters registered on given types.
	 * @throws IllegalArgumentException When the length of given params doesn't match those of given types.
	 */
	private static String[] convertToStrings(FacesContext context, Object[] values, Class<?>[] types) {
		validateParamLength(values, types);
		String[] strings = new String[values.length];
		Application application = context.getApplication();

		for (int i = 0; i < values.length; i++) {
			Object value = values[i];
			Converter converter = application.createConverter(types[i]);
			strings[i] = (converter != null)
				? converter.getAsString(context, DUMMY_COMPONENT, value)
				: (value != null) ? value.toString() : "";
		}

		return strings;
	}

	/**
	 * Convert the given strings to objects using converters registered on given types.
	 * @throws IllegalArgumentException When the length of given params doesn't match those of given types.
	 */
	private static Object[] convertToObjects(FacesContext context, String[] values, Class<?>[] types) {
		validateParamLength(values, types);
		Object[] objects = new Object[values.length];
		Application application = context.getApplication();

		for (int i = 0; i < values.length; i++) {
			String value = isEmpty(values[i]) ? null : values[i];
			Converter converter = application.createConverter(types[i]);
			objects[i] = (converter != null)
				? converter.getAsObject(context, DUMMY_COMPONENT, value)
				: value;
		}

		return objects;
	}

	private static void validateParamLength(Object[] params, Class<?>[] types) {
		if (params.length != types.length) {
			throw new IllegalArgumentException(String.format(ERROR_INVALID_PARAMS, Arrays.toString(params)));
		}
	}

}