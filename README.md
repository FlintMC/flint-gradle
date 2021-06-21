# Flint gradle Plugin

###### Generic Minecraft plugin for the Gradle build system

------------------------------------------------------------------------------------------------------------------------

## Usage

### Basic

**Gradle 6.5 or higher is required!**

The plugin is available on the LabyMod distributor and can be added to gradle the following way:

`settings.gradle`:

```groovy
pluginManagement {
    plugins {
        // Make sure to use the latest version
        id "net.flintmc.flint-gradle" version "2.7.1"
    }

    buildscript {
        repositories {
            maven {
                url = "https://dist.labymod.net/api/v1/maven/release"
                name = "LabyMod Distributor"
            }
            mavenCentral()
        }
    }
}
```

`build.gradle`:

```groovy
plugins {
    id "net.flintmc.flint-gradle"
}
```

(Syntax varies slightly for kotlin)

This alone is not enough to actually initialize the project, the plugin still needs to be told, which minecraft versions
run configurations should be generated for. Additionally, other properties may be set to further customize build. To
solve this task, the plugin creates a global `flint` extension on the build script, which can be used like this:

```groovy
flint {
    minecraftVersions "1.15.2", "1.16.4"
    // Make sure to use latest
    flintVersion "2.0.5"

    authors "Me", "Myself", "I"
}
```

This will generate multiple run tasks and source sets:

- Tasks
    - `runClient1.15.2`
    - `runClient1.16.4`
- Source sets:
    - `main` Will contain source code shared across all versions and configurations
    - `internal` Will contain source code only available to source sets bound to versions
    - `v1_15_2` Will contain source code when running the `1.15.2` client
    - `v1_16_4` Will contain source code when running the `1.16.4` client

This results in the following project structure:

```
.
├── build.gradle
├── settings.gradle
└── src
    ├── internal
    |   ├── java
    │   └── resources
    ├── main
    │   ├── java
    |   └── resources
    ├── v1_15_2
    │   ├── java
    │   └── resources
    └── v1_16_4
        ├── java
        └── resources
```

Java files now go into their respective directories:

- Everything in `main` can be accessed by other source sets and other packages, often `main`
  only contains interfaces. `main` does not have access to any other source sets.
- Everything in `internal` can be used from the version bound source sets, but will not be available to packages
  depending on the package built by this workspace. `internal` often contains the implementation of the interfaces
  in `main`.
- Everything in `v1_15_2` and `v1_16_4` has access to `main` and `internal`, but is not visible to other packages.
  Additionally, the source sets also have access to their respective minecraft version.

### With flint

Just adding the plugin to the project does not automatically pull in the Flint framework, but sets up a workspace for
running minecraft with a user defined classpath.

#### Setting up the framework

To use the Flint framework, the build script needs to be configured in the usual way, as it has to be for all
dependencies.

```groovy
repositories {
    maven {
        // Required for the Flint dependencies below
        url = "https://dist.labymod.net/api/v1/maven/release"
        name = "LabyMod Distributor"
    }
}

dependencies {
    // Do NOT use the old `compile` or `runtime` configurations, they
    // might cause issues. Instead rely on `implementation` and
    // `runtimeOnly`

    // Pull in the required flint modules, only render-gui in this case
    implementation flintApi("render-gui")
    // flintApi("module") is a shortcut for "net.flintmc:module:flint-version"

    // The annotation processor is also required, however, it needs to be
    // applied to each individual configuration it is used on. In the most
    // cases that means the internal and versioned configurations, in rare
    // cases the main source set might also need it.
    //
    // In this example it will be applied to the internal and versioned
    // source sets
    internalAnnotationProcessor flintApi("annotation-processing-autoload")

    // This will apply the annotation processor to the source sets for
    // the minecraft versions 1.15.2 and 1.16.4 
    minecraft(["1.15.2", "1.16.4"]) {
        annotationProcessor flintApi("annotation-processing-autoload")
    }
    // Declaring dependencies for versioned source sets should be done
    // the following way (as also seen above for the annotation processor)
    //
    // This will allow code in the v1_16_4 source set to use JOML, all other
    // source sets wont have it available
    minecraft("1.16.4") {
        implementation group: 'org.joml', 'joml', version: '1.9.25'
    }
}
```

Additionally, the main class needs to be overwritten to point to the Flint specific one. This is done the following way:

```groovy
flint {
    runs {
        overrideMainClass 'net.flintmc.launcher.FlintLauncher'
    }
}
```

#### Publishing

If you have a publish-token to publish to the official LabyMod package repository, you can set it using the 2 following
ways:

1. Setting the environment variable `FLINT_DISTRIBUTOR_PUBLISH_TOKEN`
2. Setting the gradle property `net.flint.distributor.publish-token`

If you use variant 2, make sure to set it in your local gradle properties (`C:\Users\YourUser\.gradle\gradle.properties`
on Windows, `~/.gradle/gradle.properties` on OSX/Linux).

**Do not set it in the project gradle.properties as that could expose the token to others!**

### Advanced usage

The plugin is capable of working in more complex environments where the configuration needs to be tweaked.

#### Filtering the projects the plugin applies itself to

By default, the flint plugin will apply itself to all sub-projects which also have the `java`
plugin applied. If this behavior is undesired, or you want finer grained control about the projects the plugin applies
itself to, a specialized filter can be set on the parent project.

```groovy
flint {
    projectFilter {
        // Only apply to sub-projects which names end with -mod
        return it.getName().endsWith("-mod")
    }
}
```

#### Filtering the minecraft version a source set applies to

Sometimes you want certain source sets to only apply to certain minecraft versions. To do so, you can add
a `minecraftVersion` property to the source set extensions. Take the following example:

```groovy
sourceSets {
    myCustomSourceSet {
        // Include the source set only in 1.12.2 runs
        ext.minecraftVersion = "1.12.2"
    }
}
```

#### Run configurations

**This feature is unstable!**
The plugin supports having multiple, named run configurations. Run-configurations allow you to specify different
scenarios, in which only certain configurations actually end up on the runtime classpath for certain run tasks.

Take the setup below for example:

```groovy
flint {
    runs {
        // Include all source sets of this project in the potato
        // run-configuration
        include "potato"

        // Exclude the fancy_gui source set from the `potato` run-configuration
        exclude "potato", "fancy_gui"

        // Exclude all source sets of this project from the `lightning` run-
        // configuration
        exclude "lightning"

        // However, include the fancy_gui module nevertheless
        include "lightning", "fancy_gui"
    }
}
```

This setup will create or edit 2 run-configurations, named `potato` and `lightning`. Run-configurations are shared
across all projects in multi-project builds and can be used to create customized scenarios, in which only parts of the
classpath influence the final runtime classpath.

In the example above, the source set named `fancy_gui` will not be included when running the
`potato` configuration, while all other source sets are. The reverse is true for the
`lightning` configuration, only the `fancy_gui` source set will be included (please note, that even explicitly excluded
dependency source sets of `fancy_gui` will still be pulled in to prevent breaking the classpath!). Above configuration
will yield at least 3 run-tasks, assuming the project has only been configured for 1.15.2:

- `runClient1.15.2` - The default run-configuration, the project will have its `main`,
  `internal` and `v1_15_2` source sets included. Those are the source sets included by default for the `main` run
  configuration.
- `runClient1.15.2Potato` - The `potato` run-configuration, every source set except the
  `fancy_gui` one will be included in it.
- `runClient1.15.2Lightning` - The `lightning` run-configuration, only the `fancy_gui` source set (and its dependencies)
  will be included in the run configuration.

These filters apply recursively to all sub-projects, so children can overwrite their own configurations, and the
defaults for their children.
