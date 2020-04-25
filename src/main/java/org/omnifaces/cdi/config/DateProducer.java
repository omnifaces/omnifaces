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

import java.time.Instant;
import java.util.Date;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.omnifaces.cdi.Eager;

/**
 * <p>
 * Producer for <code>#{startup}</code> and <code>#{now}</code>.
 * <p>
 * Since 4.0 it produces by default {@link Instant} whereas it previously produced {@link Date}.
 * <p>
 * Historical note: since 1.0 these were registered as beans in faces-config.xml. Since 3.5.1 these were migrated to
 * CDI producers, because the CDI implementation being used may emit warnings on them not being proxyable.
 *
 * @author Bauke Scholtz
 * @since 3.5.1
 */
public class DateProducer {

	/**
	 * This makes an instance of {@link Instant} as startup datetime available by <code>#{startup}</code>.
	 */
	@Produces @Named @ApplicationScoped @Eager
	public Instant getStartup() {
		return Instant.now();
	}

	/**
	 * This makes an instance of {@link Instant} as current datetime available by <code>#{now}</code>.
	 */
	@Produces @Named @RequestScoped
	public Instant getNow() {
		return Instant.now();
	}

}
