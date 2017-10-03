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

import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.InterruptedIOException;
import java.util.HashSet;
import java.util.Set;

public class ConnectionInterruptionRepositoryBlacklister implements RepositoryBlacklister {

    private final Set<String> blacklistedRepositories = new HashSet<String>();

    @Override
    public boolean isBlacklisted(String repositoryId) {
        return blacklistedRepositories.contains(repositoryId);
    }

    @Override
    public void blacklistRepository(String repositoryId, Throwable throwable) {
        if (!isBlacklisted(repositoryId) && isRootCauseInterruptedIOException(throwable)) {
            blacklistedRepositories.add(repositoryId);
        }
    }

    @Override
    public Set<String> getBlacklistedRepositories() {
        return blacklistedRepositories;
    }

    private boolean isRootCauseInterruptedIOException(Throwable throwable) {
        return ExceptionUtils.getRootCause(throwable) instanceof InterruptedIOException;
    }
}
