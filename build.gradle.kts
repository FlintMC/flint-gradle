fun RepositoryHandler.flintRepository() {
    maven {
        url = uri("https://git.laby.tech/api/v4/projects/148/packages/maven")
        name = "GitLab"
        credentials(HttpHeaderCredentials::class) {
            name = "Job-Token"
            value = System.getenv("CI_JOB_TOKEN")
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

plugins {
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "net.flintmc"

repositories {
    flintRepository()
    mavenCentral()
}

// 10.0.0 as default, only relevant for local publishing
version = System.getenv().getOrDefault("VERSION", "2.3.2")

dependencies {
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.11.1")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.11.1")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = "2.11.1")
    implementation(group = "org.apache.httpcomponents", name = "httpmime", version = "4.5.12")
    implementation(group = "org.apache.httpcomponents", name = "httpclient", version = "4.5.12")
    implementation(group = "io.github.java-diff-utils", name = "java-diff-utils", version = "4.7")
}

gradlePlugin {
    plugins {
        create("flintGradle") {
            id = "net.flintmc.flint-gradle-plugin"
            implementationClass = "net.flintmc.gradle.FlintGradlePlugin"
        }
    }
}

publishing {
    repositories {
        flintRepository()
    }
}
