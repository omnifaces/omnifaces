package org.omnifaces.exceptionhandler;

import javax.faces.context.ExceptionHandlerFactory;

/**
 * Default implementation for {@link ExceptionHandlerFactory}, saving boilerplate to get hold of wrapped one.
 *
 * @author Bauke Scholtz
 * @since 2.0
 */
public abstract class DefaultExceptionHandlerFactory extends ExceptionHandlerFactory {

	// Variables ------------------------------------------------------------------------------------------------------

	private ExceptionHandlerFactory wrapped;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Constructs a exception handler factory, wrapping the given exception handler factory.
	 * @param wrapped The wrapped exception handler factory.
	 */
	public DefaultExceptionHandlerFactory(ExceptionHandlerFactory wrapped) {
		this.wrapped = wrapped;
	}

	// Getters --------------------------------------------------------------------------------------------------------

	/**
	 * Returns the wrapped exception handler factory.
	 */
	@Override
	public ExceptionHandlerFactory getWrapped() {
		return wrapped;
	}

}