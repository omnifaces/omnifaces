package org.omnifaces.cdi.eager;


/**
 * Interface used to hide the implementation class of the actual repository from Tomcat when no CDI implementation has
 * been added.
 * <p>
 * This will otherwise throw ClassNotFoundExceptions for the various CDI classes uses by the repository.
 * 
 * @author Arjan Tijms
 * 
 */
public interface HideForTomcatEagerBeansRepository {

	void instantiateApplicationScoped();
	void instantiateSessionScoped();
	void instantiateByRequestURI(String relativeRequestURI);
	void instantiateByViewID(String viewId);
	
}
