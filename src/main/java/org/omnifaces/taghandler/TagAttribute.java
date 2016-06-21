/*
 * Copyright 2015 OmniFaces.
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
package org.omnifaces.taghandler;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.faces.component.UIComponent;
import javax.faces.view.facelets.FaceletContext;
import javax.faces.view.facelets.TagConfig;
import javax.faces.view.facelets.TagHandler;

import org.omnifaces.el.DelegatingVariableMapper;

/**
 * <p>
 * The <code>&lt;o:tagAttribute&gt;</code> is a tag handler that can be used to explicitly declare a tag attribute on
 * a Facelets tag file. This makes sure that any tag attribute with the same name on a parent tag file is cleared out,
 * which does not properly happen in Mojarra. This tag handler is designed to be used only in Mojarra and does not work
 * in MyFaces as it has already internally solved this problem.
 * <p>
 * Consider the following custom tag structure:
 * <pre>
 * &lt;my:tag id="foo"&gt;
 *     &lt;my:tag id="bar" /&gt;
 * &lt;/my:tag&gt;
 * </pre>
 * <p>
 * Inside the nested tag, the <code>#{id}</code> will just evaluate to <code>"bar"</code>. However, if this isn't
 * declared on the nested tag like so,
 * <pre>
 * &lt;my:tag id="foo"&gt;
 *     &lt;my:tag /&gt;
 * &lt;/my:tag&gt;
 * </pre>
 * <p>
 * then <code>#{id}</code> would evaluate to <code>"foo"</code> instead of <code>null</code>, even when you explicitly
 * specify the attribute in the <code>*.taglib.xml</code> file.
 * <p>
 * This tag handler is designed to overcome this peculiar problem and unintuitive behavior of nested tagfiles in
 * Mojarra.
 *
 * <h3>Usage</h3>
 * <p>
 * Just declare the attribute name in top of the tagfile as below.
 * <pre>
 * &lt;o:tagAttribute name="id" /&gt;
 * </pre>
 * <p>
 * You can optionally provide a default value.
 * <pre>
 * &lt;o:tagAttribute name="type" default="text" /&gt;
 * </pre>
 *
 * <h3>MyFaces</h3>
 * <p>
 * MyFaces has already internally solved this problem. Using <code>&lt;o:tagAttribute&gt;</code> in MyFaces will break
 * tag attributes. Do not use it in MyFaces. In case you intend to solely have a default value for a tag attribute,
 * then continue using JSTL for that.
 * <pre>
 * &lt;c:set var="type" value="#{empty type ? 'text' : type}" /&gt;
 * </pre>
 *
 * @author Arjan Tijms.
 * @since 2.1
 * @see DelegatingVariableMapper
 */
public class TagAttribute extends TagHandler {

	private static final String MARKER = TagAttribute.class.getName();

	private final String name;
	private final javax.faces.view.facelets.TagAttribute defaultValue;

	public TagAttribute(TagConfig config) {
		super(config);
		name = getRequiredAttribute("name").getValue();
		defaultValue = getAttribute("default");
	}

	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		checkAndMarkMapper(context);
		VariableMapper variableMapper = context.getVariableMapper();
		ValueExpression valueExpressionLocal = variableMapper.setVariable(name, null);

		if (valueExpressionLocal == null && defaultValue != null) {
			valueExpressionLocal = defaultValue.getValueExpression(context, Object.class);
		}

		if (valueExpressionLocal == null) {
			ValueExpression valueExpressionParent = variableMapper.resolveVariable(name);

			if (valueExpressionParent != null) {
				valueExpressionLocal = context.getExpressionFactory().createValueExpression(null, Object.class);
			}
		}

		variableMapper.setVariable(name, valueExpressionLocal);
	}

	private static void checkAndMarkMapper(FaceletContext context) {
		Integer marker = (Integer) context.getAttribute(MARKER);

		if (marker != null && marker.equals(context.hashCode())) {
			return; // Already marked.
		}

		VariableMapper variableMapper = context.getVariableMapper();
		ValueExpression valueExpressionParentMarker = variableMapper.resolveVariable(MARKER);

		if (valueExpressionParentMarker == null) { // We're the outer faces tag, or parent didn't mark because it didn't have any attributes set.
			context.setAttribute(MARKER, context.hashCode());
			return;
		}

		variableMapper.setVariable(MARKER, null); // If we have our own mapper, this will not affect our parent mapper.
		ValueExpression valueExpressionParentMarkerCheck = variableMapper.resolveVariable(MARKER);

		if (valueExpressionParentMarkerCheck == null || !valueExpressionParentMarkerCheck.equals(valueExpressionParentMarker)) {
			// We were able to remove our parent's mapper, so we share it.

			variableMapper.setVariable(MARKER, valueExpressionParentMarker); // First put parent marker back ...
			context.setVariableMapper(new DelegatingVariableMapper(variableMapper)); // ... then add our own variable mapper.
		}

		context.setAttribute(MARKER, context.hashCode());
	}

}