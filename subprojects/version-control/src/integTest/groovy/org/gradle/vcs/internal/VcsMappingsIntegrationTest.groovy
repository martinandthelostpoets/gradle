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

package org.gradle.vcs.internal

class VcsMappingsIntegrationTest extends AbstractVcsIntegrationTest {
    def setup() {
        settingsFile << """
            import ${DirectoryRepositorySpec.canonicalName}
        """
    }

    def "can define and use source repositories"() {
        settingsFile << """
            sourceControl {
                vcsMappings {
                    withModule("org.test:dep") {
                        from vcs(DirectoryRepositorySpec) {
                            sourceDir = file("dep")
                        }
                    }
                }
            }
        """
        expect:
        succeeds("assemble")
        assertRepoCheckedOut()
    }

    def "only use source repositories when version matches latest.integration"() {
        settingsFile << """
            sourceControl {
                vcsMappings {
                    withModule("org.test:dep") {
                        from vcs(DirectoryRepositorySpec) {
                            sourceDir = file("dep")
                        }
                    }
                }
            }
        """
        buildFile.text = buildFile.text.replace("latest.integration", "1.0")
        expect:
        fails("assemble")
        assertRepoNotCheckedOut("dep")
    }

    def "can define and use source repositories with all {}"() {
        settingsFile << """
            sourceControl {
                vcsMappings {
                    addRule("rule") { details ->
                        if (details.requested.group == "org.test") {
                            from vcs(DirectoryRepositorySpec) {
                                sourceDir = file("dep")
                            }
                        }
                    }
                }
            }
        """
        expect:
        succeeds("assemble")
        assertRepoCheckedOut()
    }

    def "can define unused vcs mappings"() {
        settingsFile << """
            // include the missing dep as a composite
            includeBuild 'dep'
            
            sourceControl {
                vcsMappings {
                    withModule("unused:dep") {
                        from vcs(DirectoryRepositorySpec) {
                            sourceDir = file("does-not-exist")
                        }
                    }
                    addRule("rule") { details ->
                        if (details instanceof ModuleVersionSelector && details.requested.group == "unused") {
                            from vcs(DirectoryRepositorySpec) {
                                sourceDir = file("does-not-exist")
                            }
                        }
                    }
                }
            }
        """
        expect:
        succeeds("assemble")
        assertRepoNotCheckedOut()
        assertRepoNotCheckedOut("does-not-exist")
    }

    def "last vcs mapping rule wins"() {
        settingsFile << """
            sourceControl {
                vcsMappings {
                    withModule("org.test:dep") {
                        from vcs(DirectoryRepositorySpec) {
                            sourceDir = file("does-not-exist")
                        }
                    }
                    withModule("org.test:dep") {
                        from vcs(DirectoryRepositorySpec) {
                            sourceDir = file("dep")
                        }
                    }
                }
            }
        """
        expect:
        succeeds("assemble")
        assertRepoCheckedOut()
        assertRepoNotCheckedOut("does-not-exist")
    }

    void assertRepoCheckedOut(String repoName="dep") {
        def checkout = checkoutDir(SimpleVersionControlSystem, repoName, "fixed", file(repoName).absolutePath)
        checkout.file("checkedout").assertIsFile()
    }

    void assertRepoNotCheckedOut(String repoName="dep") {
        def checkout = checkoutDir(SimpleVersionControlSystem, repoName, "fixed", file(repoName).absolutePath)
        checkout.file("checkedout").assertDoesNotExist()
    }
}
