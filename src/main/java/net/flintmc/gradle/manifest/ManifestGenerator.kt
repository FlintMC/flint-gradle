package net.flintmc.gradle.manifest

import net.flintmc.gradle.FlintGradlePlugin
import net.flintmc.gradle.extension.FlintGradleExtension
import net.flintmc.gradle.maven.MavenArtifactDownloader
import net.flintmc.gradle.maven.RemoteMavenRepository
import net.flintmc.gradle.maven.pom.MavenArtifact
import net.flintmc.installer.impl.repository.models.DependencyDescriptionModel
import net.flintmc.installer.impl.repository.models.InternalModelSerializer
import net.flintmc.installer.impl.repository.models.PackageModel
import net.flintmc.installer.impl.repository.models.install.DownloadFileDataModel
import net.flintmc.installer.impl.repository.models.install.DownloadMavenDependencyDataModel
import net.flintmc.installer.impl.repository.models.install.InstallInstructionModel
import net.flintmc.installer.impl.repository.models.install.InstallInstructionTypes
import net.flintmc.installer.impl.util.InternalFileHelper
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.authentication.http.HttpHeaderAuthentication
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.util.*
import java.util.jar.JarFile
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class ManifestGenerator(val flintGradlePlugin: FlintGradlePlugin) {

    val dependencyDescriptionModelGroups: MutableMap<DependencyDescriptionModel, String> = HashMap()

    fun installManifestGenerateTask(): Action<Project> = Action { project ->

        val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)

        if (isValidProject(project)) {
            publishingExtension.publications { publications ->
                publications.create("maven", MavenPublication::class.java) { publication ->
                    publication.groupId = project.group.toString()
                    publication.artifactId = project.name
                    publication.version = project.version.toString()
                    publication.from(project.components.getByName("java"))

                }
            }

            publishingExtension.repositories.maven { mavenArtifactRepository ->
                mavenArtifactRepository.setUrl(
                    System.getenv()["FLINT_DISTRIBUTOR_URL"] + "maven/" + System.getenv()
                        .getOrDefault("FLINT_DISTRIBUTOR_CHANNEL", "release") + "/"
                )
                mavenArtifactRepository.name = "Distributor"

                mavenArtifactRepository.credentials(HttpHeaderCredentials::class.java) { httpHeaderCredentials ->
                    val authorizationToken = System.getenv()["FLINT_DISTRIBUTOR_AUTHORIZATION"]

                    if (authorizationToken != null) {
                        httpHeaderCredentials.name = "Authorization"
                        httpHeaderCredentials.value = "Bearer $authorizationToken"
                    } else {
                        val publishToken = System.getenv()["FLINT_DISTRIBUTOR_PUBLISH_TOKEN"]
                        httpHeaderCredentials.name = "Publish-Token"
                        httpHeaderCredentials.value = publishToken
                    }

                }

                mavenArtifactRepository.authentication { authenticationContainer ->
                    authenticationContainer.create("header", HttpHeaderAuthentication::class.java)
                }
            }
        }


        project.tasks.create("generateFlintManifest") { task ->
            task.doLast {
                task.group = "publish"
                val manifest = InternalModelSerializer().toString(generateManifest(project))
                val path = Paths.get(project.buildDir.path.plus("/generated/flint/manifest.json"));
                val file = File(path.toString())
                file.parentFile.mkdirs()
                file.delete()
                file.createNewFile()
                Files.write(path, manifest.toByteArray(StandardCharsets.UTF_8))
            }
        }


        val jarTask: Jar = project.tasks.findByName("jar") as Jar;
        jarTask.dependsOn("generateFlintManifest")
        jarTask.from(project.buildDir.path.plus("/generated/flint/manifest.json"))

        project.tasks.create("publishFlintManifest") { task ->
            task.group = "publish"
            task.dependsOn("generateFlintManifest")
            task.doLast {
                if (!isValidProject(project)) return@doLast

                this.uploadFile(
                    StringEntity(
                        Paths.get(project.buildDir.path.plus("/generated/flint/manifest.json")).toFile()
                            .readText(StandardCharsets.UTF_8),
                        ContentType.APPLICATION_JSON
                    ),
                    "publish/" + System.getenv().getOrDefault("FLINT_DISTRIBUTOR_CHANNEL", "release") + "/${
                        project.group.toString().replace('.', '/')
                    }/${project.name}/${project.version}/manifest.json"
                )
            }
        }


        project.tasks.create("publishFlintStaticFiles") { task ->
            task.group = "publish"
            task.doLast {
                if (!isValidProject(project)) return@doLast
                val flintExtension = getFlintExtension(project)

                flintExtension.staticFileEntries.forEach {
                    this.uploadFile(
                        ByteArrayEntity(
                            project.file(it.from).readBytes(),
                            ContentType.DEFAULT_BINARY
                        ),
                        "maven/" + System.getenv().getOrDefault("FLINT_DISTRIBUTOR_CHANNEL", "release") + "/${
                            project.group.toString().replace('.', '/')
                        }/${project.name}/${project.version}/${it.upstreamName}"
                    )

                    this.uploadFile(
                        ByteArrayEntity(
                            InternalFileHelper().getHash(
                                project.projectDir.toPath().toString() + "/" + it.from.toString()
                            )
                                .toByteArray(StandardCharsets.UTF_8),
                            ContentType.DEFAULT_BINARY
                        ),
                        "maven/" + System.getenv().getOrDefault("FLINT_DISTRIBUTOR_CHANNEL", "release") + "/${
                            project.group.toString().replace('.', '/')
                        }/${project.name}/${project.version}/${it.upstreamName}.md5"
                    )


                }

            }
        }

        project.tasks.create("publishFlintJar") { task ->
            task.group = "publish"
            task.dependsOn("jar")
            task.doLast {
                if (!isValidProject(project)) return@doLast
                this.uploadFile(
                    ByteArrayEntity(
                        project.tasks.getByName("jar").outputs.files.singleFile
                            .readBytes(),
                        ContentType.DEFAULT_BINARY
                    ),
                    "maven/" + System.getenv().getOrDefault("FLINT_DISTRIBUTOR_CHANNEL", "release") + "/${
                        project.group.toString().replace('.', '/')
                    }/${project.name}/${project.version}/${project.name}-${project.version}.jar"
                )
            }
        }


    }

    private fun uploadFile(data: HttpEntity, path: String) {

        val httpPut =
            HttpPut(System.getenv().getOrDefault("FLINT_DISTRIBUTOR_URL", "https://dist.labymod.net/api/v1/") + path)
        httpPut.setHeader("Authorization", "Bearer " + System.getenv("FLINT_DISTRIBUTOR_AUTHORIZATION"))
        httpPut.setHeader("Publish-Token", System.getenv("FLINT_DISTRIBUTOR_PUBLISH_TOKEN"))
        httpPut.entity = data

        val execute: CloseableHttpResponse = flintGradlePlugin.httpClient.execute(httpPut) as CloseableHttpResponse
        if (execute.statusLine.statusCode == 200) {
            execute.close()
            return
        }
        execute.close()

        throw java.lang.RuntimeException("$httpPut $execute")
    }

    private fun generateManifest(project: Project): PackageModel {
        val flintExtension = getFlintExtension(project)

        if (flintExtension.flintVersion == null)
            throw IllegalArgumentException("Flint version defined for project $project must not be null")

        val collectedInstructions = collectInstructions(project);
        val collectedDependencies = collectDependencies(project)

        val collectedOwnDependency = collectOwnDependency(project)
        return PackageModel(
            project.group.toString(),
            project.name,
            project.description,
            project.version.toString(),
            System.getenv().getOrDefault("FLINT_DISTRIBUTOR_CHANNEL", "release"),
            flintExtension.minecraftVersions.joinToString(","),
            flintExtension.flintVersion,
            flintExtension.authors.toSet(),
            collectedDependencies,
            collectedInstructions.mapNotNull {
                when (it.type) {
                    InstallInstructionTypes.DOWNLOAD_MAVEN_DEPENDENCY.toString() -> {
                        val data = collectedOwnDependency.getData<DownloadMavenDependencyDataModel>()
                        if (data.path != it.getData<DownloadMavenDependencyDataModel>().path)
                            return@mapNotNull it.getData<DownloadMavenDependencyDataModel>().path
                        else return@mapNotNull null
                    }
                    InstallInstructionTypes.DOWNLOAD_FILE.toString() ->
                        it.getData<DownloadFileDataModel>().path
                    else -> null
                }
            }.toSet(),
            collectedInstructions
        )
    }

    private fun collectDependencies(project: Project): Set<DependencyDescriptionModel> {
        return this.collectModuleDependencies(project) + this.collectProjectDependencies(project)
    }

    private fun collectInstructions(project: Project): List<InstallInstructionModel> {
        val mavenArtifactDownloader = MavenArtifactDownloader()

        for (repository in project.repositories) {
            if (repository is MavenArtifactRepository) {
                val uris: MutableCollection<URI> = HashSet(repository.artifactUrls)
                uris.add(repository.url)

                for (uri in uris) {
                    if (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
                        var creds =
                            repository.getCredentials(HttpHeaderCredentials::class.java) as HttpHeaderCredentials

                        if (creds.name != null && creds.value != null) {
                            mavenArtifactDownloader.addSource(
                                RemoteMavenRepository(
                                    flintGradlePlugin.httpClient,
                                    uri.toString(),
                                    creds
                                )
                            )
                        } else {
                            mavenArtifactDownloader.addSource(
                                RemoteMavenRepository(
                                    flintGradlePlugin.httpClient,
                                    uri.toString()
                                )
                            )
                        }
                    }
                }
            }
        }

        return collectModuleInstructions(
            project,
            mavenArtifactDownloader
        ) + collectStatic(project) + collectOwnDependency(project)
    }

    private fun collectStatic(project: Project): List<InstallInstructionModel> {
        val flintExtension = getFlintExtension(project)
        return flintExtension.staticFileEntries
            .map {
                InstallInstructionModel(
                    InstallInstructionTypes.DOWNLOAD_MAVEN_DEPENDENCY,
                    DownloadMavenDependencyDataModel(
                        project.group.toString(),
                        project.name,
                        project.version.toString(),
                        null,
                        "\${FLINT_DISTRIBUTOR_URL}" + System.getenv()
                            .getOrDefault("FLINT_DISTRIBUTOR_CHANNEL", "release") + "/${
                            project.group.toString().replace('.', '/')
                        }/${project.name}/${project.version}/${it.upstreamName}",
                        it.to.toString()
                    )
                )

            }
    }

    private fun collectOwnDependency(project: Project): InstallInstructionModel {

        val flintExtension = getFlintExtension(project)

        var ownModel = InstallInstructionModel(
            InstallInstructionTypes.DOWNLOAD_MAVEN_DEPENDENCY,
            DownloadMavenDependencyDataModel(
                project.group.toString(),
                project.name,
                project.version.toString(),
                null,
                "\${FLINT_DISTRIBUTOR_URL}" + System.getenv()
                    .getOrDefault("FLINT_DISTRIBUTOR_CHANNEL", "release") + "/",
                if (flintExtension.type == FlintGradleExtension.Type.LIBRARY)
                    "\${FLINT_LIBRARY_DIR}/${
                        project.group.toString().replace('.', '/')
                    }/${project.name}/${project.version}/${project.name}-${project.version}.jar"
                else
                    "\${FLINT_PACKAGE_DIR}/${project.name}.jar"
            )
        )

        return ownModel
    }

    private fun collectProjectDependencies(project: Project): Set<DependencyDescriptionModel> {
        return collectArtifacts(project)
            .filter {
                it.id.componentIdentifier is ProjectComponentIdentifier
            }
            .map {

                val componentIdentifier: ProjectComponentIdentifier =
                    it.id.componentIdentifier as ProjectComponentIdentifier

                val targetProject =
                    project.rootProject.project(componentIdentifier.projectPath)

                if (!hasFlintExtension(targetProject)) {
                    throw RuntimeException("Project $targetProject does not match and is not a flint package. We cannot handle that.. No Idea where tf to get it from")
                }
                if (!isValidProject(targetProject)) {
                    throw IllegalStateException("project filter does not match project")
                }
                val dependencyDescriptionModel = DependencyDescriptionModel(
                    targetProject.name,
                    targetProject.version.toString(),
                    System.getenv().getOrDefault("FLINT_DISTRIBUTOR_CHANNEL", "release")
                )
                dependencyDescriptionModelGroups[dependencyDescriptionModel] = targetProject.group.toString()

                dependencyDescriptionModel
            }
            .toSet()
    }

    private fun collectModuleDependencies(project: Project): Set<DependencyDescriptionModel> {
        return collectArtifacts(project)
            .filter {
                it.id.componentIdentifier is ModuleComponentIdentifier
            }
            .map {
                if (!it.file.extension.equals("jar", true)) return@map null
                val jarFile = JarFile(it.file)
                for (entry in jarFile.entries()) {
                    if (entry.name != "manifest.json") continue

                    val inputStream = jarFile.getInputStream(entry)
                    val manifestData = String(inputStream.readBytes())
                    println(manifestData)
                    val packageModel = InternalModelSerializer().fromString(manifestData, PackageModel::class.java)
                    val dependencyDescriptionModel = DependencyDescriptionModel(
                        packageModel.name,
                        packageModel.version,
                        packageModel.channel
                    )

                    println("group ${packageModel.group} for " + packageModel.name)
                    dependencyDescriptionModelGroups[dependencyDescriptionModel] = packageModel.group.toString()
                    return@map dependencyDescriptionModel
                }
                jarFile.close()
                return@map null
            }
            .filterNotNull()
            .toSet()
    }

    private fun collectModuleInstructions(
        project: Project,
        mavenArtifactDownloader: MavenArtifactDownloader
    ): List<InstallInstructionModel> {
        return collectArtifacts(project)
            .filter {
                it.id.componentIdentifier is ModuleComponentIdentifier
            }
            .map {
                val componentIdentifier: ModuleComponentIdentifier =
                    it.id.componentIdentifier as ModuleComponentIdentifier

                if (it.file.extension.equals("jar", true)) {
                    val jarFile = JarFile(it.file)
                    for (entry in jarFile.entries()) {
                        if (entry.name == "manifest.json") return@map null
                    }
                }

                InstallInstructionModel(
                    InstallInstructionTypes.DOWNLOAD_MAVEN_DEPENDENCY,
                    DownloadMavenDependencyDataModel(
                        componentIdentifier.group,
                        componentIdentifier.module,
                        componentIdentifier.version,
                        it.classifier,
                        mavenArtifactDownloader.findRepositoryUri(
                            MavenArtifact(
                                componentIdentifier.group,
                                componentIdentifier.module,
                                componentIdentifier.version,
                                it.classifier
                            )
                        ).toString(),
                        "\${FLINT_LIBRARY_DIR}/${
                            componentIdentifier.group.toString().replace('.', '/')
                        }/${componentIdentifier.module}/${componentIdentifier.version}/${componentIdentifier.module}-${componentIdentifier.version}${if (it.classifier != null) "-${it.classifier}" else ""}.jar"
                    )
                )
            }
            .filterNotNull()
    }

    private fun collectArtifacts(project: Project): Collection<ResolvedArtifact> {
        return project.configurations.getByName("runtimeClasspath").resolvedConfiguration.resolvedArtifacts
    }

    private fun getFlintExtension(project: Project): FlintGradleExtension =
        project.extensions.getByType(FlintGradleExtension::class.java)

    private fun hasFlintExtension(project: Project): Boolean =
        project.extensions.findByType(FlintGradleExtension::class.java) != null

    private fun isValidProject(project: Project): Boolean =
        hasFlintExtension(project) && getFlintExtension(project).projectFilter.test(project)
}