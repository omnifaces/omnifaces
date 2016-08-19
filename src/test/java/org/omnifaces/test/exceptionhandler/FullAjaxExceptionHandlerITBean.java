package org.omnifaces.test.exceptionhandler;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

@Named
@RequestScoped
public class FullAjaxExceptionHandlerITBean {

	public void throwDuringInvokeApplication() {
		throw new RuntimeException("throwDuringInvokeApplication");
	}

	public Object getThrowDuringUpdateModelValues() {
		return null;
	}

	public Object setThrowDuringUpdateModelValues(Object input) {
		throw new RuntimeException("throwDuringUpdateModelValues");
	}

	public Object getThrowDuringRenderResponse() {
		throw new RuntimeException("throwDuringRenderResponse");
	}

}