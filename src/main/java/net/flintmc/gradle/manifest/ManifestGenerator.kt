package net.flintmc.gradle.manifest

import net.flintmc.gradle.FlintGradlePlugin
import net.flintmc.gradle.extension.FlintGradleExtension
import net.flintmc.installer.impl.repository.models.InternalModelSerializer
import net.flintmc.installer.impl.repository.models.PackageModel
import net.flintmc.installer.impl.repository.models.install.DownloadMavenDependencyDataModel
import net.flintmc.installer.impl.repository.models.install.InstallInstructionModel
import net.flintmc.installer.impl.repository.models.install.InstallInstructionTypes
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import java.lang.IllegalArgumentException

class ManifestGenerator(flintGradlePlugin: FlintGradlePlugin) {

    fun installManifestGenerateTask(): Action<Project> = Action { project ->
        project.tasks.create("publishManifest") { task ->
            task.group = "publish"
            task.doLast {
                if (!isValidProject(project)) return@doLast

                println(InternalModelSerializer().toString(generateManifest(project)))
            }
        }
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
                HashSet(),
                collectInstructions(project)
        )
    }

    private fun collectInstructions(project: Project): List<InstallInstructionModel> {
        return collectModuleInstructions(project) + collectProjectInstructions(project);
    }

    private fun collectProjectInstructions(project: Project): List<InstallInstructionModel> {
        return collectDependencies(project).filter {
            ModuleComponentIdentifier::class.isInstance(it)
        }.map {
            InstallInstructionModel(
                    InstallInstructionTypes.DOWNLOAD_MAVEN_DEPENDENCY,
                    DownloadMavenDependencyDataModel(
                            it.group,
                            it.name,
                            it.version,
                            ""))
        }
    }

    private fun collectModuleInstructions(project: Project): List<InstallInstructionModel> {
        return collectDependencies(project).filter {
            ModuleComponentIdentifier::class.isInstance(it)
        }.map {
            InstallInstructionModel(
                    InstallInstructionTypes.DOWNLOAD_MAVEN_DEPENDENCY,
                    DownloadMavenDependencyDataModel(
                            it.group,
                            it.name,
                            it.version,
                            ""))
        }
    }

    private fun collectDependencies(project: Project): Collection<Dependency> {
        return project.configurations.getByName("runtimeClasspath").allDependencies
    }

    private fun getFlintExtension(project: Project): FlintGradleExtension = project.extensions.findByType(FlintGradleExtension::class.java)!!

    private fun isValidProject(project: Project): Boolean = project.extensions.findByType(FlintGradleExtension::class.java) != null && getFlintExtension(project).projectFilter.test(project)
}