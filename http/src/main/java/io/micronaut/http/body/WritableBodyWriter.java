/*
 * Copyright 2017-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.http.body;

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.io.Writable;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.MutableHeaders;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import io.micronaut.http.codec.CodecException;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Body writer for {@link Writable}s.
 *
 * @since 4.0.0
 * @author Graeme Rocher
 */
@Singleton
@Experimental
public final class WritableBodyWriter implements MessageBodyWriter<Writable> {
    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public void writeTo(Argument<Writable> type, MediaType mediaType, Writable object, MutableHeaders outgoingHeaders, OutputStream outputStream) throws CodecException {
        if (mediaType != null && !outgoingHeaders.contains(HttpHeaders.CONTENT_TYPE)) {
            outgoingHeaders.set(HttpHeaders.CONTENT_TYPE, mediaType);
        }
        try {
            object.writeTo(outputStream, MessageBodyWriter.getCharset(outgoingHeaders));
            outputStream.flush();
        } catch (IOException e) {
            throw new CodecException("Error writing body text: " + e.getMessage(), e);
        }
    }
}
