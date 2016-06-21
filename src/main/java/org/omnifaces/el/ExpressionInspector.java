package org.omnifaces.el;

import static org.omnifaces.el.MethodReference.NO_PARAMS;
import static org.omnifaces.el.functions.Strings.capitalize;
import static org.omnifaces.util.Reflection.findMethod;

import java.lang.reflect.Method;
import java.util.Objects;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.MethodNotFoundException;
import javax.el.ValueExpression;
import javax.el.ValueReference;
import javax.faces.el.CompositeComponentExpressionHolder;

/**
 * This class contains methods that inspect expressions to reveal information about them.
 *
 * @author Arjan Tijms
 *
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
	 * <p>
	 * This is also a workaround for the fact that a ValueReference can't
	 * be obtained from a TagValueExpression in JSF 2.x (since it doesn't implement getValueReference and its super
	 * class just returns null).
	 *
	 * @param context the context of this evaluation
	 * @param valueExpression the value expression being evaluated
	 * @return a ValueReference holding the final base and property where the value expression evaluated to.
	 */
	public static ValueReference getValueReference(ELContext context, ValueExpression valueExpression) {

		InspectorElContext inspectorElContext = new InspectorElContext(context);
		valueExpression.getType(inspectorElContext);
		inspectorElContext.setPass(InspectorPass.PASS2_FIND_FINAL_NODE);
		valueExpression.getValue(inspectorElContext);

		Object base = inspectorElContext.getBase();
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
		valueExpression.getType(inspectorElContext);

		// If everything went well, we thus have the length of the chain now.

		// Indicate that we're now at pass 2 and want to resolve the entire chain.
		// We can then capture the last element (the special resolver makes sure that
		// we don't actually invoke that last element)
		inspectorElContext.setPass(InspectorPass.PASS2_FIND_FINAL_NODE);

		// Calling getValue() will cause getValue() to be called on the resolver in case the
		// value expresses referred to a property, and invoke() when it's a method.
		ValueExpressionType type = (ValueExpressionType) valueExpression.getValue(inspectorElContext);

		String methodName = inspectorElContext.getProperty().toString();
		Object[] params = inspectorElContext.getParams();

		if (type != ValueExpressionType.METHOD) {
			methodName = "get" + capitalize(methodName); // support for "is"?
			params = NO_PARAMS;
		}

		Method method = findMethod(inspectorElContext.getBase(), methodName, params);

		if (method == null) {
			throw new MethodNotFoundException(inspectorElContext.getBase() + "." + methodName + " " + valueExpression);
		}

		return new MethodReference(inspectorElContext.getBase(), method, inspectorElContext.getParams(), type == ValueExpressionType.METHOD);
	}



	/**
	 * Types of a value expression
	 *
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
	 *
	 * @author arjan
	 *
	 */
	private enum InspectorPass {
		PASS1_FIND_NEXT_TO_LAST_NODE,
		PASS2_FIND_FINAL_NODE
	}

	/**
	 * Custom ELContext implementation that wraps a given ELContext to be able to provide a custom
	 * ElResolver.
	 *
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
	 *
	 */
	static class InspectorElResolver extends ELResolverWrapper {

		private int passOneCallCount;
		private int passTwoCallCount;

		private Object lastBase;
		private Object lastProperty; // Method name in case VE referenced a method, otherwise property name
		private Object[] lastParams; // Actual parameters supplied to a method (if any)

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

				// Normally, we'd return ValueExpressionType.PROPERTY here, but this causes trouble with EL coercion.
				// TODO: When on EL 3.0, implement InspectorELContext#convertToType() to always return original value,
				// so we can "nicely" return ValueExpressionType.PROPERTY here.
				return null;
			}

			checkSubchainStarted(base);

			if (subchainResolving) {
				return super.getValue(context, base, property);
			}

			recordCall(base, property);

			return wrapOutcomeIfNeeded(super.getValue(context, base, property));
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

			if (subchainResolving) {
				return super.invoke(context, base, method, paramTypes, params);
			}

			recordCall(base, method);

			return wrapOutcomeIfNeeded(super.invoke(context, base, method, paramTypes, params));
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

			switch (pass) {
				case PASS1_FIND_NEXT_TO_LAST_NODE:

					// In the first "find next to last" pass, we'll be collecting the next to last element
					// in an expression.
					// E.g. given the expression a.b().c.d, we'll end up with the base returned by b() and "c" as
					// the last property.

					passOneCallCount++;
					lastBase = base;
					lastProperty = property;

					break;

				case PASS2_FIND_FINAL_NODE:

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

					break;
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

	}

}