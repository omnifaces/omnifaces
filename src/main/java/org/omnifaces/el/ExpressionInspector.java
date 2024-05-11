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
package org.omnifaces.el;

import static java.beans.Introspector.decapitalize;
import static org.omnifaces.el.MethodReference.NO_PARAMS;
import static org.omnifaces.el.functions.Strings.capitalize;
import static org.omnifaces.util.Beans.unwrapIfNecessary;
import static org.omnifaces.util.Components.createValueExpression;
import static org.omnifaces.util.Reflection.findMethod;

import java.lang.reflect.Method;
import java.util.Objects;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.MethodExpression;
import jakarta.el.MethodNotFoundException;
import jakarta.el.ValueExpression;
import jakarta.el.ValueReference;
import jakarta.faces.el.CompositeComponentExpressionHolder;

/**
 * <p>
 * This class contains methods that inspect expressions to reveal information about them.
 *
 * <h2>Examples</h2>
 * <p>
 * Determine the bean instance and the property value behind a {@link ValueExpression}.
 * <pre>
 * ValueExpression valueExpression = component.getValueExpression("value");
 * ValueReference valueReference = ExpressionInspector.getValueReference(context.getELContext(), valueExpression);
 * Object bean = methodReference.getBase();
 * Object property = methodReference.getProperty();
 * </pre>
 * <p>
 * Determine the bean instance and the concrete getter {@link Method} behind a {@link ValueExpression}.
 * <pre>
 * ValueExpression valueExpression = component.getValueExpression("value");
 * MethodReference methodReference = ExpressionInspector.getMethodReference(context.getELContext(), valueExpression);
 * Object bean = methodReference.getBase();
 * Method method = methodReference.getMethod();
 * </pre>
 * <p>
 * Determine the bean instance and the concrete action {@link Method} behind a {@link MethodExpression}.
 * <pre>
 * MethodExpression methodExpression = commandComponent.getActionExpression();
 * MethodReference methodReference = ExpressionInspector.getMethodReference(context.getELContext(), methodExpression);
 * Object bean = methodReference.getBase();
 * Method method = methodReference.getMethod();
 * </pre>
 *
 * @author Arjan Tijms
 * @since 1.4
 */
public final class ExpressionInspector {

    private ExpressionInspector() {
        // Hide constructor.
    }

    /**
     * Gets the ValueReference from a ValueExpression, without any checks whether the property is actually
     * a property or if it isn't a "MethodSuffix". The property is stored as it appears in the expression,
     * and may thus not actually exists. It's up to the caller how to interpret this.
     *
     * @param context the context of this evaluation
     * @param valueExpression the value expression being evaluated
     * @return a ValueReference holding the final base and property where the value expression evaluated to.
     */
    public static ValueReference getValueReference(ELContext context, ValueExpression valueExpression) {

        InspectorElContext inspectorElContext = new InspectorElContext(context);
        Class<?> type = valueExpression.getType(inspectorElContext);

        if (type != null && MethodExpression.class.isAssignableFrom(type)) {
            MethodReference methodReference = getMethodReference(inspectorElContext, valueExpression);
            Object base = methodReference.getBase();
            String property = decapitalize(methodReference.getName().replaceFirst("(get|is)", ""));
            return new ValueReference(base, property);
        }

        inspectorElContext.setPass(InspectorPass.PASS2_FIND_FINAL_NODE);
        valueExpression.getValue(inspectorElContext);

        Object base = unwrapIfNecessary(inspectorElContext.getBase());
        Object property = inspectorElContext.getProperty();

        if (base instanceof CompositeComponentExpressionHolder) {
            return getValueReference(context, ((CompositeComponentExpressionHolder) base).getExpression(property.toString()));
        }

        return new ValueReference(base, property);
    }

    /**
     * Gets a MethodReference from a ValueExpression. If the ValueExpression refers to a method, this will
     * contain the actual method. If it refers to a property, this will contain the corresponding getter method.
     * <p>
     * Note that in case the expression refers to a method, the method reference contains the method with
     * the name the expression refers to, with a matching number of arguments and <i>a</i> match of types.
     * Overloads with the same amount of parameters are supported, but if the actual arguments match with
     * the types of multiple overloads (e.g. actual argument Long, overloads for Number and Long) a random
     * method will be chosen.
     *
     * @param context the context of this evaluation
     * @param valueExpression the value expression being evaluated
     * @return a MethodReference holding the final base and Method where the value expression evaluated to.
     */
    public static MethodReference getMethodReference(ELContext context, ValueExpression valueExpression) {
        InspectorElContext inspectorElContext = new InspectorElContext(context);

        // Invoke getType() on the value expression to have the expression chain resolved.
        // The InspectorElContext contains a special resolver that will record the next to last
        // base and property. The EL implementation may shortcut the chain when it
        // discovers the final target was a method. E.g. a.b.c().d.f('1')
        // In that case too, the result will be that the inspectorElContext contains the
        // one but last base and property, and the length of the expression chain.
        Class<?> type = valueExpression.getType(inspectorElContext);

        // If the type happens to be a value expression wrapping a method expression, then extract it from there instead.
        if (type != null && MethodExpression.class.isAssignableFrom(type)) {
            return getMethodReference(inspectorElContext, (MethodExpression) valueExpression.getValue(inspectorElContext));
        }

        // Else if everything went well, we thus have the length of the chain now.
        // Indicate that we're now at pass 2 and want to resolve the entire chain.
        // We can then capture the last element (the special resolver makes sure that
        // we don't actually invoke that last element)
        inspectorElContext.setPass(InspectorPass.PASS2_FIND_FINAL_NODE);

        // Calling getValue() will cause getValue() to be called on the resolver in case the
        // value expresses referred to a property, and invoke() when it's a method.
        ValueExpressionType valueExpressionType = (ValueExpressionType) valueExpression.getValue(inspectorElContext);

        Object base = unwrapIfNecessary(inspectorElContext.getBase());
        String property = inspectorElContext.getProperty().toString();
        boolean fromMethod = (valueExpressionType == ValueExpressionType.METHOD);
        Object[] params = fromMethod ? inspectorElContext.getParams() : NO_PARAMS;
        String methodName = fromMethod ? property : ("get" + capitalize(property));
        Method method = findMethod(base, methodName, params);

        if (method == null && !fromMethod) {
            method = findMethod(base, "is" + capitalize(property), params);

            if (method == null) {
                method = findMethod(base, property, NO_PARAMS);

                if (method != null) {
                    fromMethod = true; // From MethodExpressionValueExpressionAdapter.
                }
            }
        }

        if (method == null) {
            throw new MethodNotFoundException(base + "." + methodName + " " + valueExpression);
        }

        return new MethodReference(base, method, params, fromMethod);
    }

    /**
     * Gets a MethodReference from a MethodExpression.
     *
     * @param context the context of this evaluation
     * @param methodExpression the method expression being evaluated
     * @return a MethodReference holding the final base and Method where the method expression evaluated to.
     * @since 2.5
     */
    public static MethodReference getMethodReference(ELContext context, MethodExpression methodExpression) {

        // Historical note: MethodExpression#getMethodInfo() was previously used as 1st attempt to obtain method reference.
        // However as per #646 it turns out to be unreliable.
        // Hence MethodExpressionValueExpressionAdapter, which was previously used as fallback, is form now on used as permanent approach.

        MethodExpressionValueExpressionAdapter adapter = null;

        if (methodExpression instanceof MethodExpressionValueExpressionAdapter) {
            adapter = (MethodExpressionValueExpressionAdapter) methodExpression;
        }
        else {
            adapter = new MethodExpressionValueExpressionAdapter(createValueExpression(methodExpression.getExpressionString(), Object.class));
        }

        return (MethodReference) adapter.getMethodInfo(context);
    }

    /**
     * Types of a value expression.
     */
    private enum ValueExpressionType {
        /** Value expression that refers to a method, e.g. <code>#{foo.bar(1)}</code>. */
        METHOD,

        /** Value expression that refers to a property, e.g. <code>#{foo.bar}</code>. */
        PROPERTY
    }

    /**
     * Due to the nature of how the EL Resolver and EL 3.0 ValueExpressions work, the final
     * node of a resolved expression chain has to be found in two passes.
     *
     * <p>
     * In pass 1 the caller has to call {@link ValueExpression#getType(ELContext)} on the ValueExpression
     * in question. The EL Resolver will then be able to find the next to last node without risk of actually
     * invoking the final node (which is the node most likely to have an unwanted side-effect when from
     * the user's point of view called at random).
     *
     * <p>
     * In pass 2 the caller has to call {@link ValueExpression#getValue(ELContext)} on the ValueExpression
     * in question. Using data obtained in pass 1, the EL Resolver will be able to find the final node again
     * without needing to actually invoke it. With the final node found, the EL resolver can capture the
     * base and property in case the final node represented a property, or the base, method and the actual
     * arguments for said method in case the final repesented a method.
     */
    private enum InspectorPass {
        PASS1_FIND_NEXT_TO_LAST_NODE,
        PASS2_FIND_FINAL_NODE
    }

    /**
     * Custom ELContext implementation that wraps a given ELContext to be able to provide a custom ElResolver.
     */
    static class InspectorElContext extends ELContextWrapper {

        private final InspectorElResolver inspectorElResolver;

        public InspectorElContext(ELContext elContext) {
            super(elContext);
            inspectorElResolver = new InspectorElResolver(elContext.getELResolver());
        }

        @Override
        public ELResolver getELResolver() {
            return inspectorElResolver;
        }

		@Override
		@SuppressWarnings("unchecked")
        public <T> T convertToType(Object value, Class<T> targetType) {
        	return (T) value;
        }

        public InspectorPass getPass() {
            return inspectorElResolver.getPass();
        }

        public void setPass(InspectorPass pass) {
            inspectorElResolver.setPass(pass);
        }

        public Object getBase() {
            return inspectorElResolver.getBase();
        }

        public Object getProperty() {
            return inspectorElResolver.getProperty();
        }

        public Object[] getParams() {
            return inspectorElResolver.getParams();
        }

        public Object getOutcome() {
            return inspectorElResolver.getOutcome();
        }

    }

    static class FinalBaseHolder {
        private Object base;

        public FinalBaseHolder(Object base) {
            this.base = base;
        }

        public Object getBase() {
            return base;
        }
    }

    /**
     * Custom EL Resolver that can be used for inspecting expressions by means of recording the calls
     * made on this resolved by the EL implementation.
     */
    static class InspectorElResolver extends ELResolverWrapper {

        private int passOneCallCount;
        private int passTwoCallCount;

        private Object lastBase;
        private Object lastProperty; // Method name in case VE referenced a method, otherwise property name
        private Object[] lastParams; // Actual parameters supplied to a method (if any)
        private Object lastOutcome;

        private boolean subchainResolving;

        // Marker holder via which we can track our last base. This should become
        // the last base in a next iteration. This is needed because if the very last property is a
        // method node with a variable, we can't track resolving that variable anymore since it will not have been processed by the
        // getType() call of the first pass.
        // E.g. a.b.c(var.foo())
        private FinalBaseHolder finalBaseHolder;

        private InspectorPass pass = InspectorPass.PASS1_FIND_NEXT_TO_LAST_NODE;

        public InspectorElResolver(ELResolver elResolver) {
            super(elResolver);
        }

        @Override
        public Object getValue(ELContext context, Object base, Object property) {

            if (base instanceof FinalBaseHolder || property instanceof FinalBaseHolder) {
                // If we get called with a FinalBaseHolder, which was set in the next to last node,
                // we know we're done and can set the base and property as the final ones.
                // A property can also be a FinalBaseHolder when it is a dynamic property (brace notation).
                lastBase = (base instanceof FinalBaseHolder) ? ((FinalBaseHolder) base).getBase() : base;
                lastProperty = (property instanceof FinalBaseHolder) ? ((FinalBaseHolder) property).getBase() : property;

                context.setPropertyResolved(true);
                return ValueExpressionType.PROPERTY;
            }

            checkSubchainStarted(base);
            lastOutcome = super.getValue(context, base, property);

            if (subchainResolving) {
                return lastOutcome;
            }

            recordCall(base, property);
            return wrapOutcomeIfNeeded(lastOutcome);
        }

        @Override
        public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {

            if (base instanceof FinalBaseHolder) {
                // If we get called with a FinalBaseHolder, which was set in the next to last node,
                // we know we're done and can set the base, method and params as the final ones.
                lastBase = ((FinalBaseHolder) base).getBase();
                lastProperty = method;
                lastParams = params;

                context.setPropertyResolved(true);
                return ValueExpressionType.METHOD;
            }

            checkSubchainStarted(base);
            lastOutcome = super.invoke(context, base, method, paramTypes, params);

            if (subchainResolving) {
                return lastOutcome;
            }

            recordCall(base, method);
            return wrapOutcomeIfNeeded(lastOutcome);
        }

        @Override
        public Class<?> getType(ELContext context, Object base, Object property) {

            // getType is only called on the last element in the chain (if the EL
            // implementation actually calls this, which might not be the case if the
            // value expression references a method)
            //
            // We thus do know the size of the chain now, and the "lastBase" and "lastProperty"
            // that were set *before* this call are the next to last now.
            //
            // Alternatively, this method is NOT called by the EL implementation, but then
            // "lastBase" and "lastProperty" are still the next to last.
            //
            // Independent of what the EL implementation does, "passOneCallCount" should thus represent
            // the total size of the call chain minus 1. We use this in pass two to capture the
            // final base, property/method and optionally parameters.

            context.setPropertyResolved(true);

            // Special value to signal that getType() has actually been called (this value is
            // not used by the algorithm now, but may be useful when debugging)
            return InspectorElContext.class;
        }

        private boolean isAtNextToLastNode() {
            return passTwoCallCount == passOneCallCount;
        }

        private void checkSubchainStarted(Object base) {
            if (pass == InspectorPass.PASS2_FIND_FINAL_NODE && base == null && isAtNextToLastNode()) {
                // If "base" is null it means a new chain is being resolved.
                // The main expression chain likely has ended with a method that has one or more EL variables
                // as parameters that now need to be resolved.
                // E.g. a.b().c.d(var1)
                subchainResolving = true;
            }
        }

        private void recordCall(Object base, Object property) {

            if (pass == InspectorPass.PASS1_FIND_NEXT_TO_LAST_NODE) {

                // In the first "find next to last" pass, we'll be collecting the next to last element
                // in an expression.
                // E.g. given the expression a.b().c.d, we'll end up with the base returned by b() and "c" as
                // the last property.

                passOneCallCount++;
                lastBase = base;
                lastProperty = property;
            }
            else if (pass == InspectorPass.PASS2_FIND_FINAL_NODE) {

                // In the second "find final node" pass, we'll collecting the final node
                // in an expression. We need to take care that we're not actually calling / invoking
                // that last element as it may have a side-effect that the user doesn't want to happen
                // twice (like storing something in a DB etc).

                passTwoCallCount++;

                if (passTwoCallCount == passOneCallCount && (base != lastBase || !Objects.equals(property, lastProperty))) {
                    // We're at the same call count as the first phase ended with.
                    // If the chain has resolved the same, we should be dealing with the same base and property now
                    // If that is not the case, then throw ISE.
                    throw new IllegalStateException(
                        "First and second pass of resolver at call #" + passTwoCallCount +
                        " resolved to different base or property.");
                }
            }
        }

        private Object wrapOutcomeIfNeeded(Object outcome) {
            if (pass == InspectorPass.PASS2_FIND_FINAL_NODE && finalBaseHolder == null && isAtNextToLastNode()) {
                // We're at the second pass and at the next to last node in the expression chain.
                // "outcome" which we have just resolved should thus represent our final base.

                // Wrap our final base in a special class that we can recognize when the EL implementation
                // invokes this resolver later again with it.
                finalBaseHolder = new FinalBaseHolder(outcome);
                return finalBaseHolder;
            }

            return outcome;
        }

        public InspectorPass getPass() {
            return pass;
        }

        public void setPass(InspectorPass pass) {
            this.pass = pass;
        }

        public Object getBase() {
            return lastBase;
        }

        public Object getProperty() {
            return lastProperty;
        }

        public Object[] getParams() {
            return lastParams;
        }

        public Object getOutcome() {
            return lastOutcome;
        }

    }

}