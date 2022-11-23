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
package org.omnifaces.resourcehandler;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.logging.Level.FINE;
import static org.omnifaces.util.Beans.getManager;
import static org.omnifaces.util.BeansLocal.getReference;
import static org.omnifaces.util.Faces.getContext;
import static org.omnifaces.util.Faces.getExternalContext;
import static org.omnifaces.util.Servlets.toQueryString;
import static org.omnifaces.util.Utils.coalesce;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.isNumber;
import static org.omnifaces.util.Utils.isOneAnnotationPresent;
import static org.omnifaces.util.Utils.isOneInstanceOf;
import static org.omnifaces.util.Utils.isOneOf;
import static org.omnifaces.util.Utils.toByteArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.el.ValueExpression;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.xml.bind.DatatypeConverter;

import org.omnifaces.cdi.GraphicImageBean;
import org.omnifaces.el.ExpressionInspector;
import org.omnifaces.el.MethodReference;

/**
 * <p>
 * This {@link Resource} implementation is used by the {@link org.omnifaces.component.output.GraphicImage} component.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public class GraphicResource extends DynamicResource {

	// Constants ------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(GraphicResource.class.getName());

	private static final String DEFAULT_CONTENT_TYPE = "image";
	private static final Map<String, String> CONTENT_TYPES_BY_BASE64_HEADER = createContentTypesByBase64Header();
	private static final Map<String, MethodReference> ALLOWED_METHODS = new ConcurrentHashMap<>();
	private static final String[] EMPTY_PARAMS = new String[0];

	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation>[] REQUIRED_ANNOTATION_TYPES = new Class[] {
		GraphicImageBean.class,
		javax.faces.bean.ApplicationScoped.class,
		javax.enterprise.context.ApplicationScoped.class
	};

	@SuppressWarnings("unchecked")
	private static final Class<? extends Annotation>[] REQUIRED_RETURN_TYPES = new Class[] {
		InputStream.class,
		byte[].class
	};

	private static final AnnotationLiteral<Any> ANY = new AnnotationLiteral<Any>() {
		private static final long serialVersionUID = 1L;
	};

	private static final String ERROR_MISSING_METHOD =
		"@GraphicImageBean bean '%s' must have a method returning an InputStream or byte[].";
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
		"o:graphicImage 'value' attribute must refer a @GraphicImageBean or @ApplicationScoped bean."
			+ " Cannot find the right annotation on bean class '%s'.";
	private static final String ERROR_INVALID_RETURNTYPE =
		"o:graphicImage 'value' attribute must represent a method returning an InputStream or byte[]."
			+ " Encountered an invalid return value of '%s'.";
	private static final String ERROR_INVALID_PARAMS =
		"o:graphicImage 'value' attribute must specify valid method parameters."
			+ " Encountered invalid method parameters '%s'.";

	// Variables ------------------------------------------------------------------------------------------------------

	private String base64;
	private String[] params;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Construct a new graphic resource which uses the given content as data URI.
	 * This constructor is called during render time of <code>&lt;o:graphicImage ... dataURI="true"&gt;</code>.
	 * @param content The graphic resource content, to be represented as data URI.
	 * @param contentType The graphic resource content type. If this is <code>null</code>, then it will be guessed
	 * based on the content type signature in the content header. So far, WEBP, JPEG, PNG, GIF, ICO, SVG, BMP and TIFF are
	 * recognized. Else if this represents the file extension, then it will be resolved based on mime mappings.
	 */
	public GraphicResource(Object content, String contentType) {
		super("", GraphicResourceHandler.LIBRARY_NAME, contentType);
		base64 = convertToBase64(content);

		if (contentType == null) {
			setContentType(guessContentType(base64));
		}
		else if (!contentType.contains("/")) {
			setContentType(getContentType("image." + contentType));
		}
	}

	/**
	 * Construct a new graphic resource based on the given name, EL method parameters converted as string, and the
	 * "last modified" representation.
	 * This constructor is called during render time of <code>&lt;o:graphicImage value="..." dataURI="false"&gt;</code>
	 * and during handling the resource request by {@link GraphicResourceHandler}.
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
			throw new IllegalArgumentException(format(ERROR_INVALID_LASTMODIFIED, lastModified));
		}
	}

	/**
	 * Create a new graphic resource based on the given value expression.
	 * @param context The involved faces context.
	 * @param value The value expression representing content to create a new graphic resource for.
	 * @param type The image type, represented as file extension. E.g. "webp", "jpg", "png", "gif", "ico", "svg", "bmp",
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
		Method beanMethod = methodReference.getMethod();

		if (beanMethod == null) {
			throw new IllegalArgumentException(format(ERROR_UNKNOWN_METHOD, value.getExpressionString()));
		}

		Class<?> beanClass = methodReference.getBase().getClass();
		String name = getResourceBaseName(beanClass, beanMethod);

		if (!ALLOWED_METHODS.containsKey(name)) { // No need to validate everytime when already known.
			if (!isOneAnnotationPresent(beanClass, REQUIRED_ANNOTATION_TYPES)) {
				throw new IllegalArgumentException(format(ERROR_INVALID_SCOPE, beanClass));
			}

			if (!isOneOf(beanMethod.getReturnType(), REQUIRED_RETURN_TYPES)) {
				throw new IllegalArgumentException(format(ERROR_INVALID_RETURNTYPE, beanMethod.getReturnType()));
			}

			ALLOWED_METHODS.put(name, new MethodReference(methodReference.getBase(), beanMethod));
		}

		Object[] params = methodReference.getActualParameters();
		String[] convertedParams = convertToStrings(context, params, beanMethod.getParameterTypes());
		return new GraphicResource(name + (isEmpty(type) ? "" :  "." + type), convertedParams, lastModified);
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

		Method method;
		Object[] convertedParams;

		try {
			method = methodReference.getMethod();
			convertedParams = convertToObjects(getContext(), params, method.getParameterTypes());
		}
		catch (Exception ignore) {
			logger.log(FINE, "Ignoring thrown exception; this can only be a hacker attempt.", ignore);
			return null; // I'd rather return 400 here, but JSF spec doesn't support it.
		}

		Object content;

		try {
			content = method.invoke(methodReference.getBase(), convertedParams);
		}
		catch (Exception e) {
			throw new FacesException(e);
		}

		if (content instanceof InputStream) {
			return (InputStream) content;
		}
		else if (content instanceof byte[]) {
			return new ByteArrayInputStream((byte[]) content);
		}
		else {
			return null;
		}
	}

	// Helpers --------------------------------------------------------------------------------------------------------

	/**
	 * Create mapping of content types by base64 header.
	 */
	private static Map<String, String> createContentTypesByBase64Header() {
		Map<String, String> contentTypesByBase64Header = new HashMap<>();
		contentTypesByBase64Header.put("UklGR", "image/webp");
		contentTypesByBase64Header.put("/9j/", "image/jpeg");
		contentTypesByBase64Header.put("iVBORw", "image/png");
		contentTypesByBase64Header.put("R0lGOD", "image/gif");
		contentTypesByBase64Header.put("AAABAA", "image/x-icon");
		contentTypesByBase64Header.put("PD94bW", "image/svg+xml");
		contentTypesByBase64Header.put("Qk0", "image/bmp");
		contentTypesByBase64Header.put("SUkqAA", "image/tiff");
		contentTypesByBase64Header.put("TU0AKg", "image/tiff");
		return unmodifiableMap(contentTypesByBase64Header);
	}

	/**
	 * Register graphic image scoped beans discovered so far.
	 * @throws IllegalArgumentException When bean method is missing.
	 */
	public static void registerGraphicImageBeans() {
		BeanManager beanManager = getManager();

		for (Bean<?> bean : beanManager.getBeans(Object.class, ANY)) {
			Class<?> beanClass = bean.getBeanClass();

			if (!isOneAnnotationPresent(beanClass, GraphicImageBean.class)) {
				continue;
			}

			Object instance = getReference(beanManager, bean);
			boolean registered = false;

			for (Method method : beanClass.getMethods()) {
				if (isPublic(method.getModifiers()) && isOneInstanceOf(method.getReturnType(), REQUIRED_RETURN_TYPES)) {
					String resourceBaseName = getResourceBaseName(beanClass, method);
					MethodReference methodReference = new MethodReference(instance, method);
					ALLOWED_METHODS.put(resourceBaseName, methodReference);
					registered = true;
				}
			}

			if (!registered) {
				throw new IllegalArgumentException(format(ERROR_MISSING_METHOD, beanClass.getName()));
			}
		}
	}

	/**
	 * This must return an unique and URL-safe identifier of the bean+method without any periods.
	 */
	private static String getResourceBaseName(Class<?> beanClass, Method beanMethod) {
		return beanClass.getSimpleName().replaceAll("\\W", "") + "_" + beanMethod.getName();
	}

	/**
	 * This must extract the content type from the resource name, if any, else return the default content type.
	 * @throws IllegalArgumentException When given type is unrecognized.
	 */
	private static String getContentType(String resourceName) {
		if (!resourceName.contains(".")) {
			return DEFAULT_CONTENT_TYPE;
		}

		String contentType = getExternalContext().getMimeType(resourceName);

		if (contentType == null) {
			throw new IllegalArgumentException(format(ERROR_INVALID_TYPE, resourceName.split("\\.", 2)[1]));
		}

		return contentType;
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
			throw new IllegalArgumentException(format(ERROR_INVALID_RETURNTYPE, content));
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
		UIComponent dummyComponent = new UIOutput();

		for (int i = 0; i < values.length; i++) {
			Object value = values[i];
			Converter converter = application.createConverter(types[i]);
			strings[i] = (converter != null)
				? converter.getAsString(context, dummyComponent, value)
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
		UIComponent dummyComponent = new UIOutput();

		for (int i = 0; i < values.length; i++) {
			String value = isEmpty(values[i]) ? null : values[i];
			Converter converter = application.createConverter(types[i]);
			objects[i] = (converter != null)
				? converter.getAsObject(context, dummyComponent, value)
				: value;
		}

		return objects;
	}

	private static void validateParamLength(Object[] params, Class<?>[] types) {
		if (params.length != types.length) {
			throw new IllegalArgumentException(format(ERROR_INVALID_PARAMS, Arrays.toString(params)));
		}
	}

}