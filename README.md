# Labyfy gradle Plugin
Generic Minecraft plugin for the Gradle build system.

## Usage
### Basic
```groovy
buildscript {
    repositories {
        maven {
            // Add the repository, the plugin is available on
            // the Labymedia Artifactory
            url "https://maven.laby.tech/artifactory/general"
            name "Labymedia"
        } 
    }
}

// Apply the plugin
apply plugin: 'net.labyfy.labyfy-gradle-plugin'
```
This alone is not enough to actually initialize the project, the plugin still needs to be told,
which minecraft versions run configurations should be generated for. To solve this task, the
plugin creates a global `labyfy` extension on the build script, which can be used like this:
```groovy
labyfy {
    minecraftVersions "1.12.2", "1.15.2"
}
```

This will generate multiple run tasks and source sets:
- Tasks
  - `runClient1.12.2`
  - `runClient1.15.2`
- Source sets:
  - `main` Will contain source code shared across all versions and configurations
  - `internal` Will contain source code only available to source sets bound to versions
  - `v1_12_2` Will contain source code when running the `1.12.2` client
  - `v1_15_2` Will contain source code when running the `1.15.2` client
  
This results in the following project structure:
```
.
├── build.gradle
├── settings.gradle
└── src
    ├── internal
    │   ├── java
    │   └── resources
    ├── main
    │   ├── java
    │   └── resources
    ├── v1_12_2
    │   ├── java
    │   └── resources
    └── v1_15_2
        ├── java
        └── resources
```
Java files now go into their respective directories:
- Everything in `main` can be accessed by other source sets and other packages, often `main`
  only contains interfaces. `main` does not have access to any other source sets.
- Everything in `internal` can be used from the version bound source sets, but will not be
  available to packages depending on the package built by this workspace. `internal` often
  contains the implementation of the interfaces in `main`.
- Everything in `v1_12_2` and `v1_15_2` has access to `main` and `internal`, but is not visible
  to other packages. Additionally, the source sets also have access to their respective
  minecraft version.

### With labyfy
Just adding the plugin to the project does not automatically pull in the Labyfy framework, but
sets up a workspace for running minecraft with a user defined classpath.

#### Setting up the framework
 To use the Labyfy framework, the build script needs to be configured in the usual way, as it has
to be for all dependencies.
```groovy
repositories {
    // Required for the Labyfy dependencies below
    url "https://maven.laby.tech/artifactory/general"
    name "Labymedia"
}

dependencies {
    // Do NOT use the old `compile` or `runtime` configurations, they
    // might cause issues. Instead rely on `implementation` and
    // `runtimeOnly`

    // Pull in the required labyfy modules, only gui in this case
    implementation group: 'net.labyfy.component', name: 'gui', version: '<LABYFY VERSION>'

    // The annotation processor is also required, however, it needs to be
    // applied to each individual configuration it is used on. In the most
    // cases that means the internal and versioned configurations, in rare
    // cases the main source set might also need it.
    //
    // In this example it will be applied to the internal and versioned
    // source sets
    internalAnnotationProcessor group: 'net.labyfy.component.annotation-processing', name: 'autoload', version: '<LABYFY VERSION>'
    versionedAnnotationProcessor('1.12.2', group: 'net.labyfy.component.annotation-processing', name: 'autoload', version: '<LABYFY VERSION>')
    versionedAnnotationProcessor('1.15.2', group: 'net.labyfy.component.annotation-processing', name: 'autoload', version: '<LABYFY VERSION>')

    // Declaring dependencies for versioned source sets should be done
    // the following way (as also seen above for the annotation processor)
    //
    // This will allow code in the v1_12_2 source set to use JOML, all other
    // source sets wont have it available
    versionedImplementation('1.12.2', group: 'org.joml', 'joml', version: '1.9.25')
}
```
Additionally, the main class needs to be overwritten to point to the Labyfy specific one.
This is done the following way:
```groovy
labyfy {
    runs {
        overrideMainClass 'net.labyfy.component.launcher.LabyLauncher'
    }
}
```
#### Publishing
If you have a publish-token to publish to the official LabyMod package repository, this can be
done by setting it in the `labyfy` extension of the gradle build script.
```groovy
labyfy {
    // DON'T USE THIS IF YOUR REPOSITORY IS PUBLIC!
    publishToken "your-personal-publish-token"
}
```
**For security reasons, you should never push the token to a public GIT repository**.
Consider using the following approach, which will allow you to set the publish-token in your
local `gradle.properties`:
```groovy
labyfy {
    if(project.hasProperty('net.labyfy.publish-token')) {
        publishToken "${project.getProperty('net.labyfy.publish-token')}"
    }
}
```

### Advanced usage
The plugin is capable of working in more complex environments where the configuration
needs to be tweaked.

#### Filtering the projects the plugin applies itself to
By default, the labyfy plugin will apply itself to all sub-projects which also have the `java`
plugin applied. If this behavior is undesired, or you want finer grained control about the
projects the plugin applies itself to, a specialized filter can be set on the parent project.
```groovy
labyfy {
    projectFilter { 
        // Only apply to sub-projects which names end with -mod
        return it.getName().endsWith("-mod") 
    }
}
```

#### Filtering the minecraft version a source set applies to
Sometimes you want certain source sets to only apply to certain minecraft versions.
To do so, you can add a `minecraftVersion` property to the source set extensions.
Take the following example:
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
The plugin supports having multiple, named run configurations. Run-configurations allow you
to specify different scenarios, in which only certain configurations actually end up on
the runtime classpath for certain run tasks.

Take the setup below for example:
```groovy
labyfy {
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
This setup will create or edit 2 run-configurations, named `potato` and `lightning`.
Run-configurations are shared across all projects in multi-project builds and can be used
to create customized scenarios, in which only parts of the classpath influence the final 
runtime classpath.

In the example above, the source set named `fancy_gui` will not be included when running the
`potato` configuration, while all other source sets are. The reverse is true for the 
`lightning` configuration, only the `fancy_gui` source set will be included (please note, that
even explicitly excluded dependency source sets of `fancy_gui` will still be pulled in to 
prevent breaking the classpath!). Above configuration will yield at least 3 run-tasks,
assuming the project has only been configured for 1.15.2:
- `runClient1.15.2` - The default run-configuration, the project will have its `main`, 
  `internal` and `v1_15_2` source sets included. Those are the source sets included by default
  for the `main` run configuration.
- `runClient1.15.2Potato` - The `potato` run-configuration, every source set except the 
  `fancy_gui` one will be included in it.
- `runClient1.15.2Lightning` - The `lightning` run-configuration, only the `fancy_gui` source 
   set (and its dependencies) will be included in the run configuration.
 
These filters apply recursively to all sub-projects, so children can overwrite their own
configurations, and the defaults for their children (Note that you usually just end up with
one parent and a lot of children, since gradle includes all projects as children of the root
when done from one `settings.gradle`.)