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

package org.gradle.tooling;

import org.gradle.api.Incubating;

import java.util.Collection;

/**
 * Provides infrastructure to select which test classes, methods, and packages will be included in the test execution.
 * <p>
 * A complex example:
 * <pre>
 * TestLauncher testLauncher = projectConnection.newTestLauncher();
 * testLauncher.withTestsFor(spec -&gt; {
 *     spec.forTaskPath(":test")                                    // configure the test selection on the ':test' task
 *         .includePackage("org.pkg")                               // execute all tests declared the org.pkg package and its sub-packages
 *         .includeClass("com.MyTest")                              // execute the MyTest test class
 *         .includeMethod("com.OtherTest", Arrays.asList("verify")) // execute the OtherTest.verify() test method
 *         .includePattern("io.*")                                  // execute all tests matching to io.*
 * }).run();
 * </pre>
 * <p>
 * Test classes and methods accept patterns. Patterns follow the rules of <a href="https://docs.gradle.org/current/userguide/java_testing.html#test_filtering">Test Filtering</a>.
 * <p>
 * The test execution will fail if any of the selected classes, methods, or patters have no matching tests.
 *
 * @since 7.5
 */
@Incubating
public interface TestPatternSpec {
    TestPatternSpec includePackage(String pkg);
    TestPatternSpec includePackages(Collection<String> packages);
    TestPatternSpec includeClass(String cls);
    TestPatternSpec includeClasses(Collection<String>  classes);
    TestPatternSpec includeMethod(String clazz, String method);
    TestPatternSpec includeMethods(String clazz, Collection<String> methods);
    TestPatternSpec includePattern(String pattern);
    TestPatternSpec includePatterns(Collection<String>  pattern);
}
