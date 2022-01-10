/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.integtests.resolve.derived

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class AmbiguousVariantSelectionTest extends AbstractIntegrationSpec {

    def setup() {
        multiProjectBuild('root', ['producer', 'consumer']) {
            buildFile << '''
            '''

            file('producer/build.gradle') << """
                group = 'org.test'
                version = '1.0'

                configurations {
                    def attrA = Attribute.of('A', String)
                    def attrB = Attribute.of('B', String)

                    fooElements {
                        canBeResolved = false
                        canBeConsumed = true
                        attributes {
                            attribute(attrA, '1')
                            attribute(attrB, '2')
                        }

                        outgoing {
                            artifact(layout.projectDirectory.file(project.name + '-foo.txt'))
                        }
                    }

                    barElements {
                        canBeResolved = false
                        canBeConsumed = true
                        attributes {
                            attribute(attrA, '2')
                            attribute(attrB, '1')
                        }

                        outgoing {
                            artifact(layout.projectDirectory.file(project.name + '-bar.txt'))
                        }
                    }
                }
            """

            file('consumer/build.gradle') << """

                def attrA = Attribute.of('A', String)
                def attrB = Attribute.of('B', String)

                class AmbiguousCompatibilityRule implements AttributeCompatibilityRule<String> {
                    void execute(CompatibilityCheckDetails<String> details) {
                        details.compatible()
                    }
                }

                dependencies.attributesSchema {
                    attribute(attrA) {
                        compatibilityRules.add(AmbiguousCompatibilityRule)
                    }
                    attribute(attrB) {
                        compatibilityRules.add(AmbiguousCompatibilityRule)
                    }
                }

                configurations {
                    producerArtifacts {
                        attributes {
                            attribute(attrA, '1') // adjust these to select a different variant
                            attribute(attrB, '1') // adjust these to select a different variant
                        }
                    }
                }

                dependencies {
                    producerArtifacts project(':producer')
                }

                abstract class Resolve extends DefaultTask {
                    @InputFiles
                    abstract ConfigurableFileCollection getArtifacts()
                    @Internal
                    List<String> expectations = []
                    @TaskAction
                    void assertThat() {
                        logger.lifecycle 'Found files: {}', artifacts.files*.name
                        assert artifacts.files*.name == expectations
                    }
                }

                tasks.register('resolve', Resolve) {
                    artifacts.from(configurations.producerArtifacts)
                }

                tasks.register('resolveAmbiguous', Resolve) {
                    artifacts.from(configurations.producerArtifacts.incoming.artifactView {
                        attributes {
                            attribute(attrA, '2')
                            attribute(attrB, '1')
                        }
                    }.files)
                }
            """
        }
    }

    def 'resolves ambiguous configuration'() {
        when:
        file('consumer/build.gradle') << '''
            resolve {
                expectations = [ 'producer-foo.txt' ] // TODO really?
            }
        '''

        then:
        succeeds ':consumer:resolve'
//        succeeds ':consumer:resolveAmbiguous'
    }
}
