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
package org.omnifaces.test.cdi.viewscoped.unloadduringaction;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

/**
 * Lets the test hold the action of the view scoped bean while it sends the unload request, and observe when the bean
 * is destroyed. Only the destroy of the bean whose action was held is observed. The test must {@link #reset()} it
 * before each run.
 */
@ApplicationScoped
public class ViewScopedUnloadDuringActionITProbe {

	private static final int TIMEOUT_IN_SECONDS = 30;

	private volatile String actionBeanId;
	private volatile CountDownLatch actionStarted;
	private volatile CountDownLatch actionReleased;
	private volatile CountDownLatch beanDestroyed;

	@PostConstruct
	public void reset() {
		actionBeanId = null;
		actionStarted = new CountDownLatch(1);
		actionReleased = new CountDownLatch(1);
		beanDestroyed = new CountDownLatch(1);
	}

	public void startAction(String beanId) {
		actionBeanId = beanId;
		actionStarted.countDown();
		await(actionReleased);
	}

	public boolean awaitActionStarted() {
		return await(actionStarted);
	}

	public void releaseAction() {
		actionReleased.countDown();
	}

	public void destroyBean(String beanId) {
		if (beanId.equals(actionBeanId)) {
			beanDestroyed.countDown();
		}
	}

	public boolean awaitBeanDestroyed() {
		return await(beanDestroyed);
	}

	private static boolean await(CountDownLatch latch) {
		try {
			return latch.await(TIMEOUT_IN_SECONDS, SECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

}
