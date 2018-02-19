package step.grid.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class ResourceExtractor {
	
	public static File extractResource(ClassLoader cl, String resourceName) {
		File gridJar;
		InputStream is = cl.getResourceAsStream(resourceName);
		try {
			gridJar = File.createTempFile(resourceName + "-" + UUID.randomUUID(), resourceName.substring(resourceName.lastIndexOf(".")));
			Files.copy(is, gridJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
			gridJar.deleteOnExit();
			return gridJar; 
		} catch (IOException e) {
			throw new RuntimeException("Error while extracting plugin file", e);
		}
	}

}