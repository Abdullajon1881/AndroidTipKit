plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
    alias(libs.plugins.dokka)
}

group = providers.gradleProperty("nudgekitGroup").get()
version = providers.gradleProperty("nudgekitVersion").get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

// Real API docs (Dokka, javadoc format) packaged as the -javadoc.jar required
// by Maven Central. Output dir is build/dokka/javadoc.
val javadocJar by tasks.registering(Jar::class) {
    dependsOn("dokkaJavadoc")
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("dokka/javadoc"))
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // `api`, not `implementation`: the public API exposes coroutine types
    // (ReactiveTipManager returns kotlinx.coroutines.flow.Flow, and the whole
    // manager/evaluator surface is `suspend`). Exposing it as `api` puts
    // coroutines-core on the consumer's compile classpath, so external users of
    // this Android-free core can call the reactive/suspend API without manually
    // adding coroutines themselves.
    api(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    // Parses the shared cross-language rule vectors in ParityVectorTest.
    testImplementation("org.json:json:20240303")
}

// Expose the shared rule-vector directory (repo-root /spec/rule-vectors) to the
// parity tests, regardless of the test working directory.
tasks.withType<Test>().configureEach {
    systemProperty(
        "nudgekit.specDir",
        rootProject.layout.projectDirectory.dir("spec/rule-vectors").asFile.absolutePath,
    )
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(javadocJar)
            artifactId = "nudgekit-core"
            pom {
                name.set("NudgeKit Core")
                description.set(
                    "Android-free rule engine and models for NudgeKit — contextual " +
                        "tips, feature discovery, and onboarding nudges.",
                )
                url.set("https://github.com/Abdullajon1881/AndroidTipKit")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("Abdullajon1881")
                        name.set("Abdullajon1881")
                        url.set("https://github.com/Abdullajon1881")
                    }
                }
                scm {
                    url.set("https://github.com/Abdullajon1881/AndroidTipKit")
                    connection.set("scm:git:https://github.com/Abdullajon1881/AndroidTipKit.git")
                    developerConnection.set("scm:git:ssh://git@github.com/Abdullajon1881/AndroidTipKit.git")
                }
            }
        }
    }
}

// In-memory PGP signing, gated so it only activates when key material is
// supplied (via -P / ORG_GRADLE_PROJECT_ env vars). Without keys, signing is
// skipped entirely so normal builds and CI stay green. No keyring on disk.
signing {
    val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
    val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword").orNull
    isRequired = signingKey != null
    if (signingKey != null) {
        val signingKeyId = providers.gradleProperty("signingInMemoryKeyId").orNull
        if (signingKeyId != null) {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        } else {
            useInMemoryPgpKeys(signingKey, signingPassword)
        }
        sign(publishing.publications)
    }
}
