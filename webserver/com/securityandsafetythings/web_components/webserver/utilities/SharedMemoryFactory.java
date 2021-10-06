/*
 * Copyright 2019-2020 by Security and Safety Things GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.securityandsafetythings.web_components.webserver.utilities;

import android.os.SharedMemory;
import android.system.ErrnoException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * {@link SharedMemory} factory class
 */
public class SharedMemoryFactory {

    /**
     * Creates a {@link SharedMemory} of a String (UTF-8 encoded)
     *
     * @param value string to put
     * @return shared memory
     * @throws ErrnoException on error
     */
    SharedMemory createSharedMemoryForString(final String value) throws ErrnoException {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        return createSharedMemoryForBytes(bytes);
    }

    /**
     * Creates a {@link SharedMemory} of a byte array
     *
     * @param bytes byte data
     * @return shared memory
     * @throws ErrnoException on error
     */
    SharedMemory createSharedMemoryForBytes(final byte[] bytes) throws ErrnoException {
        final SharedMemory sharedMemory = SharedMemory.create("response", bytes.length);
        final ByteBuffer byteBuffer = sharedMemory.mapReadWrite();
        byteBuffer.put(bytes);
        return sharedMemory;
    }
}
