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
package org.omnifaces.cdi;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Stereotype;

/**
 * Stereo type that designates a bean as an eagerly instantiated bean with application scope.
 * <p>
 * This bean type effectively functions as a CDI based startup listener for the web application.
 * <p>
 * Note that Java EE also provides the {@link javax.ejb.Startup} and {@link Singleton} annotations
 * which together provide similar functionality, but requires EJB and will result in the bean annotated 
 * with these annotations to become an EJB session bean. 
 * 
 * @since 1.8
 * @author Arjan Tijms
 *
 */
@ApplicationScoped
@Eager
@Stereotype
@Retention(RUNTIME)
@Target(TYPE)
public @interface Startup {}