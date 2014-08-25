package org.omnifaces.el;

import static org.omnifaces.el.functions.Strings.capitalize;

import java.lang.reflect.Method;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ValueExpression;
import javax.el.ValueReference;

/**
 * This class contains methods that inspect expressions to reveal information about them.
 *
 * @author Arjan Tijms
 *
 * @since 1.4
 */
public class ExpressionInspector {

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

		return new ValueReference(inspectorElContext.getBase(), inspectorElContext.getProperty());
	}

	/**
	 * Gets a MethodReference from a ValueExpression. This assumes that this expression references a method.
	 * <p>
	 * Note that the method reference contains <em>a</em> method with the name the expression refers to.
	 * Overloads are not supported.
	 *
	 *
	 * @param context the context of this evaluation
	 * @param valueExpression the value expression being evaluated
	 * @return a MethodReference holding the final base and Method where the value expression evaluated to.
	 */
	public static MethodReference getMethodReference(ELContext context, ValueExpression valueExpression) {
		ValueReference valueReference = getValueReference(context, valueExpression);

		return new MethodReference(valueReference.getBase(), findMethod(valueReference.getBase(), valueReference.getProperty().toString()));
	}

	/**
	 * Finds a method based on the method name only, if necessary prefixed with "get".
	 * Does not support overloaded methods.
	 *
	 * @param base the object in which the method is to be found
	 * @param methodName name of the method to be found
	 * @return a method if one is found, null otherwise
	 */
	public static Method findMethod(Object base, String methodName) {

		for (Method method : base.getClass().getMethods()) {
			if (method.getName().equals(methodName)) {
				return method;
			}
		}

		if (!methodName.startsWith("get")) {
			return findMethod(base, "get" + capitalize(methodName));
		}

		return null;
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
			inspectorElResolver = new InspectorElResolver(super.getELResolver());
		}

		@Override
		public ELResolver getELResolver() {
			return inspectorElResolver;
		}

		public boolean isFindOneButLast() {
			return inspectorElResolver.isFindOneButLast();
		}

		public void setFindOneButLast(boolean findOneButLast) {
			inspectorElResolver.setFindOneButLast(findOneButLast);
		}

		public Object getBase() {
			return inspectorElResolver.getBase();
		}

		public Object getProperty() {
			return inspectorElResolver.getProperty();
		}

	}

	/**
	 * Custom EL Resolver that can be used for inspecting expressions by means of recording the calls
	 * made on this resolved by the EL implementation.
	 *
	 */
	static class InspectorElResolver extends ELResolverWrapper {

		private int findOneButLastCallCount;
		private int findLastCallCount;
		private Object lastBase;
		private Object lastProperty;

		private boolean findOneButLast = true;

		public InspectorElResolver(ELResolver elResolver) {
			super(elResolver);
		}

		@Override
		public Object getValue(ELContext context, Object base, Object property) {

			if (recordCall(base, property)) {
				return super.getValue(context, base, property);
			}

			context.setPropertyResolved(true);
			return null;
		}

		@Override
		public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {

			if (recordCall(base, method)) {
				return super.invoke(context, base, method, paramTypes, params);
			}

			context.setPropertyResolved(true);
			return null;
		}

		@Override
		public Class<?> getType(ELContext context, Object base, Object property) {

			// getType is only called on the last element in the chain, so this immediately gives
			// us our targets.

			lastBase = base;
			lastProperty = property;

			context.setPropertyResolved(true);
			return null;
		}

		private boolean recordCall(Object base, Object property) {

			if (findOneButLast) {

				// In the first "findOneButLast" pass, we'll collecting the one but last element
				// in an expression.
				// E.g. given the expression a.b().c.d, we'll end up with the base returned by b() and "c" as
				// the last property.

				findOneButLastCallCount++;
				lastBase = base;
				lastProperty = property;
			} else {

				// In the second "findLast" pass, we'll collecting the final element
				// in an expression. We need to make care that we're not actually calling / invoking
				// that last element as it may have a side-effect that the user doesn't want to happen
				// twice (like storing something in a DB etc).

				findLastCallCount++;

				if (findLastCallCount == findOneButLastCallCount) {

					// We're at the same call count as the first phase ended with.
					// If the chain has resolved the same, we should be dealing with the same base and property now

					if (base != lastBase || property != lastProperty) {
						System.out.print("Error! Not equal!"); // tmp
					}

				} else if (findLastCallCount == findOneButLastCallCount + 1) {

					// We're at the (supposedly!) last element of our expression chain.
					lastBase = base;
					lastProperty = property;

					// Don't continue with the call, as we'll otherwise invoke the last element of the chain
					// which has the possible side-effect.
					return false;
				}
			}

			// Continue with the call, as we're on an intermediate node that's save to invoke multiple times.
			return true;
		}

		public boolean isFindOneButLast() {
			return findOneButLast;
		}

		public void setFindOneButLast(boolean findOneButLast) {
			this.findOneButLast = findOneButLast;
		}

		public Object getBase() {
			return lastBase;
		}

		public Object getProperty() {
			return lastProperty;
		}

	}

}
