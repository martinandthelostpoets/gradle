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

package org.gradle.internal.component.external.model

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.internal.component.external.descriptor.Configuration
import org.gradle.internal.component.external.descriptor.ModuleDescriptorState
import org.gradle.internal.component.external.descriptor.MutableModuleDescriptorState
import org.gradle.internal.component.model.ComponentResolveMetadata
import org.gradle.internal.component.model.DependencyMetadata
import org.gradle.internal.component.model.IvyArtifactName
import org.gradle.internal.component.model.ModuleSource
import org.gradle.internal.hash.HashValue

class DefaultMutableMavenModuleResolveMetadataTest extends AbstractMutableModuleComponentResolveMetadataTest {
    @Override
    AbstractMutableModuleComponentResolveMetadata createMetadata(ModuleComponentIdentifier id, ModuleDescriptorState moduleDescriptor, List<Configuration> configurations, List<DependencyMetadata> dependencies) {
        return new DefaultMutableMavenModuleResolveMetadata(Mock(ModuleVersionIdentifier), id, moduleDescriptor, dependencies)
    }

    @Override
    AbstractMutableModuleComponentResolveMetadata createMetadata(ModuleComponentIdentifier id, Set<IvyArtifactName> artifacts) {
        return new DefaultMutableMavenModuleResolveMetadata(Mock(ModuleVersionIdentifier), id, artifacts)
    }

    def "defines configurations for maven scopes and several usage buckets"() {
        def id = DefaultModuleComponentIdentifier.newId("group", "module", "version")
        def descriptor = new MutableModuleDescriptorState(id, "2", true)

        def metadata = new DefaultMutableMavenModuleResolveMetadata(Mock(ModuleVersionIdentifier), id, descriptor, [])

        expect:
        def immutable = metadata.asImmutable()
        immutable.configurationNames == ["compile", "runtime", "test", "provided", "system", "optional", "master", "default", "javadoc", "sources"] as Set
        immutable.getConfiguration("compile").hierarchy == ["compile"]
        immutable.getConfiguration("runtime").hierarchy == ["runtime", "compile"]
        immutable.getConfiguration("master").hierarchy == ["master"]
        immutable.getConfiguration("test").hierarchy == ["test", "runtime", "compile"]
        immutable.getConfiguration("default").hierarchy == ["default", "runtime", "compile", "master"]
        immutable.getConfiguration("provided").hierarchy == ["provided"]
        immutable.getConfiguration("optional").hierarchy == ["optional"]
    }

    def "default metadata"() {
        def id = DefaultModuleComponentIdentifier.newId("group", "module", "version")
        def metadata = new DefaultMutableMavenModuleResolveMetadata(Mock(ModuleVersionIdentifier), id, [] as Set)

        expect:
        metadata.packaging == 'jar'
        !metadata.relocated
        metadata.snapshotTimestamp == null

        def immutable = metadata.asImmutable()
        immutable.generated
        immutable.packaging == 'jar'
        !immutable.relocated
        immutable.configurationNames == ["compile", "runtime", "test", "provided", "system", "optional", "master", "default", "javadoc", "sources"] as Set
        immutable.variants.empty
    }

    def "initialises values from descriptor state and defaults"() {
        def id = DefaultModuleComponentIdentifier.newId("group", "module", "version")
        def descriptor = new MutableModuleDescriptorState(id, "2", true)

        def vid = Mock(ModuleVersionIdentifier)
        def metadata = new DefaultMutableMavenModuleResolveMetadata(vid, id, descriptor, [])

        expect:
        metadata.componentId == id
        metadata.id == vid
        metadata.status == "2"

        and:
        metadata.source == null
        !metadata.changing
        metadata.statusScheme == ComponentResolveMetadata.DEFAULT_STATUS_SCHEME
        metadata.snapshotTimestamp == null
        metadata.packaging == "jar"
        !metadata.relocated
        metadata.descriptor == descriptor

        and:
        def immutable = metadata.asImmutable()
        immutable != metadata
        immutable.componentId == id
        immutable.source == null
        immutable.id == vid
        immutable.status == "2"
        immutable.statusScheme == ComponentResolveMetadata.DEFAULT_STATUS_SCHEME
        immutable.generated
        !immutable.changing
        immutable.snapshotTimestamp == null
        immutable.packaging == "jar"
        !immutable.relocated

        and:
        def copy = immutable.asMutable()
        copy != metadata
        copy.componentId == id
        copy.source == null
        copy.id == vid
        copy.status == "2"
        copy.statusScheme == ComponentResolveMetadata.DEFAULT_STATUS_SCHEME
        !copy.changing
        copy.snapshotTimestamp == null
        copy.packaging == "jar"
        !copy.relocated
    }

    def "can override values from descriptor"() {
        def id = DefaultModuleComponentIdentifier.newId("group", "module", "version")
        def descriptor = new MutableModuleDescriptorState(id, "2", true)
        def newId = DefaultModuleComponentIdentifier.newId("group", "module", "1.2")
        def source = Stub(ModuleSource)
        def contentHash = new HashValue("123")

        def vid = Mock(ModuleVersionIdentifier)
        def metadata = new DefaultMutableMavenModuleResolveMetadata(vid, id, descriptor, [])

        when:
        metadata.componentId = newId
        metadata.source = source
        metadata.status = "3"
        metadata.changing = true
        metadata.statusScheme = ["1", "2", "3"]
        metadata.snapshotTimestamp = "123"
        metadata.packaging = "pom"
        metadata.relocated = true
        metadata.contentHash = contentHash

        then:
        metadata.componentId == newId
        metadata.id == DefaultModuleVersionIdentifier.newId(newId)
        metadata.source == source
        metadata.changing
        metadata.status == "3"
        metadata.statusScheme == ["1", "2", "3"]
        metadata.snapshotTimestamp == "123"
        metadata.packaging == "pom"
        metadata.relocated
        metadata.contentHash == contentHash

        def immutable = metadata.asImmutable()
        immutable != metadata
        immutable.componentId == newId
        immutable.id == DefaultModuleVersionIdentifier.newId(newId)
        immutable.source == source
        immutable.status == "3"
        immutable.changing
        immutable.statusScheme == ["1", "2", "3"]
        immutable.snapshotTimestamp == "123"
        immutable.packaging == "pom"
        immutable.relocated
        immutable.contentHash == contentHash

        def copy = immutable.asMutable()
        copy != metadata
        copy.componentId == newId
        copy.id == DefaultModuleVersionIdentifier.newId(newId)
        copy.source == source
        copy.status == "3"
        copy.changing
        copy.statusScheme == ["1", "2", "3"]
        copy.snapshotTimestamp == "123"
        copy.packaging == "pom"
        copy.relocated
        copy.contentHash == contentHash
    }

    def "can attach variants"() {
        def id = DefaultModuleComponentIdentifier.newId("group", "module", "version")
        def descriptor = new MutableModuleDescriptorState(id, "2", true)
        def metadata = new DefaultMutableMavenModuleResolveMetadata(Mock(ModuleVersionIdentifier), id, descriptor, [])

        given:
        def v1 = metadata.addVariant("api", attributes(usage: "compile"))
        v1.addFile("f1", "dir/f1")
        v1.addFile("f2.jar", "f2-1.2.jar")
        def v2 = metadata.addVariant("runtime", attributes(usage: "runtime"))
        v2.addFile("f1", "dir/f1")

        def immutable = metadata.asImmutable()

        expect:
        immutable.variants.size() == 2
        immutable.variants[0].name == "api"
        immutable.variants[0].attributes == attributes(usage: "compile")
        immutable.variants[0].files.size() == 2
        immutable.variants[0].files[0].name == "f1"
        immutable.variants[0].files[0].uri == "dir/f1"
        immutable.variants[1].name == "runtime"
        immutable.variants[1].attributes == attributes(usage: "runtime")
        immutable.variants[1].files.size() == 1

        def immutable2 = immutable.asMutable().asImmutable()
        immutable2.variants.size() == 2
        immutable2.variants[0].name == "api"
        immutable2.variants[1].name == "runtime"

        def copy = immutable.asMutable()
        copy.addVariant("link", attributes())

        def immutable3 = copy.asImmutable()
        immutable3.variants.size() == 3
        immutable3.variants[0].name == "api"
        immutable3.variants[1].name == "runtime"
        immutable3.variants[2].name == "link"
        immutable3.variants[2].files.empty
    }

    def "making changes to copy does not affect original"() {
        def id = DefaultModuleComponentIdentifier.newId("group", "module", "version")
        def descriptor = new MutableModuleDescriptorState(id, "2", true)
        def newId = DefaultModuleComponentIdentifier.newId("group", "module", "1.2")
        def source = Stub(ModuleSource)
        def metadata = new DefaultMutableMavenModuleResolveMetadata(Mock(ModuleVersionIdentifier), id, descriptor, [])

        when:
        def immutable = metadata.asImmutable()
        def copy = immutable.asMutable()
        copy.componentId = newId
        copy.source = source
        copy.changing = true
        copy.status = "3"
        copy.statusScheme = ["2", "3"]
        copy.snapshotTimestamp = "123"
        copy.packaging = "pom"
        copy.relocated = true
        def immutableCopy = copy.asImmutable()

        then:
        metadata.componentId == id
        metadata.source == null
        !metadata.changing
        metadata.status == "2"
        metadata.statusScheme == ComponentResolveMetadata.DEFAULT_STATUS_SCHEME
        metadata.snapshotTimestamp == null
        metadata.packaging == "jar"
        !metadata.relocated

        immutable.componentId == id
        immutable.source == null
        !immutable.changing
        immutable.status == "2"
        immutable.statusScheme == ComponentResolveMetadata.DEFAULT_STATUS_SCHEME
        immutable.snapshotTimestamp == null
        immutable.packaging == "jar"
        !immutable.relocated

        copy.componentId == newId
        copy.source == source
        copy.changing
        copy.status == "3"
        copy.statusScheme == ["2", "3"]
        copy.snapshotTimestamp == "123"
        copy.packaging == "pom"
        copy.relocated

        immutableCopy.componentId == newId
        immutableCopy.source == source
        immutableCopy.changing
        immutableCopy.status == "3"
        immutableCopy.statusScheme == ["2", "3"]
        immutableCopy.snapshotTimestamp == "123"
        immutableCopy.packaging == "pom"
        immutableCopy.relocated
    }

    def "making changes to original does not affect copy"() {
        def id = DefaultModuleComponentIdentifier.newId("group", "module", "version")
        def descriptor = new MutableModuleDescriptorState(id, "2", true)
        def newId = DefaultModuleComponentIdentifier.newId("group", "module", "1.2")
        def source = Stub(ModuleSource)
        def metadata = new DefaultMutableMavenModuleResolveMetadata(Mock(ModuleVersionIdentifier), id, descriptor, [])

        when:
        def immutable = metadata.asImmutable()

        metadata.componentId = newId
        metadata.source = source
        metadata.changing = true
        metadata.status = "3"
        metadata.statusScheme = ["1", "2"]
        metadata.snapshotTimestamp = "123"
        metadata.packaging = "pom"
        metadata.relocated = true

        def immutableCopy = metadata.asImmutable()

        then:
        metadata.componentId == newId
        metadata.source == source
        metadata.changing
        metadata.status == "3"
        metadata.statusScheme == ["1", "2"]
        metadata.snapshotTimestamp == "123"
        metadata.packaging == "pom"
        metadata.relocated

        immutable.componentId == id
        immutable.source == null
        !immutable.changing
        immutable.status == "2"
        immutable.statusScheme == ComponentResolveMetadata.DEFAULT_STATUS_SCHEME
        immutable.snapshotTimestamp == null
        immutable.packaging == "jar"
        !immutable.relocated

        immutableCopy.componentId == newId
        immutableCopy.source == source
        immutableCopy.changing
        immutableCopy.status == "3"
        immutableCopy.statusScheme == ["1", "2"]
        immutableCopy.snapshotTimestamp == "123"
        immutableCopy.packaging == "pom"
        immutableCopy.relocated
    }
}
