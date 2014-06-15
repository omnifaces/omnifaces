/*
 * Copyright 2014 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.cdi.eager;


/**
 * Interface for types that know how to instantiate all beans of a certain type.
 * <p> 
 * Note: the prime purpose of this interface is to hide the implementation class of the actual repository from 
 * Servlet containers like Tomcat when no CDI implementation has been added. This will otherwise throw ClassNotFoundExceptions 
 * for the various CDI classes uses by the repository, even when CDI is not being used by user.
 * <p>
 * This interface will most likely be removed in OmniFaces 2.0 if OmniFaces 2.0 requires CDI to be present.
 * 
 * @since 1.8
 * @author Arjan Tijms
 * 
 */
public interface BeansInstantiator {

	void instantiateApplicationScoped();
	void instantiateSessionScoped();
	void instantiateByRequestURI(String relativeRequestURI);
	void instantiateByViewID(String viewId);
	
}
