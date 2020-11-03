fun RepositoryHandler.flintGradlePluginRepository() {
    val labymediaMavenAuthToken: String? by project

    maven {
        url = uri("https://git.laby.tech/api/v4/projects/148/packages/maven")
        name = "Gitlab"
        credentials(HttpHeaderCredentials::class) {
            if (System.getenv().containsKey("CI_JOB_TOKEN")) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            } else {
                name = "Private-Token"
                value = labymediaMavenAuthToken
            }
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

fun RepositoryHandler.flintRepository() {
    val labymediaMavenAuthToken: String? by project

    maven {
        setUrl("https://git.laby.tech/api/v4/groups/2/-/packages/maven")
        name = "Gitlab"
        credentials(HttpHeaderCredentials::class) {
            if (System.getenv().containsKey("CI_JOB_TOKEN")) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            } else {
                name = "Private-Token"
                value = labymediaMavenAuthToken
            }
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

plugins {
    id("java-gradle-plugin")
    id("maven")
    id("org.jetbrains.kotlin.jvm") version "1.3.72"
}

group = "net.flintmc"

repositories {
    flintRepository()
    flintGradlePluginRepository()
    mavenCentral()
}

// 10.0.0 as default, only relevant for local publishing
version = System.getenv().getOrDefault("VERSION", "2.3.3")

dependencies {
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.11.1")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.11.1")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = "2.11.1")
    implementation(group = "org.apache.httpcomponents", name = "httpmime", version = "4.5.12")
    implementation(group = "org.apache.httpcomponents", name = "httpclient", version = "4.5.12")
    implementation(group = "io.github.java-diff-utils", name = "java-diff-utils", version = "4.7")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.12.0-rc1")
    implementation(group = "net.flintmc.installer", name = "logic-implementation", version = "1.0.5")
}

gradlePlugin {
    plugins {
        create("flintGradle") {
            id = "net.flintmc.flint-gradle-plugin"
            implementationClass = "net.flintmc.gradle.FlintGradlePlugin"
        }
    }
}