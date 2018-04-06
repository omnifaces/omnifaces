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
package org.omnifaces.component.search;

import static java.lang.String.join;
import static org.omnifaces.util.Utils.isOneInstanceOf;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIMessage;
import javax.faces.component.UIMessages;
import javax.faces.component.UIViewRoot;
import javax.faces.component.search.SearchExpressionContext;
import javax.faces.component.search.SearchKeywordContext;
import javax.faces.component.search.SearchKeywordResolver;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;

import org.omnifaces.ApplicationProcessor;
import org.omnifaces.util.Components;

/**
 * <p>
 * The <strong><code>{@literal @}messages</code></strong> search keyword resolver will automatically resolve all {@link UIMessage} and
 * {@link UIMessages} components within the current {@link UIForm}. This is particularly useful when you have a relatively large form and
 * would like to Ajax-update only the message components when submitting the form.
 * <pre>
 * &lt;h:form id="form"&gt;
 *     &lt;h:inputText id="input1" ... /&gt;
 *     &lt;h:message id="m_input1" for="input1" /&gt;
 *     &lt;h:inputText id="input2" ... /&gt;
 *     &lt;h:message id="m_input2" for="input2" /&gt;
 *     &lt;h:inputText id="input3" ... /&gt;
 *     &lt;h:message id="m_input3" for="input3" /&gt;
 *     ...
 *     &lt;h:commandButton ...&gt;
 *         &lt;f:ajax execute="@form" render="@messages" /&gt;
 *     &lt;/h:commandButton&gt;
 *     &lt;h:messages id="m_form" globalOnly="true" redisplay="false" /&gt;
 * &lt;/h:form&gt;
 * </pre>
 * <p>
 * This has only one prerequirement: the message component must have a fixed <code>id</code> attribute set as demonstrated above. Otherwise
 * JSF won't render anything to the client side when there are no messages and ultimately JavaScript won't be able to find it when
 * processing the JSF Ajax response.
 * <p>
 * This keyword resolver is already registered by OmniFaces own <code>faces-config.xml</code> and thus gets auto-initialized when the
 * OmniFaces JAR is bundled in a web application, so end-users do not need to register this keyword resolver explicitly themselves.
 *
 * @author Bauke Scholtz
 * @since 3.1
 */
public class MessagesKeywordResolver extends SearchKeywordResolver {

	// Constants --------------------------------------------------------------------------------------------------------------------------

	private static final Logger logger = Logger.getLogger(MessagesKeywordResolver.class.getName());

	// Init -------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Invoked by {@link ApplicationProcessor}.
	 * @param application Involved faces application.
	 */
	public static void register(Application application) {
		application.addSearchKeywordResolver(new MessagesKeywordResolver());
	}

	// Actions ----------------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns <code>true</code> when keyword equals "messages".
	 */
	@Override
	public boolean isResolverForKeyword(SearchExpressionContext context, String keyword) {
		return "messages".equals(keyword);
	}

	/**
	 * Grab the current {@link UIForm}, visit it and collect client IDs of all {@link UIMessage} and {@link UIMessages} components and
	 * finally invoke context call back with an {@link UIMessage} component whose client ID returns a space separated collection of found
	 * client IDs.
	 */
	@Override
	public void resolve(SearchKeywordContext context, UIComponent component, String keyword) {
		UIForm form = Components.getClosestParent(component, UIForm.class);

		if (form != null) {
			Set<String> messageClientIds = new HashSet<>();
			VisitContext visitContext = VisitContext.createVisitContext(context.getSearchExpressionContext().getFacesContext());

			form.visitTree(visitContext, (visit, child) -> {
				if (isOneInstanceOf(child.getClass(), UIMessage.class, UIMessages.class)) {
					if (child.getId().startsWith(UIViewRoot.UNIQUE_ID_PREFIX)) {
						logger.warning(String.format("@messages can only target message components with a fixed ID; auto generated ID %s encountered", child.getId()));
					}
					else {
						messageClientIds.add(child.getClientId());
					}
				}
				return VisitResult.ACCEPT;
			});

			if (!messageClientIds.isEmpty()) {
				context.invokeContextCallback(new UIMessage() {
					@Override
					public String getClientId(FacesContext context) {
						return join(" ", messageClientIds);
					}
				});
			}
		}

		context.setKeywordResolved(true);
	}
}
