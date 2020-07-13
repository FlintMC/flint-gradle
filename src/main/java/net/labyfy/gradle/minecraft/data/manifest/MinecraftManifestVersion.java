package net.labyfy.gradle.minecraft.data.manifest;

import java.net.URL;
import java.util.Date;

public class MinecraftManifestVersion {
	private Date releaseTime;
	private String id;
	private String time;
	private MinecraftVersionType type;
	private URL url;

	public Date getReleaseTime(){
		return releaseTime;
	}

	public String getId(){
		return id;
	}

	public String getTime(){
		return time;
	}

	public MinecraftVersionType getType(){
		return type;
	}

	public URL getUrl(){
		return url;
	}
}
