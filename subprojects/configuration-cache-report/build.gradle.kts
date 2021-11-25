plugins {
    id("gradlebuild.internal.kotlin-js")
    id("gradlebuild.configuration-cache-report")
}

description = "Configuration cache problems HTML report"

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink>().configureEach {
    doFirst {
        println("filteredArgumentsMap: $filteredArgumentsMap")
    }
}
