/*
 * Copyright 2019 OmniFaces
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
package org.omnifaces.cdi.jsparam;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlBody;
import javax.faces.component.html.HtmlCommandScript;
import javax.faces.component.html.HtmlForm;
import javax.faces.component.html.HtmlInputHidden;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.omnifaces.cdi.JSParam;
import org.omnifaces.util.Components;
import org.omnifaces.util.Faces;

/**
 * CDI extension that implements injection of client-side evaluated JavaScript results. The implementation is stateless,
 * completely relying on the model provided and stored in the JSF component tree.
 * <p>
 * When initialized, the extension first checks if the body of the current view root contains a form suitable for
 * passing the values collected from the specified javascript expressions. If a suitable {@link HtmlForm} form does not
 * exist, one is created together with a {@link HtmlCommandScript}. The command script is also connected to a action
 * listener that actually handles the injection logic - being called as soon as the page is loaded.
 * Once done, the bean instance holding the injection points is checked for all<code>&#64;</code>{@link JSParam}
 * properties defined in the class. Each property receives the JavaScript and hidden input fields needed to send the
 * result of the evaluated expressions back to the CDI managed bean.
 * 
 * @since 3.3
 * @author Adam Waldenberg, Ejwa Software/Hosting
 */
public class JSParamExtension implements Extension {
		private static final int GENERATED_ID_NUM_CHARACTERS = 8;
	private static final String JSPARAM_FORM_NAME = JSParam.class.getName().toLowerCase().replace('.', '-');
	private static final String ASSIGNMENT_TEMPLATE = "document.getElementById('%s:%s').value = %s;";
	private static final String STRINGIFY_TEMPLATE = "JSON.stringify(clone(%s))";
	private static final String CLONE_FUNCTION = "function clone(n){if(null===n||!(n instanceof Object))return n;"
	                                             + "var t={};for(var c in n)n[c]instanceof Function||n[c]"
	                                             + "instanceof Object||(t[c]=n[c]);return t}";

	private HtmlBody getDocumentBody() {
		for (UIComponent c : Faces.getViewRoot().getChildren()) {
			if (c instanceof HtmlBody) {
				return (HtmlBody) c;
			}
		}

		throw new IllegalStateException(String.format("The docoument %s does not contain a body",
		                                              Faces.getContext().getViewRoot()));
	}

	private <T> void initializePageForm(T instance, Field field) {
		if (Components.findComponent(JSPARAM_FORM_NAME) == null) {
			final HtmlForm form = new HtmlForm();
			final HtmlCommandScript commandScript = new HtmlCommandScript();

			commandScript.addActionListener(new JSParamActionListener());
			commandScript.setName(RandomStringUtils.randomAlphabetic(GENERATED_ID_NUM_CHARACTERS));
			commandScript.setAutorun(true);
			form.setStyle("display: none;");
			form.setId(JSPARAM_FORM_NAME);
			form.getChildren().add(commandScript);
			getDocumentBody().getChildren().add(form);
		}
	}

	private <T> void addHiddenComponent(T instance, AnnotatedField field) {
		final HtmlForm form = (HtmlForm) Components.findComponent(JSPARAM_FORM_NAME);
		final HtmlInputHidden input = new HtmlInputHidden();
		input.setId(field.getJavaMember().getName());
		input.getAttributes().put("field", field);
		form.getAttributes().put("instance", instance);
		form.getChildren().add(input);
	}

	private <T> void addScript(T instance, Field field) {
		final Class<?> beanClass = field.getDeclaringClass();
		final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(beanClass, JSParam.class);
		final Field lastField = IteratorUtils.get(fields.iterator(), fields.size() - 1);

		if (field.equals(lastField)) {
			final List<String> scripts = new ArrayList<>();
			final MutableBoolean containsComplexType = new MutableBoolean(false);

			fields.forEach((Field f) -> {
				final String value = f.getAnnotation(JSParam.class).value();
				final String javascript;

				if (f.getType() != Integer.class && f.getType() != int.class
					&& f.getType() != String.class) {
					containsComplexType.setTrue();
					javascript = String.format(STRINGIFY_TEMPLATE, value);
				} else {
					javascript = value;
				}

				scripts.add(String.format(ASSIGNMENT_TEMPLATE,
					JSPARAM_FORM_NAME, f.getName(), javascript)
				);
			});

			if (containsComplexType.isTrue()) {
				scripts.add(CLONE_FUNCTION);
			}

			Components.addScriptToBody(StringUtils.join(scripts, "\n"));
		}
	}

	public <T> void initializePropertyLoading(final @Observes ProcessInjectionTarget<T> pit) {
		pit.getAnnotatedType().getFields().forEach(f -> {
			final InjectionTarget<T> it = pit.getInjectionTarget();

			if (f.getAnnotation(JSParam.class) != null) {
				pit.setInjectionTarget(new InjectionTarget<T>() {
					@Override
					public void inject(T instance, CreationalContext<T> ctx) {
						it.inject(instance, ctx);

						initializePageForm(instance, f.getJavaMember());
						addHiddenComponent(instance, f);
						addScript(instance, f.getJavaMember());
					}

					@Override
					public void postConstruct(T instance) {
						it.postConstruct(instance);
					}

					@Override
					public void preDestroy(T instance) {
						it.dispose(instance);
					}

					@Override
					public void dispose(T instance) {
						it.dispose(instance);
					}

					@Override
					public Set<InjectionPoint> getInjectionPoints() {
						return it.getInjectionPoints();
					}

					@Override
					public T produce(CreationalContext<T> ctx) {
						return it.produce(ctx);
					}
				});
			}
		});
	}
}
