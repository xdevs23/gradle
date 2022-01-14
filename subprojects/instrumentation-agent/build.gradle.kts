plugins {
    id("gradlebuild.distribution.implementation-java")  // TODO(mlopatkin) I don't know how to do it right
    id("gradlebuild.shaded-jar")
}

shadedJar {
    keepPackages.set(listOf("org.gradle"))
    unshadedPackages.set(listOf("org.gradle"))
}

dependencies {
    compileOnly(project(":base-instrumentation"))

    implementation(libs.bytebuddy)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            mapOf(
                "Premain-Class" to "org.gradle.instrumentation.agent.Agent",
                "Can-Redefine-Classes" to "true",
                "Can-Retransform-Classes" to "true"
            )
        )
    }
}
