/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.composite.internal;

import org.gradle.StartParameter;
import org.gradle.api.internal.artifacts.ImmutableModuleIdentifierFactory;
import org.gradle.api.internal.composite.CompositeBuildContext;
import org.gradle.api.internal.initialization.ScriptClassPathInitializer;
import org.gradle.api.internal.tasks.TaskReferenceResolver;
import org.gradle.initialization.BuildIdentity;
import org.gradle.internal.composite.CompositeContextBuilder;
import org.gradle.internal.concurrent.ExecutorFactory;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistration;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.service.scopes.AbstractPluginServiceRegistry;
import org.gradle.internal.work.WorkerLeaseService;

public class CompositeBuildServices extends AbstractPluginServiceRegistry {
    @Override
    public void registerBuildTreeServices(ServiceRegistration registration) {
        registration.addProvider(new CompositeBuildTreeScopeServices());
    }

    @Override
    public void registerBuildServices(ServiceRegistration registration) {
        registration.addProvider(new CompositeBuildBuildScopeServices());
    }

    private static class CompositeBuildTreeScopeServices {
        public DefaultIncludedBuildRegistry createIncludedBuildRegistry(IncludedBuildFactory includedBuildFactory) {
            return new DefaultIncludedBuildRegistry(includedBuildFactory);
        }

        public IncludedBuildFactory createIncludedBuildFactory(Instantiator instantiator, StartParameter startParameter, ImmutableModuleIdentifierFactory moduleIdentifierFactory, WorkerLeaseService workerLeaseService) {
            return new DefaultIncludedBuildFactory(instantiator, startParameter, moduleIdentifierFactory, workerLeaseService);
        }

        public CompositeBuildContext createCompositeBuildContext(IncludedBuildRegistry includedBuildRegistry, ImmutableModuleIdentifierFactory moduleIdentifierFactory) {
            return new DefaultBuildableCompositeBuildContext(includedBuildRegistry, moduleIdentifierFactory);
        }

        public IncludedBuildDependencySubstitutionsBuilder createIncludedBuildDependencySubstitutionsBuilder(CompositeBuildContext context, ImmutableModuleIdentifierFactory moduleIdentifierFactory) {
            return new IncludedBuildDependencySubstitutionsBuilder(context, moduleIdentifierFactory);
        }

        public DefaultProjectPathRegistry createProjectPathRegistry() {
            return new DefaultProjectPathRegistry();
        }

        public CompositeContextBuilder createCompositeContextBuilder(IncludedBuildRegistry includedBuildRegistry, DefaultProjectPathRegistry projectRegistry, IncludedBuildDependencySubstitutionsBuilder dependencySubstitutionsBuilder) {
            return new DefaultCompositeContextBuilder(includedBuildRegistry, projectRegistry, dependencySubstitutionsBuilder);
        }

        public IncludedBuildControllers createIncludedBuildControllers(ExecutorFactory executorFactory, IncludedBuildRegistry includedBuildRegistry) {
            return new DefaultIncludedBuildControllers(executorFactory, includedBuildRegistry);
        }

        public IncludedBuildTaskGraph createIncludedBuildTaskGraph(IncludedBuildControllers controllers) {
            return new DefaultIncludedBuildTaskGraph(controllers);
        }
    }

    private static class CompositeBuildBuildScopeServices {
        public TaskReferenceResolver createResolver(IncludedBuildTaskGraph includedBuilds, BuildIdentity buildIdentity) {
            return new IncludedBuildTaskReferenceResolver(includedBuilds, buildIdentity);
        }

        public ScriptClassPathInitializer createCompositeBuildClasspathResolver(IncludedBuildRegistry includedBuildRegistry, IncludedBuildTaskGraph includedBuildTaskGraph, ServiceRegistry serviceRegistry) {
            return new CompositeBuildClassPathInitializer(includedBuildRegistry, includedBuildTaskGraph, serviceRegistry);
        }
    }

}
