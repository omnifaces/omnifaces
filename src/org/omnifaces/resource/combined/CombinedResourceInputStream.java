/*
 * Copyright 2012 OmniFaces.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.resource.combined;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.application.Resource;

import org.omnifaces.util.Utils;

/**
 * This {@link InputStream} implementation takes care that all in the constructor given resources are been read in
 * sequence.
 * @author Bauke Scholtz
 */
final class CombinedResourceInputStream extends InputStream {

	// Properties -----------------------------------------------------------------------------------------------------

	private List<InputStream> streams;
	private Iterator<InputStream> streamIterator;
	private InputStream currentStream;

	// Constructors ---------------------------------------------------------------------------------------------------

	/**
	 * Creates an instance of {@link CombinedResourceInputStream} based on the given resources. For each resource, the
	 * {@link InputStream} will be obtained and hold in an iterable collection.
	 * @param resources The resources to be read.
	 * @throws IOException If something fails at I/O level.
	 */
	public CombinedResourceInputStream(Set<Resource> resources) throws IOException {
		streams = new ArrayList<InputStream>();

		for (Resource resource : resources) {
			streams.add(resource.getInputStream());
		}

		streamIterator = streams.iterator();
		streamIterator.hasNext(); // We assume it to be always true; CombinedResourceInfo won't be created anyway if it's empty.
		currentStream = streamIterator.next();
	}

	// Actions --------------------------------------------------------------------------------------------------------

	/**
	 * For each resource, read until its {@link InputStream#read()} returns <tt>-1</tt> and then iterate to the
	 * {@link InputStream} of the next resource, if any available, else return <tt>-1</tt>.
	 */
	@Override
	public int read() throws IOException {
		int read = -1;

		while ((read = currentStream.read()) == -1) {
			if (streamIterator.hasNext()) {
				currentStream = streamIterator.next();
			}
			else {
				break;
			}
		}

		return read;
	}

	/**
	 * Closes the {@link InputStream} of each resource. Whenever the {@link InputStream#close()} throws an
	 * {@link IOException} for the first time, it will be caught and be thrown after all resources have been closed.
	 * Any {@link IOException} which is thrown by a subsequent close will be ignored by design.
	 */
	@Override
	public void close() throws IOException {
		IOException caught = null;

		for (InputStream stream : streams) {
			IOException e = Utils.close(stream);

			if (caught == null) {
				caught = e; // Don't throw it yet. We have to continue closing all other streams.
			}
		}

		if (caught != null) {
			throw caught;
		}
	}

}