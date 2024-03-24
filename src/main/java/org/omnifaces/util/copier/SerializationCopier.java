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
package org.omnifaces.util.copier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Copier that copies an object by serializing and subsequently deserializing it again.
 * <p>
 * As per the platform serialization rules, the object and all its non transient dependencies have
 * to implement the {@link Serializable} interface.
 *
 * @since 2.0
 * @author Arjan Tijms
 *
 */
public class SerializationCopier implements Copier {

    @Override
    public Object copy(Object object) {

        if (!(object instanceof Serializable)) {
            throw new IllegalStateException("Can't copy object of type " + object.getClass() + " since it doesn't implement Serializable");
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            new ObjectOutputStream(outputStream).writeObject(object);

            return new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray())).readObject();

        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

    }

}
