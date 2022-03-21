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

package org.gradle.api.provider

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class ListPropertyIntegrationTest extends AbstractIntegrationSpec {
    def "Adding a provider returning non-null to a list property works"() {
        given:
        buildFile << """
            abstract class MyTask extends DefaultTask {
                @Input
                abstract ListProperty<String> getMyProperty()

                MyTask() {
                    myProperty.set(['a'])
                }

                @TaskAction
                void printText() {
                    println myProperty.get().join(' ')
                }
            }

            tasks.register("myTask", MyTask) {
                myProperty.add(project.provider(() -> 'b'))
            }
        """.stripIndent()

        expect:
        succeeds('myTask')
        result.getGroupedOutput().task(':myTask').assertOutputContains('a b')
    }

    def "Adding a provider returning null to a list property results in a misleading error message"() {
        given:
        buildFile << """
            abstract class MyTask extends DefaultTask {
                @Input
                abstract ListProperty<String> getMyProperty()

                MyTask() {
                    myProperty.set(['a'])
                }

                @TaskAction
                void printText() {
                    println myProperty.get().join(' ')
                }
            }

            tasks.register("myTask", MyTask) {
                myProperty.add(project.provider(() -> null))
            }
        """.stripIndent()

        expect:
        fails('myTask')
        /* Yields:
        A problem was found with the configuration of task ':myTask' (type 'MyTask').
          - Type 'MyTask' property 'myProperty' doesn't have a configured value.

            Reason: This property isn't marked as optional and no value has been configured.

            Possible solutions:
              1. Assign a value to 'myProperty'.
              2. Mark property 'myProperty' as optional.

            Please refer to https://docs.gradle.org/7.5-20220321040000+0000/userguide/validation_problems.html#value_not_set for more details about this problem.

        * Try:
        > Run with --info or --debug option to get more log output.
        > Run with --scan to get full insights.

        * Exception is:
        org.gradle.internal.execution.WorkValidationException: A problem was found with the configuration of task ':myTask' (type 'MyTask').
         */
    }
}
