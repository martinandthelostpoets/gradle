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

package org.gradle.ide.xcode

import org.gradle.ide.xcode.fixtures.AbstractXcodeIntegrationSpec
import org.gradle.ide.xcode.fixtures.XcodebuildExecuter
import org.gradle.ide.xcode.internal.DefaultXcodeProject
import org.gradle.nativeplatform.fixtures.app.CppApp
import org.gradle.nativeplatform.fixtures.app.CppLib
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition

import static org.gradle.ide.xcode.internal.XcodeUtils.toSpaceSeparatedList

class XcodeSingleCppProjectIntegrationTest extends AbstractXcodeIntegrationSpec {
    def "can create xcode project for C++ executable"() {
        given:
        buildFile << """
apply plugin: 'cpp-executable'
"""

        def app = new CppApp()
        app.writeToProject(testDirectory)
        file('src/main/headers/ignore.cpp') << 'broken!'
        file('src/main/cpp/ignore.h') << 'broken!'
        file('src/main/cpp/ignore.swift') << 'broken!'

        when:
        succeeds("xcode")

        then:
        executedAndNotSkipped(":xcodeProject", ":xcodeProjectWorkspaceSettings", ":xcodeSchemeAppExecutable", ":xcodeWorkspace", ":xcodeWorkspaceWorkspaceSettings", ":xcode")

        def project = rootXcodeProject.projectFile
        project.mainGroup.assertHasChildren(['Products', 'build.gradle'] + app.files*.name)
        project.buildConfigurationList.buildConfigurations.name == [DefaultXcodeProject.BUILD_DEBUG, DefaultXcodeProject.BUILD_RELEASE]

        project.targets.size() == 2
        assertTargetIsTool(project.targets[0], 'App', 'app')
        assertTargetIsIndexer(project.targets[1], 'App')

        project.products.children.size() == 1
        project.products.children[0].path == exe("build/exe/main/debug/app").absolutePath
    }

    def "can create xcode project for C++ library"() {
        given:
        buildFile << """
apply plugin: 'cpp-library'
"""

        def lib = new CppLib()
        lib.writeToProject(testDirectory)
        file('src/main/public/ignore.cpp') << 'broken!'
        file('src/main/headers/ignore.cpp') << 'broken!'
        file('src/main/cpp/ignore.h') << 'broken!'
        file('src/main/cpp/ignore.swift') << 'broken!'

        when:
        succeeds("xcode")

        then:
        executedAndNotSkipped(":xcodeProject", ":xcodeSchemeAppSharedLibrary", ":xcodeProjectWorkspaceSettings", ":xcode")

        def project = rootXcodeProject.projectFile
        project.mainGroup.assertHasChildren(['Products', 'build.gradle'] + lib.files*.name)
        project.buildConfigurationList.buildConfigurations.name == [DefaultXcodeProject.BUILD_DEBUG, DefaultXcodeProject.BUILD_RELEASE]

        project.targets.size() == 2
        assertTargetIsDynamicLibrary(project.targets[0], 'App', 'app')
        assertTargetIsIndexer(project.targets[1], 'App')
        project.targets[1].buildConfigurationList.buildConfigurations[0].buildSettings.HEADER_SEARCH_PATHS == toSpaceSeparatedList(file("src/main/public"), file("src/main/headers"))

        project.products.children.size() == 1
        project.products.children[0].path == sharedLib("build/lib/main/debug/app").absolutePath
    }

    @Requires(TestPrecondition.XCODE)
    def "returns meaningful errors from xcode when C++ executable product doesn't have test configured"() {
        useXcodebuildTool()

        given:
        buildFile << """
apply plugin: 'cpp-executable'
"""

        def lib = new CppLib()
        lib.writeToProject(testDirectory)
        succeeds("xcode")

        when:
        def resultDebug = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme("App Executable")
            .fails(XcodebuildExecuter.XcodeAction.TEST)

        then:
        resultDebug.error.contains("Scheme App Executable is not currently configured for the test action.")

        when:
        def resultRelease = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme("App Executable")
            .withConfiguration(DefaultXcodeProject.BUILD_RELEASE)
            .fails(XcodebuildExecuter.XcodeAction.TEST)

        then:
        resultRelease.error.contains("Scheme App Executable is not currently configured for the test action.")

        when:
        def resultRunner = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme("App Executable")
            .withConfiguration(DefaultXcodeProject.TEST_DEBUG)
            .fails(XcodebuildExecuter.XcodeAction.TEST)

        then:
        resultRunner.error.contains("Scheme App Executable is not currently configured for the test action.")
    }

    @Requires(TestPrecondition.XCODE)
    def "returns meaningful errors from xcode when C++ library doesn't have test configured"() {
        useXcodebuildTool()

        given:
        buildFile << """
apply plugin: 'cpp-library'
"""

        def lib = new CppLib()
        lib.writeToProject(testDirectory)
        succeeds("xcode")

        when:
        def resultDebug = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme("App SharedLibrary")
            .fails(XcodebuildExecuter.XcodeAction.TEST)

        then:
        resultDebug.error.contains("Scheme App SharedLibrary is not currently configured for the test action.")

        when:
        def resultRelease = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme("App SharedLibrary")
            .withConfiguration(DefaultXcodeProject.BUILD_RELEASE)
            .fails(XcodebuildExecuter.XcodeAction.TEST)

        then:
        resultRelease.error.contains("Scheme App SharedLibrary is not currently configured for the test action.")

        when:
        def resultRunner = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme("App SharedLibrary")
            .withConfiguration(DefaultXcodeProject.TEST_DEBUG)
            .fails(XcodebuildExecuter.XcodeAction.TEST)

        then:
        resultRunner.error.contains("Scheme App SharedLibrary is not currently configured for the test action.")
    }

    @Requires(TestPrecondition.XCODE)
    def "can build C++ executable from Xcode"() {
        useXcodebuildTool()
        def app = new CppApp()

        given:
        buildFile << """
apply plugin: 'cpp-executable'
"""

        app.writeToProject(testDirectory)
        succeeds("xcode")

        when:
        exe("build/exe/main/debug/App").assertDoesNotExist()
        def resultDebug = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme('App Executable')
            .succeeds()

        then:
        resultDebug.assertTasksExecuted(':compileDebugCpp', ':linkDebug')
        resultDebug.assertTasksNotSkipped(':compileDebugCpp', ':linkDebug')
        exe("build/exe/main/debug/App").exec().out == app.expectedOutput

        when:
        exe("build/exe/main/release/App").assertDoesNotExist()
        def resultRelease = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme('App Executable')
            .withConfiguration(DefaultXcodeProject.BUILD_RELEASE)
            .succeeds()

        then:
        resultRelease.assertTasksExecuted(':compileReleaseCpp', ':linkRelease')
        resultRelease.assertTasksNotSkipped(':compileReleaseCpp', ':linkRelease')
        exe("build/exe/main/release/App").exec().out == app.expectedOutput
    }

    @Requires(TestPrecondition.XCODE)
    def "can build C++ library from Xcode"() {
        useXcodebuildTool()
        def lib = new CppLib()

        given:
        buildFile << """
apply plugin: 'cpp-library'
"""

        lib.writeToProject(testDirectory)
        succeeds("xcode")

        when:
        sharedLib("build/lib/main/debug/App").assertDoesNotExist()
        def resultDebug = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme('App SharedLibrary')
            .succeeds()

        then:
        resultDebug.assertTasksExecuted(':compileDebugCpp', ':linkDebug')
        resultDebug.assertTasksNotSkipped(':compileDebugCpp', ':linkDebug')
        sharedLib("build/lib/main/debug/App").assertExists()

        when:
        sharedLib("build/lib/main/release/App").assertDoesNotExist()
        def resultRelease = xcodebuild
            .withProject(rootXcodeProject)
            .withScheme('App SharedLibrary')
            .withConfiguration(DefaultXcodeProject.BUILD_RELEASE)
            .succeeds()

        then:
        resultRelease.assertTasksExecuted(':compileReleaseCpp', ':linkRelease')
        resultRelease.assertTasksNotSkipped(':compileReleaseCpp', ':linkRelease')
        sharedLib("build/lib/main/release/App").assertExists()
    }

    def "adds new source files in the project"() {
        given:
        buildFile << """
apply plugin: 'cpp-executable'
"""

        when:
        def app = new CppApp()
        app.writeToProject(testDirectory)
        succeeds("xcode")

        then:
        rootXcodeProject.projectFile.mainGroup
            .assertHasChildren(['Products', 'build.gradle'] + app.files*.name)

        when:
        file("src/main/cpp/new.cpp") << "include <iostream>\n"
        succeeds('xcode')

        then:
        rootXcodeProject.projectFile.mainGroup
            .assertHasChildren(['Products', 'build.gradle', 'new.cpp'] + app.files*.name)
    }

    def "removes deleted source files from the project"() {
        given:
        buildFile << """
apply plugin: 'cpp-executable'
"""

        when:
        def app = new CppApp()
        app.writeToProject(testDirectory)
        file("src/main/cpp/old.cpp") << "include <iostream>\n"
        succeeds("xcode")

        then:
        rootXcodeProject.projectFile.mainGroup
            .assertHasChildren(['Products', 'build.gradle', 'old.cpp'] + app.files*.name)

        when:
        file('src/main/cpp/old.cpp').delete()
        succeeds('xcode')

        then:
        rootXcodeProject.projectFile.mainGroup
            .assertHasChildren(['Products', 'build.gradle'] + app.files*.name)
    }

    def "includes source files in a non-default location in C++ executable project"() {
        given:
        buildFile << """
apply plugin: 'cpp-executable'

executable {
    source.from 'Sources'
    privateHeaders.from 'Sources/include'
}
"""

        when:
        def app = new CppApp()
        app.headers.writeToSourceDir(file('Sources/include'))
        app.sources.writeToSourceDir(file('Sources'))
        file("src/main/headers/ignore.h") << 'broken!'
        file('src/main/cpp/ignore.cpp') << 'broken!'
        succeeds("xcode")

        then:
        rootXcodeProject.projectFile.mainGroup
            .assertHasChildren(['Products', 'build.gradle'] + app.files*.name)
    }

    def "includes source files in a non-default location in C++ library project"() {
        given:
        buildFile << """
apply plugin: 'cpp-library'

library {
    source.from 'Sources'
    privateHeaders.from 'Sources/include'
    publicHeaders.from 'Includes'
}
"""

        when:
        def lib = new CppLib()
        lib.publicHeaders.writeToSourceDir(file('Includes'))
        lib.privateHeaders.writeToSourceDir(file('Sources/include'))
        lib.sources.writeToSourceDir(file('Sources'))
        file('src/main/headers/ignore.h') << 'broken!'
        file('src/main/public/ignore.h') << 'broken!'
        file('src/main/cpp/ignore.cpp') << 'broken!'
        succeeds("xcode")

        then:
        rootXcodeProject.projectFile.mainGroup
            .assertHasChildren(['Products', 'build.gradle'] + lib.files*.name)
    }

    def "honors changes to executable output locations"() {
        given:
        buildFile << """
apply plugin: 'cpp-executable'
buildDir = 'output'
executable.baseName = 'test_app'
"""

        def app = new CppApp()
        app.writeToProject(testDirectory)

        when:
        succeeds("xcode")

        then:
        executedAndNotSkipped(":xcodeProject", ":xcodeProjectWorkspaceSettings", ":xcodeSchemeAppExecutable", ":xcodeWorkspace", ":xcodeWorkspaceWorkspaceSettings", ":xcode")

        def project = rootXcodeProject.projectFile
        project.targets.size() == 2

        project.targets[0].name == 'App Executable'
        project.targets[0].productReference.path == exe("output/exe/main/debug/test_app").absolutePath
        project.targets[0].buildConfigurationList.buildConfigurations.name == [DefaultXcodeProject.BUILD_DEBUG, DefaultXcodeProject.BUILD_RELEASE]
        project.targets[0].buildConfigurationList.buildConfigurations[0].buildSettings.CONFIGURATION_BUILD_DIR == file("output/exe/main/debug").absolutePath
        project.targets[0].buildConfigurationList.buildConfigurations[1].buildSettings.CONFIGURATION_BUILD_DIR == file("output/exe/main/release").absolutePath

        project.products.children.size() == 1
        project.products.children[0].path == exe("output/exe/main/debug/test_app").absolutePath
    }

    def "honors changes to library output locations"() {
        given:
        buildFile << """
apply plugin: 'cpp-library'
buildDir = 'output'
library.baseName = 'test_lib'
"""

        def lib = new CppLib()
        lib.writeToProject(testDirectory)

        when:
        succeeds("xcode")

        then:
        executedAndNotSkipped(":xcodeProject", ":xcodeSchemeAppSharedLibrary", ":xcodeProjectWorkspaceSettings", ":xcode")

        def project = rootXcodeProject.projectFile
        project.targets.size() == 2

        project.targets[0].name == 'App SharedLibrary'
        project.targets[0].productReference.path == sharedLib("output/lib/main/debug/test_lib").absolutePath
        project.targets[0].buildConfigurationList.buildConfigurations.name == [DefaultXcodeProject.BUILD_DEBUG, DefaultXcodeProject.BUILD_RELEASE]
        project.targets[0].buildConfigurationList.buildConfigurations[0].buildSettings.CONFIGURATION_BUILD_DIR == file("output/lib/main/debug").absolutePath
        project.targets[0].buildConfigurationList.buildConfigurations[1].buildSettings.CONFIGURATION_BUILD_DIR == file("output/lib/main/release").absolutePath

        project.products.children.size() == 1
        project.products.children[0].path == sharedLib("output/lib/main/debug/test_lib").absolutePath
    }
}
