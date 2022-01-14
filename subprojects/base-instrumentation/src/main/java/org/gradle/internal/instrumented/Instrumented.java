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

package org.gradle.internal.instrumented;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class Instrumented {
    private static final Listener NO_OP = new Listener() {
        @Override
        public void systemPropertyQueried(String key, Object value, String consumer) {
        }

        @Override
        public void envVariableQueried(String key, String value, String consumer) {
        }

        @Override
        public void externalProcessStarted(String command, String consumer) {
        }

        @Override
        public void fileOpened(File file, String consumer) {
        }
    };

    private static final AtomicReference<Listener> LISTENER = new AtomicReference<>(NO_OP);

    public static void setListener(Listener listener) {
        LISTENER.set(listener);
    }

    public static void discardListener() {
        setListener(NO_OP);
    }

    private static Listener listener() {
        if (disableForThread.get()) {
            return NO_OP;
        }
        return LISTENER.get();
    }

    public interface Listener {
        /**
         * @param consumer The name of the class that is reading the property value
         */
        void systemPropertyQueried(String key, Object value, String consumer);

        /**
         * Invoked when the code reads the environment variable.
         *
         * @param key the name of the variable
         * @param value the value of the variable
         * @param consumer the name of the class that is reading the variable
         */
        void envVariableQueried(String key, String value, String consumer);

        /**
         * Invoked when the code starts an external process. The command string with all argument is provided for reporting but its value may not be suitable to actually invoke the command because all
         * arguments are joined together (separated by space) and there is no escaping of special characters.
         *
         * @param command the command used to start the process (with arguments)
         * @param consumer the name of the class that is starting the process
         */
        void externalProcessStarted(String command, String consumer);

        /**
         * Invoked when the code opens a file.
         *
         * @param file the absolute file that was open
         * @param consumer the name of the class that is opening the file
         */
        void fileOpened(File file, String consumer);
    }

    public static void onProcessStarted(List<String> command) {
        listener().externalProcessStarted(String.join(" ", command), null);
    }

    public static void onSystemPropertyQueried(String key, Object value) {
        listener().systemPropertyQueried(key, value, null);
    }

    public static void onEnvironmentVariableQueried(String key, String value) {
        listener().envVariableQueried(key, value, null);
    }

    public static Map<String, String> getenv(Map<String, String> original) {
        if (disableForThread.get()) {
            return original;
        }
        return new AccessTrackingEnvMap(original, Instrumented::onEnvironmentVariableQueried);
    }

    public static Properties getProperties(Properties original) {
        if (disableForThread.get()) {
            return original;
        }
        return new AccessTrackingProperties(original, Instrumented::onSystemPropertyQueried);
    }

    public static void onFileOpened(File file) {
        listener().fileOpened(file, null);
    }

    @SuppressWarnings("try")
    public static void onFileOpened(String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            try (DisabledForThread ignored = withInstrumentationDisabled()) {
                file = new File(System.getProperty("user.dir"), path);
            }
        }
        listener().fileOpened(file, null);
    }

    private static final ThreadLocal<Boolean> disableForThread = ThreadLocal.withInitial(() -> false);

    public interface DisabledForThread extends Closeable {
        @Override
        void close();
    }

    public static DisabledForThread withInstrumentationDisabled() {
        disableForThread.set(true);
        return DISABLER;
    }

    private static final DisabledForThread DISABLER = () -> disableForThread.set(false);
}
