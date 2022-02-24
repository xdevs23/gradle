/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.api.tasks

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.BuildOperationsFixture
import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.internal.execution.timeout.impl.DefaultTimeoutHandler
import org.gradle.test.fixtures.file.LeaksFileHandles
import org.gradle.test.fixtures.file.TestFile
import org.gradle.util.GradleVersion
import spock.lang.IgnoreIf

import java.time.Duration

import static org.gradle.cache.internal.VersionSpecificCacheCleanupFixture.MarkerFileType.NOT_USED_WITHIN_30_DAYS
import static org.gradle.cache.internal.VersionSpecificCacheCleanupFixture.MarkerFileType.USED_TODAY

// https://github.com/gradle/gradle-private/issues/3433
@IgnoreIf({ GradleContextualExecuter.isNoDaemon() })
class TaskTimeoutIntegrationTest extends AbstractIntegrationSpec implements org.gradle.cache.internal.GradleUserHomeCleanupFixture {

    private static final TIMEOUT = 10000

    long postTimeoutCheckFrequencyMs = Duration.ofMinutes(3).toMillis()
    long slowStopLogStacktraceFrequencyMs = Duration.ofMinutes(3).toMillis()

    def operations = new BuildOperationsFixture(executer, temporaryFolder)

    def setup() {
        executer.beforeExecute {
            [
                (DefaultTimeoutHandler.POST_TIMEOUT_CHECK_FREQUENCY_PROPERTY): postTimeoutCheckFrequencyMs,
                (DefaultTimeoutHandler.SLOW_STOP_LOG_STACKTRACE_FREQUENCY_PROPERTY): slowStopLogStacktraceFrequencyMs
            ].each { k, v ->
                executer.withArgument("-D$k=$v".toString())
            }
        }
    }

    @LeaksFileHandles
    // TODO https://github.com/gradle/gradle-private/issues/1532
    def "timeout stops long running work items with #isolationMode isolation"() {
        given:
        if (isolationMode == 'process') {
            // worker starting threads can be interrupted during worker startup and cause a 'Could not initialise system classpath' exception.
            // See: https://github.com/gradle/gradle/issues/8699
            executer.withStackTraceChecksDisabled()
        }
        buildFile << """
            import java.util.concurrent.CountDownLatch
            import java.util.concurrent.TimeUnit
            import org.gradle.workers.WorkParameters

            task block(type: WorkerTask) {
                timeout = Duration.ofMillis($TIMEOUT)
            }

            abstract class WorkerTask extends DefaultTask {

                @Inject
                abstract WorkerExecutor getWorkerExecutor()

                @TaskAction
                void executeTask() {
                    for (int i = 0; i < 100; i++) {
                        workerExecutor.${isolationMode}Isolation().submit(BlockingWorkAction) { }
                    }
                }
            }

            abstract class BlockingWorkAction implements WorkAction<WorkParameters.None> {
                public void execute() {
                    System.out.println("about to wait");
                    new CountDownLatch(1).await(30, TimeUnit.SECONDS);
                    System.out.println("Still working?");
                }
            }
            """

        expect:
        1.times {
            fails "block", "--info"
            failure.assertHasDescription("Execution failed for task ':block'.")
            failure.assertHasCause("Timeout has been exceeded")
            if (isolationMode == 'process' && failure.output.contains("Caused by:")) {
                assert failure.output.contains("Error occurred during initialization of VM")
            }
        }

        where:
        isolationMode << ['process']
    }

    def "cleans up unused version-specific cache directories and corresponding distributions"() {
        given:
        requireOwnGradleUserHomeDir() // because we delete caches and distributions

        and:
        def oldButRecentlyUsedVersion = GradleVersion.version("1.4.5")
        def oldButRecentlyUsedCacheDir = createVersionSpecificCacheDir(oldButRecentlyUsedVersion, USED_TODAY)
        def oldButRecentlyUsedDist = createDistributionChecksumDir(oldButRecentlyUsedVersion).parentFile
        def oldButRecentlyUsedCustomDist = createCustomDistributionChecksumDir("my-dist-1", oldButRecentlyUsedVersion).parentFile

        def oldNotRecentlyUsedVersion = GradleVersion.version("2.3.4")
        def oldNotRecentlyUsedCacheDir = createVersionSpecificCacheDir(oldNotRecentlyUsedVersion, NOT_USED_WITHIN_30_DAYS)
        def oldNotRecentlyUsedDist = createDistributionChecksumDir(oldNotRecentlyUsedVersion).parentFile
        def oldNotRecentlyUsedCustomDist = createCustomDistributionChecksumDir("my-dist-2", oldNotRecentlyUsedVersion).parentFile

        def currentCacheDir = createVersionSpecificCacheDir(GradleVersion.current(), NOT_USED_WITHIN_30_DAYS)
        def currentDist = createDistributionChecksumDir(GradleVersion.current()).parentFile

        when:
        succeeds("help")

        then:
        false

        oldButRecentlyUsedCacheDir.assertExists()
        oldButRecentlyUsedDist.assertExists()
        oldButRecentlyUsedCustomDist.assertExists()

        oldNotRecentlyUsedCacheDir.assertDoesNotExist()
        oldNotRecentlyUsedDist.assertDoesNotExist()
        oldNotRecentlyUsedCustomDist.assertDoesNotExist()

        currentCacheDir.assertExists()
        currentDist.assertExists()

        getGcFile(currentCacheDir).assertExists()
    }

    @Override
    TestFile getGradleUserHomeDir() {
        return executer.gradleUserHomeDir
    }
}
