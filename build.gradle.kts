/*
 * FlintMC
 * Copyright (C) 2020-2021 LabyMedia GmbH and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

fun RepositoryHandler.flintRepository() {
    var distributorUrl = System.getenv("FLINT_DISTRIBUTOR_URL")

    if (distributorUrl == null) {
        distributorUrl = project.properties.getOrDefault(
            "net.flintmc.distributor.url",
            "https://dist.labymod.net"
        ).toString() + "/api/v1/maven/release"
    }

    var bearerToken = System.getenv("FLINT_DISTRIBUTOR_BEARER_TOKEN")

    if (bearerToken == null) {
        bearerToken = project.properties["net.flintmc.distributor.bearer-token"].toString()
    }

    maven {
        setUrl(distributorUrl)
        name = "Flint"

        if(bearerToken != null) {
            authentication {
                create<HttpHeaderAuthentication>("header")
            }

            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Bearer $bearerToken"
            }
        }
    }
}

plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
    id("maven-publish")
    id("maven")
    id("net.minecrell.licenser") version "0.4.1"
}

group = "net.flintmc"

repositories {
    mavenLocal()
    flintRepository()
    mavenCentral()
}

version = System.getenv().getOrDefault("VERSION", "2.9.0")

dependencies {
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.11.1")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.11.1")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = "2.11.1")
    implementation(group = "com.squareup.okhttp3", name = "okhttp", version = "4.10.0-RC1")
    implementation(group = "io.github.java-diff-utils", name = "java-diff-utils", version = "4.7")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.12.0-rc1")

    implementation(group = "net.flintmc.installer", name = "logic-implementation", version = "1.1.10")
    implementation(group = "net.flintmc.installer", name = "frontend-gui", version = "1.1.10")
    implementation(group = "net.flintmc.installer", name = "logic", version = "1.1.10")
    implementation(group = "net.flintmc.installer", name = "logic-implementation", version = "1.1.5")
    implementation(group = "net.flintmc.installer", name = "logic", version = "1.1.5")

    implementation(group = "com.cloudbees", name = "diff4j", version = "1.2")
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

license {
    header = rootProject.file("LICENSE-HEADER")
    include("**/*.java")
    include("**/*.kts")

    tasks {
        create("gradle") {
            files = project.files("build.gradle.kts", "settings.gradle.kts")
        }
    }
}
