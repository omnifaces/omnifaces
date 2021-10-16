/*
 * Copyright 2021 OmniFaces
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
package org.omnifaces.test.el;

import static org.omnifaces.util.Components.createValueExpression;
import static org.omnifaces.util.Faces.getELContext;

import javax.annotation.PostConstruct;
import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.omnifaces.el.ExpressionInspector;
import org.omnifaces.el.MethodReference;
import org.omnifaces.util.Components;

@Named
@RequestScoped
public class ExpressionInspectorITBean {

	private ValueReference valueReference;
	private MethodReference getterReference;
	private MethodReference methodReference;

	@PostConstruct
	public void init() {
		ELContext elContext = getELContext();

		ValueExpression valueExpression = createValueExpression("#{bar.selected}", Baz.class);
		valueReference = ExpressionInspector.getValueReference(elContext, valueExpression);
		getterReference = ExpressionInspector.getMethodReference(elContext, valueExpression);

		MethodExpression methodExpression = Components.createMethodExpression("#{foo.create(bar.selected)}", Void.class, Baz.class);
		methodReference = ExpressionInspector.getMethodReference(elContext, methodExpression);
	}

	public ValueReference getValueReference() {
		return valueReference;
	}

	public MethodReference getGetterReference() {
		return getterReference;
	}

	public MethodReference getMethodReference() {
		return methodReference;
	}

	@Named
	@RequestScoped
	public static class Foo {
		public void create(Baz baz) {
			//
		}
	}

	@Named
	@RequestScoped
	public static class Bar {
		public Baz getSelected() {
			return new Baz();
		}
	}

	public static class Baz {
		//
	}
}
