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
package org.omnifaces.cdi.config;

import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.omnifaces.cdi.Eager;

/**
 * <p>
 * Producer for <code>#{startup}</code> and <code>#{now}</code>.
 *
 * @author Bauke Scholtz
 * @since 3.5.1
 */
public class DateProducer {

	/**
	 * This makes an instance of <code>java.util.Date</code> as startup datetime available by <code>#{startup}</code>.
	 */
	@Produces @Named @ApplicationScoped @Eager
	public Date getStartup() {
		return new Date();
	}

	/**
	 * This makes an instance of <code>java.util.Date</code> as current datetime available by <code>#{now}</code>.
	 */
	@Produces @Named @RequestScoped
	public Date getNow() {
		return new Date();
	}

}
