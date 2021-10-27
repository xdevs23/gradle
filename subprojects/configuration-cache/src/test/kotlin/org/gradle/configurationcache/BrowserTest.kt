/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.configurationcache

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import org.openqa.selenium.remote.CapabilityType
import org.testcontainers.Testcontainers.exposeHostPorts
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.DefaultRecordingFileFactory
import org.testcontainers.containers.VncRecordingContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.lifecycle.TestDescription
import java.io.File
import java.util.logging.Level

@Testcontainers
@ExtendWith(JUnit5VncRecorder::class)
class BrowserTest {
    val browserContainer: BrowserWebDriverContainer<Nothing> = BrowserWebDriverContainer<Nothing>().apply {
        withCapabilities(
            ChromeOptions().apply {
                val loggingPrefs = LoggingPreferences().apply {
                    enable(LogType.BROWSER, Level.ALL)
                }
                setCapability(CapabilityType.LOGGING_PREFS, loggingPrefs)
                // https://bugs.chromium.org/p/chromedriver/issues/detail?id=2976
                setCapability("goog:loggingPrefs", loggingPrefs)
                // https://stackoverflow.com/questions/40654358/how-to-control-the-download-of-files-with-selenium-python-bindings-in-chrome/40656336#40656336
            }
        )
        withRecordingFileFactory(DefaultRecordingFileFactory())
        withRecordingMode(
            BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL,
            File(System.getProperty("user.dir"), "build"),
            VncRecordingContainer.VncRecordingFormat.MP4
        )
        withRecordingFileFactory(DefaultRecordingFileFactory())
    }

    @Test
    fun clickButton() {
        exposeHostPorts(8080)
        browserContainer.start()
        val webDriver = browserContainer.webDriver
        webDriver.get("http://host.testcontainers.internal:8080")

        Thread.sleep(1000)
        webDriver.findElement(By.id("ClickMe")).click()
        Thread.sleep(1000)
        Assertions.assertTrue(webDriver.findElement(By.tagName("body")).text.contains("Clicked!"))
        // sleep so we can see it clearly in the recording video
        Thread.sleep(1000)
    }

    @AfterEach
    fun tearDown() {
        browserContainer.stop()
    }
}

/**
 * This is a workaround for
 * https://github.com/testcontainers/testcontainers-java/issues/1341
 *
 * The annotated class must have an instance field
 * BrowserWebDriverContainer browswerWebDriverContainer or
 * List[BrowserWebDriverContainer] browserWebDriverContainers
 *
 */
@Suppress("UNCHECKED_CAST")
class JUnit5VncRecorder : AfterTestExecutionCallback {
    private fun getBrowserContainer(testInstance: Any): BrowserWebDriverContainer<Nothing> {
        return (testInstance as BrowserTest).browserContainer
    }

    private fun toTestDescription(context: ExtensionContext): TestDescription {
        return object : TestDescription {
            override fun getTestId() = context.displayName

            override fun getFilesystemFriendlyName(): String {
                return "${context.testInstance.get().javaClass.simpleName}-${context.testMethod.get().name.replace(' ', '-')}"
            }
        }
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val browserContainer = getBrowserContainer(context.testInstance.get())
        browserContainer.afterTest(toTestDescription(context), context.executionException)
    }
}
