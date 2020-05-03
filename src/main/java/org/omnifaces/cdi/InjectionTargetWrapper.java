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

import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.faces.FacesWrapper;

/**
 * <p>
 * Provides a simple implementation of {@link InjectionTarget} that can be sub-classed by developers wishing to
 * provide specialized behavior to an existing {@link InjectionTarget} instance. The default implementation of all
 * methods is to call through to the wrapped {@link InjectionTarget}.</p>
 *
 * <p>
 * Usage: extend this class and override {@link #getWrapped} to return the instance we are wrapping.
 *
 * @author Bauke Scholtz
 * @since 3.6
 */
public class InjectionTargetWrapper<T> implements InjectionTarget<T>, FacesWrapper<InjectionTarget<T>> {

	private final InjectionTarget<T> wrapped;

	public InjectionTargetWrapper(InjectionTarget<T> wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public InjectionTarget<T> getWrapped() {
		return wrapped;
	}

	@Override
	public T produce(CreationalContext<T> ctx) {
		return getWrapped().produce(ctx);
	}

	@Override
	public void dispose(T instance) {
		getWrapped().dispose(instance);
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		return getWrapped().getInjectionPoints();
	}

	@Override
	public void inject(T instance, CreationalContext<T> ctx) {
		getWrapped().inject(instance, ctx);
	}

	@Override
	public void postConstruct(T instance) {
		getWrapped().postConstruct(instance);
	}

	@Override
	public void preDestroy(T instance) {
		getWrapped().preDestroy(instance);
	}

}
