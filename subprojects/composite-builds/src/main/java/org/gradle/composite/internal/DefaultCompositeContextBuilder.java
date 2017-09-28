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

import com.google.common.collect.Lists;
import org.gradle.api.artifacts.component.BuildIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.initialization.IncludedBuild;
import org.gradle.api.internal.SettingsInternal;
import org.gradle.api.internal.artifacts.component.DefaultBuildIdentifier;
import org.gradle.api.internal.project.ProjectRegistry;
import org.gradle.initialization.DefaultProjectDescriptor;
import org.gradle.initialization.NestedBuildFactory;
import org.gradle.internal.component.local.model.DefaultProjectComponentIdentifier;
import org.gradle.internal.composite.CompositeContextBuilder;
import org.gradle.util.Path;

import java.util.List;
import java.util.Set;

public class DefaultCompositeContextBuilder implements CompositeContextBuilder {
    private final IncludedBuildRegistry includedBuildRegistry;
    private final DefaultProjectPathRegistry projectRegistry;
    private final IncludedBuildDependencySubstitutionsBuilder dependencySubstitutionsBuilder;

    public DefaultCompositeContextBuilder(IncludedBuildRegistry includedBuildRegistry, DefaultProjectPathRegistry projectRegistry, IncludedBuildDependencySubstitutionsBuilder dependencySubstitutionsBuilder) {
        this.includedBuildRegistry = includedBuildRegistry;
        this.projectRegistry = projectRegistry;
        this.dependencySubstitutionsBuilder = dependencySubstitutionsBuilder;
    }

    @Override
    public void setRootBuild(SettingsInternal settings) {
        ProjectRegistry<DefaultProjectDescriptor> settingsProjectRegistry = settings.getProjectRegistry();
        String rootName = settingsProjectRegistry.getRootProject().getName();
        DefaultBuildIdentifier buildIdentifier = new DefaultBuildIdentifier(rootName, true);
        registerProjects(Path.ROOT, buildIdentifier, settingsProjectRegistry.getAllProjects());
    }

    @Override
    public void addIncludedBuilds(Iterable<IncludedBuild> includedBuilds, NestedBuildFactory nestedBuildFactory) {
        registerSubstitutions(registerProjects(includedBuilds, nestedBuildFactory));
    }

    private List<IncludedBuild> registerProjects(Iterable<IncludedBuild> includedBuilds, NestedBuildFactory nestedBuildFactory) {
        List<IncludedBuild> registeredBuilds = Lists.newArrayList();
        for (IncludedBuild includedBuild : includedBuilds) {
            includedBuildRegistry.registerBuild(includedBuild.getProjectDir(), nestedBuildFactory);
            registeredBuilds.add(includedBuild);
            Path rootProjectPath = Path.ROOT.child(includedBuild.getName());
            BuildIdentifier buildIdentifier = new DefaultBuildIdentifier(includedBuild.getName());
            Set<DefaultProjectDescriptor> allProjects = ((IncludedBuildInternal) includedBuild).getLoadedSettings().getProjectRegistry().getAllProjects();
            registerProjects(rootProjectPath, buildIdentifier, allProjects);
        }
        return registeredBuilds;
    }

    private void registerProjects(Path rootPath, BuildIdentifier buildIdentifier, Set<DefaultProjectDescriptor> allProjects) {
        for (DefaultProjectDescriptor project : allProjects) {
            Path projectIdentityPath = rootPath.append(project.path());
            ProjectComponentIdentifier projectComponentIdentifier = DefaultProjectComponentIdentifier.newProjectId(buildIdentifier, project.getPath());
            projectRegistry.add(projectIdentityPath, projectComponentIdentifier);
        }
    }

    private void registerSubstitutions(Iterable<IncludedBuild> includedBuilds) {
        for (IncludedBuild includedBuild : includedBuilds) {
            dependencySubstitutionsBuilder.build((IncludedBuildInternal)includedBuild);
        }
    }

}
