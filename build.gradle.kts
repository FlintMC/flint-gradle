fun RepositoryHandler.flintRepository() {
    var distributorUrl = System.getenv("FLINT_DISTRIBUTOR_URL")

    if (distributorUrl == null) {
        distributorUrl = project.properties.getOrDefault(
            "net.flintmc.distributor.url",
            "https://dist.labymod.net/api/v1/maven/release"
        ).toString()
    }

    var bearerToken = System.getenv("FLINT_DISTRIBUTOR_BEARER_TOKEN")

    if (bearerToken == null) {
        bearerToken = project.properties["net.flintmc.distributor.bearer-token"].toString()
    }

    maven {
        setUrl(distributorUrl)
        name = "Flint"
        credentials(HttpHeaderCredentials::class) {
            name = "Authorization"
            value = "Bearer $bearerToken"
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
    id("maven-publish")
    id("maven")
}

group = "net.flintmc"

repositories {
    mavenLocal()
    flintRepository()
    mavenCentral()
}

version = System.getenv().getOrDefault("VERSION", "2.7.1")

dependencies {
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.11.1")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.11.1")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = "2.11.1")
    implementation(group = "com.squareup.okhttp3", name = "okhttp", version = "4.10.0-RC1")
    implementation(group = "io.github.java-diff-utils", name = "java-diff-utils", version = "4.7")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.12.0-rc1")
    implementation(group = "net.flintmc.installer", name = "logic-implementation", version = "1.1.5")
    implementation(group = "net.flintmc.installer", name = "logic", version = "1.1.5")
}

gradlePlugin {
    plugins {
        create("FlintGradle") {
            id = "net.flintmc.flint-gradle"
            implementationClass = "net.flintmc.gradle.FlintGradlePlugin"
        }
    }
}

publishing {
    repositories {
        flintRepository()
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}
