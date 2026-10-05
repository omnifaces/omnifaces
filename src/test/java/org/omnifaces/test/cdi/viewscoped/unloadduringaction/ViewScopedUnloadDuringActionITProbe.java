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

import javax.enterprise.context.ApplicationScoped;

/**
 * Lets the test hold the action of the view scoped bean while it sends the unload request, and observe when the bean
 * is destroyed. The latches cannot be reset, so this supports a single test run per deployment.
 */
@ApplicationScoped
public class ViewScopedUnloadDuringActionITProbe {

	private static final int TIMEOUT_IN_SECONDS = 30;

	private final CountDownLatch actionStarted = new CountDownLatch(1);
	private final CountDownLatch actionReleased = new CountDownLatch(1);
	private final CountDownLatch beanDestroyed = new CountDownLatch(1);

	public void startAction() {
		actionStarted.countDown();
		await(actionReleased);
	}

	public boolean awaitActionStarted() {
		return await(actionStarted);
	}

	public void releaseAction() {
		actionReleased.countDown();
	}

	public void destroyBean() {
		beanDestroyed.countDown();
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
