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
package org.omnifaces.cdi.contextparam;

import static org.omnifaces.util.Beans.getQualifier;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;

import org.omnifaces.cdi.ContextParam;

/**
 * <p>
 * Producer for injecting a <code>web.xml</code> context parameter as defined by the {@link ContextParam} annotation.
 *
 * @since 2.2
 * @author Bauke Scholtz
 */
@Dependent
public class ContextParamProducer {

    @SuppressWarnings("unused") // Workaround for OpenWebBeans not properly passing it as produce() method argument.
    @Inject
    private InjectionPoint injectionPoint;

    @Inject
    private ServletContext servletContext;

    /**
     * Returns context parameter value associated with context parameter name derived from given injection point.
     * @param injectionPoint Injection point to derive context parameter name from.
     * @return Context parameter value associated with context parameter name derived from given injection point.
     */
    @Produces
    @ContextParam
    public String produce(InjectionPoint injectionPoint) {
        ContextParam param = getQualifier(injectionPoint, ContextParam.class);
        String name = param.name().isEmpty() ? injectionPoint.getMember().getName() : param.name();
        return servletContext.getInitParameter(name);
    }

}