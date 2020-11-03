package net.flintmc.gradle.manifest

import net.flintmc.gradle.FlintGradlePlugin
import net.flintmc.gradle.extension.FlintGradleExtension
import net.flintmc.gradle.maven.MavenArtifactDownloader
import net.flintmc.gradle.maven.RemoteMavenRepository
import net.flintmc.gradle.maven.pom.MavenArtifact
import net.flintmc.installer.impl.repository.models.DependencyDescriptionModel
import net.flintmc.installer.impl.repository.models.InternalModelSerializer
import net.flintmc.installer.impl.repository.models.PackageModel
import net.flintmc.installer.impl.repository.models.install.DownloadMavenDependencyDataModel
import net.flintmc.installer.impl.repository.models.install.InstallInstructionModel
import net.flintmc.installer.impl.repository.models.install.InstallInstructionTypes
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.content.ByteArrayBody
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.lang.AssertionError
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.*

class ManifestGenerator(val flintGradlePlugin: FlintGradlePlugin) {

    fun installManifestGenerateTask(): Action<Project> = Action { project ->

        project.tasks.create("generateManifest") { task ->
            task.doLast {
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
        jarTask.dependsOn("generateManifest")
        jarTask.from(project.buildDir.path.plus("/generated/flint/manifest.json"))

        project.tasks.create("publishManifest") { task ->
            task.group = "publish"
            task.dependsOn("generateManifest")
            task.doLast {
                if (!isValidProject(project)) return@doLast

                this.uploadFile(
                    Paths.get(project.buildDir.path.plus("/generated/flint/manifest.json")).toFile()
                        .readText(StandardCharsets.UTF_8),
                    "publish/release/${
                        project.group.toString().replace('.', '/')
                    }/${project.name}/${project.version}/manifest.json",
                    ContentType.APPLICATION_JSON
                )
            }
        }

        project.tasks.create("publishJar") { task ->
            task.group = "publish"
            task.dependsOn("jar")
            task.doLast {
                if (!isValidProject(project)) return@doLast

                this.uploadFile(
                    project.tasks.getByName("jar").outputs.files.singleFile
                        .readText(StandardCharsets.UTF_8),
                    "maven/release/${
                        project.group.toString().replace('.', '/')
                    }/${project.name}/${project.version}/${project.name}-${project.version}.jar",
                    ContentType.APPLICATION_JSON
                )
            }
        }
    }

    private fun uploadFile(data: String, path: String, fileType: ContentType) {

        val httpPut = HttpPut(System.getenv().getOrDefault("DISTRIBUTOR_URL", "https://dist.labymod.net/api/v1/")+path)
        httpPut.setHeader("Authorization", "Bearer "+System.getenv("DISTRIBUTOR_AUTHORIZATION"))
        httpPut.setHeader("Publish-Token", System.getenv("DISTRIBUTOR_PUBLISH_TOKEN"))
        httpPut.entity = StringEntity(data, fileType)

        val execute = flintGradlePlugin.httpClient.execute(httpPut)
        if (execute.statusLine.statusCode == 200) return

        throw java.lang.RuntimeException(httpPut.toString() + " " +execute.statusLine.reasonPhrase)
    }

    private fun generateManifest(project: Project): PackageModel {
        val flintExtension = getFlintExtension(project)

        if (flintExtension.flintVersion == null)
            throw IllegalArgumentException("Flint version defined for project $project must not be null")

        return PackageModel(
            project.name,
            project.description,
            project.version.toString(),
            flintExtension.minecraftVersions.joinToString(","),
            flintExtension.flintVersion,
            flintExtension.authors.toSet(),
            collectProjectDependencies(project),
            collectInstructions(project)
        )
    }

    private fun collectInstructions(project: Project): List<InstallInstructionModel> {
        val mavenArtifactDownloader = MavenArtifactDownloader()

        for (repository in project.repositories) {
            if (repository is MavenArtifactRepository) {
                val uris: MutableCollection<URI> = HashSet(repository.artifactUrls)
                uris.add(repository.url)
                for (uri in uris) {
                    mavenArtifactDownloader.addSource(
                        RemoteMavenRepository(
                            flintGradlePlugin.httpClient,
                            uri.toString()
                        )
                    )
                }
            }
        }

        return collectModuleInstructions(project, mavenArtifactDownloader) + collectProjectInstructions(project)
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
                if (getFlintExtension(targetProject).type == FlintGradleExtension.Type.PACKAGE) {
                    return@map DependencyDescriptionModel(targetProject.name, targetProject.version.toString())
                }
                return@map null
            }
            .filterNotNull()
            .toSet()

    }

    private fun collectProjectInstructions(project: Project): List<InstallInstructionModel> {

        return collectArtifacts(project)
            .filter {
                it.id.componentIdentifier is ProjectComponentIdentifier
            }
            .map {

                val componentIdentifier: ProjectComponentIdentifier =
                    it.id.componentIdentifier as ProjectComponentIdentifier

                val targetProject =
                    project.rootProject.project(componentIdentifier.projectPath)


                var nameSpace = System.getenv("FLINT_NAMESPACE")
                if (nameSpace == null) {
                    nameSpace = "RELEASE"
                }
                nameSpace = nameSpace.toLowerCase()


                if (!hasFlintExtension(project)) {
                    throw RuntimeException("Project $targetProject does not match and is not a flint package. We cannot handle that.. No Idea where tf to get it from")
                }
                if (!isValidProject(project)) {
                    throw IllegalStateException("project filter does not match project")
                }
                if (getFlintExtension(project).type == FlintGradleExtension.Type.LIBRARY) {
                    return@map InstallInstructionModel(
                        InstallInstructionTypes.DOWNLOAD_MAVEN_DEPENDENCY,
                        DownloadMavenDependencyDataModel(
                            targetProject.group.toString(),
                            targetProject.name,
                            targetProject.version.toString(),
                            null,
                            String.format(
                                "\${FLINT_DISTRIBUTOR_URL}/%s",
                                nameSpace
                            )
                        )
                    )
                }
                return@map null
            }
            .filterNotNull()
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
                        ).toString()
                    )
                )
            }
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