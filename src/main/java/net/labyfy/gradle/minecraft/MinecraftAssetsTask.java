package net.labyfy.gradle.minecraft;

import com.fasterxml.jackson.databind.JsonNode;
import net.labyfy.gradle.LabyfyGradlePlugin;
import net.labyfy.gradle.json.JsonConverter;
import net.labyfy.gradle.minecraft.data.version.AssetIndex;
import net.labyfy.gradle.minecraft.data.version.VersionManifest;
import net.labyfy.gradle.util.Util;
import org.apache.http.client.HttpClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Task for downloading and install minecraft assets.
 */
@CacheableTask
public class MinecraftAssetsTask extends DefaultTask {
    private static final String ASSET_BASE_URL = "https://resources.download.minecraft.net/";

    private VersionManifest manifest;
    private Path directory;

    /**
     * Sets the version manifest to retrieve the assets index from.
     *
     * @param manifest The manifest to retrieve the assets index from
     */
    @Input
    public void setVersionManifest(VersionManifest manifest) {
        this.manifest = manifest;
    }

    /**
     * Sets the output directory to write assets into.
     *
     * @param directory The directory to write assets into
     */
    @OutputDirectory
    public void setOutputDirectory(Path directory) {
        this.directory = directory;
    }

    /**
     * Downloads all assets for the configured minecraft manifest.
     *
     * @throws IOException If an I/O error occurs while downloading
     */
    @TaskAction
    public void run() throws IOException {
        // Validate state
        if(manifest == null) {
            throw new IllegalStateException("The manifest has not been configured");
        } else if(directory == null) {
            throw new IllegalStateException("The output directory has not been configured");
        }

        // Retrieve the json index declaration
        AssetIndex index = manifest.getAssetIndex();

        // Calculate the target path of the index json
        Path indexFile = directory.resolve("indexes").resolve(index.getId() + ".json");
        HttpClient httpClient = getProject().getPlugins().getPlugin(LabyfyGradlePlugin.class).getHttpClient();

        if(!Files.exists(indexFile)) {
            // The asset index file has not been downloaded yet, do so now
            getLogger().lifecycle("Downloading assets index for " + index.getId());
            Util.download(httpClient, index.getUrl().toExternalForm(), indexFile);
        }

        // Parse the index file
        JsonNode root;
        try(InputStream stream = Files.newInputStream(indexFile)) {
            root = JsonConverter.OBJECT_MAPPER.readTree(stream);
        }

        // Resolve the assets dir within the asset store
        Path assetsDir = directory.resolve("assets");

        // Get the object describing all assets
        JsonNode objects = root.get("objects").requireNonNull();
        Iterator<String> it = objects.fieldNames();

        // Iterate over every asset object
        while (it.hasNext()) {
            // Extract the required properties
            String objectName = it.next();
            String hash = objects.get(objectName).get("hash").requireNonNull().asText();

            // Calculate the download and store path
            String assetPath = hash.substring(0, 2) + "/" + hash;

            Path assetTargetPath = assetsDir.resolve(assetPath);
            if(!Files.isRegularFile(assetTargetPath)) {
                // The asset does not exist yet, download it
                getLogger().lifecycle("Downloading asset {} ({})", objectName, assetPath);
                Util.download(httpClient, ASSET_BASE_URL + assetPath, assetTargetPath);
            }
        }
    }
}
