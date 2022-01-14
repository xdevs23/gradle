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

package org.gradle.instrumentation.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import org.gradle.internal.instrumented.Instrumented;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Map;
import java.util.Properties;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

public class Agent {
    public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
        doMain(inst);
    }

    static void doMain(Instrumentation inst) throws UnmodifiableClassException {
        new AgentBuilder.Default()
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .with(AgentBuilder.RedefinitionStrategy.Listener.StreamWriting.toSystemError())
            .with(AgentBuilder.Listener.StreamWriting.toSystemError().withTransformationsOnly())
            .with(AgentBuilder.InstallationListener.StreamWriting.toSystemError())
            .ignore(none())
            .ignore(
                nameStartsWith("net.bytebuddy.")
                    .or(nameStartsWith("jdk.internal.reflect."))
                    .or(nameStartsWith("java.lang.invoke."))
                    .or(nameStartsWith("com.sun.proxy."))
            )
            .disableClassFormatChanges()
            .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
            .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
            .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
            .type(named("java.lang.ProcessBuilder"))
            .transform((builder, typeDescription, classLoader, module) ->
                builder.visit(Advice.to(ProcessBuilderStartAdvice.class).on(named("start").and(takesNoArguments())))
            )
            .type(named("java.lang.System"))
            .transform((builder, typeDescription, classLoader, module) ->
                builder
                    .visit(Advice.to(SystemGetEnvStringAdvice.class).on(named("getenv").and(takesArgument(0, String.class))))
                    .visit(Advice.to(SystemGetEnvAdvice.class).on(named("getenv").and(takesNoArguments())))
                    .visit(Advice.to(SystemGetProperties.class).on(named("getProperties")))
                    .visit(Advice.to(SystemGetPropertyStringAdvice.class).on(named("getProperty").and(takesArgument(0, String.class))))
            )
            .type(named("java.io.FileInputStream"))
            .transform((builder, typeDescription, classLoader, module) ->
                builder
                    .visit(Advice.to(FisFileAdvice.class).on(isConstructor().and(takesArguments(File.class))))
                    .visit(Advice.to(FisStringAdvice.class).on(isConstructor().and(takesArguments(String.class))))
            )
            .installOn(inst);
        inst.retransformClasses(ProcessBuilder.class, System.class);
    }

    static class SystemGetEnvAdvice {
        @SuppressWarnings("UnusedAssignment")
        @Advice.OnMethodExit
        static void getenv(@Advice.Return(readOnly = false) Map<String, String> env) {
            env = Instrumented.getenv(env);
        }
    }

    static class SystemGetEnvStringAdvice {
        @Advice.OnMethodExit
        static void getenv(@Advice.Argument(0) String key, @Advice.Return String value) {
            Instrumented.onEnvironmentVariableQueried(key, value);
        }
    }

    static class SystemGetProperties {
        @SuppressWarnings("UnusedAssignment")
        @Advice.OnMethodExit
        static void getenv(@Advice.Return(readOnly = false) Properties properties) {
            properties = Instrumented.getProperties(properties);
        }
    }

    static class SystemGetPropertyStringAdvice {
        @Advice.OnMethodExit
        static void getProperty(@Advice.Argument(0) String key, @Advice.Return Object value) {
            Instrumented.onSystemPropertyQueried(key, value);
        }
    }

    static class ProcessBuilderStartAdvice {
        @Advice.OnMethodExit
        static void startEnter(@Advice.This ProcessBuilder pb) {
            Instrumented.onProcessStarted(pb.command());
        }
    }

    static class FisFileAdvice {
        @Advice.OnMethodExit
        static void constructorExit(@Advice.Argument(0) File file) {
            Instrumented.onFileOpened(file);
        }
    }

    static class FisStringAdvice {
        @Advice.OnMethodExit
        static void constructorExit(@Advice.Argument(0) String file) {
            Instrumented.onFileOpened(file);
        }
    }
}
