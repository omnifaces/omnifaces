package org.omnifaces.cdi.eager;

public interface HideForTomcatEagerBeansRepository {

	void instantiateByRequestURI(String relativeRequestURI);
	void instantiateByViewID(String viewId);
	
}
