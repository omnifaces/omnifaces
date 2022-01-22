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
package org.omnifaces.cdi.config;

import static org.omnifaces.util.Utils.toZonedDateTime;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
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
 * Since 4.0 it produces by default an instance of {@link Temporal} whereas it previously produced an instance of
 * {@link Date}.
 * <p>
 * Historical note: since 1.0 these were registered as beans in faces-config.xml. Since 3.6 these were migrated to
 * CDI producers, because the CDI implementation being used may emit warnings on them not being proxyable.
 *
 * <h2>Usage</h2>
 * <ul>
 * <li>You can reference the application startup time as {@link Temporal} via <code>#{startup}</code> in EL.</li>
 * <li>You can reference the current time as {@link Temporal} via <code>#{now}</code> in EL.</li>
 * <li>They have a {@link TemporalDate#getTime()} method which allows you to obtain the epoch time via
 * <code>#{startup.time}</code> and <code>#{now.time}</code> in EL.</li>
 * <li>They have a {@link TemporalDate#getInstant()} method which allows you to convert them to {@link Instant} via
 * <code>#{startup.instant}</code> and <code>#{now.instant}</code> in EL.</li>
 * <li>They have a {@link TemporalDate#getZonedDateTime()} method which allows you to convert them to
 * {@link ZonedDateTime} via <code>#{startup.zonedDateTime}</code> and <code>#{now.zonedDateTime}</code> in EL.</li>
 * <li>They are injectable in CDI beans via e.g. <code>&#64;Inject &#64;Named private Temporal startup;</code>.
 * </ul>
 *
 * @author Bauke Scholtz
 * @since 3.6
 */
public class DateProducer {

	/**
	 * This makes an instance of {@link Temporal} as startup datetime available by <code>#{startup}</code>.
	 * @return Startup datetime.
	 */
	@Produces @Named @ApplicationScoped @Eager
	public TemporalDate getStartup() {
		return new TemporalDate();
	}

	/**
	 * This makes an instance of {@link Temporal} as current datetime available by <code>#{now}</code>.
	 * @return Current datetime.
	 */
	@Produces @Named @RequestScoped
	public TemporalDate getNow() {
		return new TemporalDate();
	}

	/**
	 * {@link Instant} is a final class, hence this proxy for CDI. Plus, it also offers a fallback for existing EL
	 * expressions relying on {@link Date#getTime()} such as <code>#{now.time}</code> so that they continue working
	 * after migration from {@link Date} to {@link Temporal} in OmniFaces 4.0.
	 *
	 * @author Bauke Scholtz
	 * @since 4.0
	 */
	public static class TemporalDate implements Temporal, Comparable<TemporalDate>, Serializable {

		private static final long serialVersionUID = 1L;

		private Instant instant;
		private ZonedDateTime zonedDateTime;
		private long time;
		private String string;

		/**
		 * Constructs a new proxyable instant which is initialized with {@link Instant#now()}.
		 */
		public TemporalDate() {
			this(Instant.now());
		}

		/**
		 * Constructs a new proxyable instant which is initialized with given {@link Instant}.
		 * @param instant Instant to initialize with.
		 */
		public TemporalDate(Instant instant) {
			this.instant = instant;
			this.zonedDateTime = toZonedDateTime(instant);
			this.time = instant.toEpochMilli();
			this.string = instant.toString();
		}

		/**
		 * Convenience method to return this as {@link Instant}.
		 * @return This as {@link Instant}.
		 */
		public Instant getInstant() {
			return instant;
		}

		/**
		 * Convenience method to return this as {@link ZonedDateTime}.
		 * @return This as {@link ZonedDateTime}.
		 */
		public ZonedDateTime getZonedDateTime() {
			return zonedDateTime;
		}

		/**
		 * Has the same signature as {@link Date#getTime()}.
		 * This ensures that <code>#{now.time}</code> and <code>#{startup.time}</code> keep working.
		 * @return The number of milliseconds since January 1, 1970, 00:00:00 GMT represented by this instant.
		 */
		public long getTime() {
			return time;
		}

		// Required overrides -----------------------------------------------------------------------------------------

		/**
		 * Returns {@link Instant#compareTo(Instant)}
		 */
		@Override
		public int compareTo(TemporalDate other) {
			return instant.compareTo(other.getInstant());
		}

		/**
		 * Returns {@link Instant#equals(Object)}
		 */
		@Override
		public boolean equals(Object other) {
			return instant.equals(other);
		}

		/**
		 * Returns {@link Instant#hashCode()}
		 */
		@Override
		public int hashCode() {
			return instant.hashCode();
		}

		/**
		 * Returns {@link Instant#toString()}.
		 */
		@Override
		public String toString() {
			return string;
		}

		// Temporal delegates -----------------------------------------------------------------------------------------

		@Override
		public boolean isSupported(TemporalField field) {
			return instant.isSupported(field);
		}

		@Override
		public long getLong(TemporalField field) {
			return instant.getLong(field);
		}

		@Override
		public boolean isSupported(TemporalUnit unit) {
			return instant.isSupported(unit);
		}

		@Override
		public Temporal with(TemporalField field, long newValue) {
			return instant.with(field, newValue);
		}

		@Override
		public Temporal plus(long amountToAdd, TemporalUnit unit) {
			return instant.plus(amountToAdd, unit);
		}

		@Override
		public long until(Temporal endExclusive, TemporalUnit unit) {
			return instant.until(endExclusive, unit);
		}

	}
}
