/*
 * Copyright 2020 OmniFaces
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
package org.omnifaces.cdi;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.annotation.PostConstruct;

import org.omnifaces.component.input.ScriptParam;

/**
 * <p>
 * The annotation <code>&#64;</code>{@link PostScriptParam} allows you to invoke a managed bean method when all
 * <code>&lt;o:scriptParam&gt;</code> values have been set in the associated managed bean. It's basically like a
 * {@link PostConstruct} for them, but then for <code>&lt;o:scriptParam&gt;</code>.
 *
 * @since 3.6
 * @author Bauke Scholtz
 * @see ScriptParam
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface PostScriptParam {
	//
}