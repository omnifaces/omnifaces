package org.omnifaces.el;

import static org.omnifaces.el.MethodReference.NO_PARAMS;
import static org.omnifaces.el.functions.Strings.capitalize;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
		inspectorElContext.setFindOneButLast(false); // TODO: or use same approach as getMethodReference?

		valueExpression.getType(inspectorElContext);

		return new ValueReference(inspectorElContext.getBase(), inspectorElContext.getProperty());
	}

	/**
	 * Gets a MethodReference from a ValueExpression. If the ValueExpression refers to a method, this will
	 * contain the actual method. If it refers to a property, this will contain the corresponding getter method.
	 * <p>
	 * Note that in case the expression refers to a method, the method reference contains <em>a</em> method with 
	 * the name the expression refers to. Overloads are not supported.
	 *
	 * @param context the context of this evaluation
	 * @param valueExpression the value expression being evaluated
	 * @return a MethodReference holding the final base and Method where the value expression evaluated to.
	 */
	public static MethodReference getMethodReference(ELContext context, ValueExpression valueExpression) {
		InspectorElContext inspectorElContext = new InspectorElContext(context);

		// Invoke getType() on the value expression to have the expression chain resolved.
		// The InspectorElContext contains a special resolver that will record the one but last
		// base and property. The EL implementation may shortcut the chain when it
		// discovers the final target was a method. E.g. #{a.b.c().d.f('1')}
		// In that case too, the result will be that the inspectorElContext contains the 
		// one but last base and property, and the length of the expression chain.
		valueExpression.getType(inspectorElContext);
		
		// If everything went well, we thus have the length of the chain now.
		
		// Flag that indicates that we now want to resolve the entire chain, so we
		// can capture the last element (the special resolver makes sure that
		// we don't actually invoke that last element)
		inspectorElContext.setFindOneButLast(false);
		
		// Calling getValue() will cause getValue() to be called on the resolver in case the
		// value expresses referred to a property, and invoke() when it's a method.
		ValueExpressionType type = (ValueExpressionType) valueExpression.getValue(inspectorElContext);
		
		return new MethodReference(
			inspectorElContext.getBase(), 
			findMethod(inspectorElContext.getBase(), inspectorElContext.getProperty().toString(), inspectorElContext.getParams()),
			inspectorElContext.getParams(),
			type == ValueExpressionType.METHOD
		);
	}

	/**
	 * Finds a method based on the method name, amount of parameters and limited typing, if necessary prefixed with "get".
	 * <p>
	 * Note that this supports overloading, but a limited one. Given an actual parameter of type Long, this will select
	 * a method accepting Number when the choice is between Number and a non-compatible type like String. However,
	 * it will NOT select the best match if the choice is between Number and Long.
	 *
	 * @param base the object in which the method is to be found
	 * @param methodName name of the method to be found
	 * @return a method if one is found, null otherwise
	 */
	public static Method findMethod(Object base, String methodName, Object[] params) {

	    List<Method> methods = new ArrayList<>();
		for (Method method : base.getClass().getMethods()) {
			if (method.getName().equals(methodName) && method.getParameterTypes().length == params.length) {
			    methods.add(method);
			}
		}
		
		if (methods.size() == 1) {
		    return methods.get(0);
		}
		
		if (methods.size() > 1) {
		    // Overloaded methods were found. Try to get a match
		    for (Method method : methods) {
		        boolean match = true;
		        Class<?>[] candidateParams = method.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
		            if (!candidateParams[i].isInstance(params[i])) {
		                match = false;
		                break;
		            }
		        }
                
                // If all candidate parameters were expected and for none of them the actual
                // parameter was NOT an instance, we have a match
                if (match) {
                    return method;
                }
                
                // Else, at least one parameter was not an instance
                // Go ahead a test then next methods
		    }
		}

		if (!methodName.startsWith("get")) {
			return findMethod(base, "get" + capitalize(methodName), NO_PARAMS);
		}

		return null;
	}
	
	/**
	 * Types of a value expression
	 *
	 */
	private enum ValueExpressionType {
		METHOD,  // Value expression that refers to a method, e.g. #{foo.bar(1)}
		PROPERTY // Value expression that refers to a property, e.g. #{foo.bar}
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
		
		public Object[] getParams() {
			return inspectorElResolver.getParams();
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
		private Object lastProperty; // Method name in case VE referenced a method, otherwise property name
		private Object[] lastParams; 	// Actual parameters supplied to a method (if any)

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
			
			return ValueExpressionType.PROPERTY;
		}

		@Override
		public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {

			if (recordCall(base, method)) {
				return super.invoke(context, base, method, paramTypes, params);
			} else {
				lastParams = params;
			}

			context.setPropertyResolved(true);
			return ValueExpressionType.METHOD;
		}

		@Override
		public Class<?> getType(ELContext context, Object base, Object property) {

			// getType is only called on the last element in the chain (if the EL
			// implementation actually calls this, which might not be the case if the
			// value expression references a method)
			// We thus do know the size of the chain now, and the lastBase and lastProperty
			// that were set before this call are the one but last now.

			if (!findOneButLast) {
				// If findOneButLast is set to false and ValueExpression#getType() is called,
				// it will immediately give us the final lastBase and lastProperty when the
				// ValueExpression is guaranteed to reference a property. If it references
				// a method instead it's up to the EL implementation what happens. 
				// UEL (Sun/GlassFish) 3.0 will NOT do the final getType() call in that case.
				lastBase = base;
				lastProperty = property;
			}

			context.setPropertyResolved(true);
			
			// Special value to signal that getType() has actually been called.
			return InspectorElContext.class;
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
		
		public Object[] getParams() {
			return lastParams;
		}

	}

}
