package org.omnifaces.cdi.component;

import static org.omnifaces.util.BeansLocal.*;
import java.util.*;
import javax.el.ValueExpression;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.*;
import javax.faces.application.*;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

@ApplicationScoped
public class ComponentManager {

	@Inject
	private BeanManager manager;
	private Map<String, Bean<UIComponent>> componentsByType = new HashMap<>();

	@SuppressWarnings("unchecked")
	public UIComponent createComponent(Application application, FacesContext context, String componentType, String rendererType) {
		Bean<UIComponent> bean = componentsByType.get(componentType);

		if (bean == null && !componentsByType.containsKey(componentType)) {
			UIComponent component = application.createComponent(context, componentType, rendererType);

			if (component != null) {
				bean = (Bean<UIComponent>) resolve(manager, component.getClass());
			}

			componentsByType.put(componentType, bean);
		}

		return bean != null ? getReference(manager, bean) : null;
	}

	@SuppressWarnings("unchecked")
	public UIComponent createComponent(Application application, String componentType) {
		Bean<UIComponent> bean = componentsByType.get(componentType);

		if (bean == null && !componentsByType.containsKey(componentType)) {
			UIComponent component = application.createComponent(componentType);

			if (component != null) {
				bean = (Bean<UIComponent>) resolve(manager, component.getClass());
			}

			componentsByType.put(componentType, bean);
		}

		return bean != null ? getReference(manager, bean) : null;
	}

	@SuppressWarnings("unchecked")
	public UIComponent createComponent(Application application, ValueExpression componentExpression, FacesContext context, String componentType, String rendererType) {
		Bean<UIComponent> bean = componentsByType.get(componentType);

		if (bean == null && !componentsByType.containsKey(componentType)) {
			UIComponent component = application.createComponent(componentExpression, context, componentType, rendererType);

			if (component != null) {
				bean = (Bean<UIComponent>) resolve(manager, component.getClass());
			}

			componentsByType.put(componentType, bean);
		}

		return bean != null ? getReference(manager, bean) : null;
	}

	@SuppressWarnings("unchecked")
	public UIComponent createComponent(Application application, ValueExpression componentExpression, FacesContext context, String componentType) {
		Bean<UIComponent> bean = componentsByType.get(componentType);

		if (bean == null && !componentsByType.containsKey(componentType)) {
			UIComponent component = application.createComponent(componentExpression, context, componentType);

			if (component != null) {
				bean = (Bean<UIComponent>) resolve(manager, component.getClass());
			}

			componentsByType.put(componentType, bean);
		}

		return bean != null ? getReference(manager, bean) : null;
	}

}
