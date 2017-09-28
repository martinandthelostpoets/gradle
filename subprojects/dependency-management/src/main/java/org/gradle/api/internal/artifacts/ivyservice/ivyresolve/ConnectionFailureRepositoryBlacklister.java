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

package org.gradle.api.internal.artifacts.ivyservice.ivyresolve;

import com.google.common.collect.Sets;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.gradle.api.UncheckedIOException;
import org.gradle.internal.resource.transport.http.HttpErrorStatusCodeException;

import java.io.IOException;
import java.util.Set;

public class ConnectionFailureRepositoryBlacklister implements RepositoryBlacklister {

    private final Set<String> blacklistedRepositories = Sets.newConcurrentHashSet();

    @Override
    public boolean isBlacklisted(String repositoryId) {
        return blacklistedRepositories.contains(repositoryId);
    }

    @Override
    public boolean blacklistRepository(String repositoryId, Throwable throwable) {
        boolean blacklisted = isBlacklisted(repositoryId);

        if (blacklisted) {
            return true;
        }

        if (isRootCauseIOException(throwable)) {
            blacklistedRepositories.add(repositoryId);
            return true;
        }

        return false;
    }

    @Override
    public Set<String> getBlacklistedRepositories() {
        return blacklistedRepositories;
    }

    private boolean isRootCauseIOException(Throwable throwable) {
        Throwable rootCause = ExceptionUtils.getRootCause(throwable);
        if(rootCause instanceof HttpErrorStatusCodeException){
            return false;
        }
        return rootCause instanceof IOException || rootCause instanceof UncheckedIOException;
    }
}
