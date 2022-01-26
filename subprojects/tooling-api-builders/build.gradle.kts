plugins {
    id("gradlebuild.distribution.implementation-java")
}

description = "Provider-side implementation for running tooling model builders"

dependencies {
    implementation(project(":base-services"))
    implementation(project(":base-services-groovy")) // for 'Specs'
    implementation(project(":enterprise-operations"))
    implementation(project(":build-events"))
    implementation(project(":composite-builds"))
    implementation(project(":core"))
    implementation(project(":core-api"))
    implementation(project(":functional"))
    implementation(project(":dependency-management"))
    implementation(project(":launcher"))
    implementation(project(":logging"))
    implementation(project(":messaging"))
    implementation(project(":model-core"))
    implementation(project(":native"))
    implementation(project(":process-services"))
    implementation(project(":reporting"))
    implementation(project(":resources"))
    implementation(project(":testing-base"))
    implementation(project(":testing-jvm"))
    implementation(project(":tooling-api"))
    implementation(project(":workers"))

    implementation(libs.groovy) // for 'Closure'
    implementation(libs.guava)
    implementation(libs.commonsIo)
    implementation(libs.openTest4j) // TODO opentest4j-specific classes should go to the testing-junit-platform project (and probably inject code with services)

    testImplementation(project(":file-collections"))
    testImplementation(project(":platform-jvm"))
    testImplementation(testFixtures(project(":core")))
}

strictCompile {
    ignoreDeprecations()
}
