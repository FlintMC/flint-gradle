package net.labyfy.gradle.minecraft.data.manifest;

import java.util.List;

public class VersionsManifest {
	private List<MinecraftManifestVersion> versions;
	private LatestVersionInformation latest;

	public List<MinecraftManifestVersion> getVersions(){
		return versions;
	}

	public LatestVersionInformation getLatest(){
		return latest;
	}
}