/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.resource.transport.http;

import org.gradle.api.UncheckedIOException;
import org.gradle.internal.exceptions.Contextual;

/**
 * Signals that HTTP response has been received successfully but an error code is encountered (neither 2xx/3xx nor 404).
 */
@Contextual
public class HttpErrorStatusCodeException extends UncheckedIOException {
    private String method;
    private String source;
    private int statusCode;
    private String reason;

    public HttpErrorStatusCodeException(String method, String source, int statusCode, String reason) {
        this(String.format("Could not %s '%s'. Received status code %s from server: %s",
            method, source, statusCode, reason));
        this.method = method;
        this.source = source;
        this.statusCode = statusCode;
        this.reason = reason;
    }

    public HttpErrorStatusCodeException(String message) {
        super(message);
    }
}
