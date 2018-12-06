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
package org.omnifaces.taghandler;

import static org.omnifaces.util.Components.createValueExpression;

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
 * which does not properly happen in Mojarra.
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
 * then <code>#{id}</code> of the nested tag would evaluate to <code>"foo"</code> instead of <code>null</code>, even when you explicitly
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
 * Since OmniFaces 2.7/3.2 there is a special case for a <code>&lt;o:tagAttribute name="id"&gt;</code> without
 * a default value: it will autogenerate an unique ID in the form of <code>j_ido[tagId]</code> where <code>[tagId]</code>
 * is the <code>&lt;o:tagAttribute&gt;</code> tag's own unique ID.
 *
 * @author Arjan Tijms.
 * @since 2.1
 */
public class TagAttribute extends TagHandler {

	private final String name;
	private final javax.faces.view.facelets.TagAttribute defaultValue;

	public TagAttribute(TagConfig config) {
		super(config);
		name = getRequiredAttribute("name").getValue();
		defaultValue = getAttribute("default");
	}

	@Override
	public void apply(FaceletContext context, UIComponent parent) throws IOException {
		DelegatingVariableMapper variableMapper = getDelegatingVariableMapper(context);
		ValueExpression valueExpression = variableMapper.resolveWrappedVariable(name);

		if (valueExpression == null) {
			if (defaultValue != null) {
				valueExpression = defaultValue.getValueExpression(context, Object.class);
			}
			else if ("id".equals(name)) {
				valueExpression = createValueExpression("#{'j_ido" + context.generateUniqueId(this.tagId) + "'}", String.class);
			}
		}

		variableMapper.setVariable(name, valueExpression);
	}

	private DelegatingVariableMapper getDelegatingVariableMapper(FaceletContext context) {
		VariableMapper variableMapper = context.getVariableMapper();

		if (variableMapper instanceof DelegatingVariableMapper) {
			return (DelegatingVariableMapper) variableMapper;
		}

		DelegatingVariableMapper delegatingVariableMapper = new DelegatingVariableMapper(variableMapper);
		context.setVariableMapper(delegatingVariableMapper);
		return delegatingVariableMapper;
	}

}