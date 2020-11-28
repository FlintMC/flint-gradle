import java.io.ObjectInput
import java.io.ObjectOutput

fun RepositoryHandler.flintGradlePluginRepository() {
    maven {
        setUrl("https://dist.labymod.net/api/v1/maven/release")
        name = "Flint"
        credentials(HttpHeaderCredentials::class) {
            name = "Authorization"
            value = "Bearer CbtTjzAOuDBr5QXcGnBc1MB3eIHxcZetnyHtdN76VpTNgbwAf87bzWPCntsXwj52"
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

fun RepositoryHandler.flintRepository() {
    maven {
        setUrl("http://dist.labymod.net/api/v1/maven/release")
        name = "Flint"
        credentials(HttpHeaderCredentials::class) {
            name = "Authorization"
            value = "Bearer CbtTjzAOuDBr5QXcGnBc1MB3eIHxcZetnyHtdN76VpTNgbwAf87bzWPCntsXwj52"
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("maven")
}

group = "net.flintmc"

repositories {
    mavenLocal()
    flintRepository()
    flintGradlePluginRepository()
    mavenCentral()
}

// 10.0.0 as default, only relevant for local publishing
version = System.getenv().getOrDefault("VERSION", "2.5.8")

dependencies {
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.11.1")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.11.1")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-annotations", version = "2.11.1")
    implementation(group = "org.apache.httpcomponents", name = "httpmime", version = "4.5.12")
    implementation(group = "org.apache.httpcomponents", name = "httpclient", version = "4.5.12")
    implementation(group = "io.github.java-diff-utils", name = "java-diff-utils", version = "4.7")
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-core", version = "2.12.0-rc1")
    implementation(group = "net.flintmc.installer", name = "logic-implementation", version = "1.1.5")
    implementation(group = "net.flintmc.installer", name = "logic", version = "1.1.5")
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
        flintGradlePluginRepository()
        maven {
            setUrl("https://dist.labymod.net/api/v1/maven/release")
            name = "Flint"
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Bearer CbtTjzAOuDBr5QXcGnBc1MB3eIHxcZetnyHtdN76VpTNgbwAf87bzWPCntsXwj52"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}

class SomeProperty(var prop: String) : java.io.Externalizable {
    companion object {
        private const val serialVersionUID: Long = 7829136421241571165L
    }

    override fun writeExternal(out: ObjectOutput?) {
        println("Writing object")
        out?.writeInt(100)
    }

    override fun readExternal(`in`: ObjectInput?) {
        println("Reading object")
        `in`?.readInt()
    }
}

open class TestTask : DefaultTask() {
    private val id = kotlin.random.Random.nextInt()

    @get:org.gradle.api.tasks.Input
    var prop: SomeProperty = SomeProperty("DEFG")

    @org.gradle.api.tasks.OutputFile
    fun getOut(): File {
        println("Retrieving output on $id")
        return File("ABC")
    }

    @org.gradle.api.tasks.TaskAction
    fun run() {
        println("Running task")
        getOut().writeText("Hello, ${prop.prop}")
    }
}

println("Creating task")
tasks.create<TestTask>("testTask") {
    println("Configuring task")
}
println("Creation done")
