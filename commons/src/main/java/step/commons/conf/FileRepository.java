package step.commons.conf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class FileRepository<T> {
	
	private File configFile;
	
	private URL fileUrl;
	
	private TypeReference<T> typeRef;
	
	private Class<T> objectClass;
	
	private FileRepositoryCallback<T> callback;
	
	private static Logger logger = LoggerFactory.getLogger(FileRepository.class);
	
	public FileRepository(String resourceName, Class<T> objectClass, FileRepositoryCallback<T> callback) {
		super();
		this.objectClass = objectClass;
		this.callback = callback;
		
		init(resourceName);
	}
	
	public FileRepository(String resourceName, TypeReference<T> typeRef, FileRepositoryCallback<T> callback) {
		super();
		this.typeRef = typeRef;
		this.callback = callback;
		
		init(resourceName);
	}

	public interface FileRepositoryCallback<T> {
		
		public void onLoad(T object) throws Exception;
	}
	
	public void init(String resourceName) {
		fileUrl = this.getClass().getClassLoader().getResource(resourceName);
		configFile = new File(fileUrl.getFile());
		
		loadConfigAndCallback();
		
		FileWatchService.getInstance().register(configFile, new Runnable() {
			@Override
			public void run() {
				loadConfigAndCallback();
			}
		});
		
	}
	
	public void destroy() {
		FileWatchService.getInstance().unregister(configFile);
	}
	
	private void loadConfigAndCallback() {
		try {
			T object = parseConfig();
			callback.onLoad(object);
		} catch (Exception e) {
			logger.error("Error while loading loading configuration file '"+configFile.getAbsolutePath()+"'", e);
		}
	}
	
	private T parseConfig() throws IOException {
		InputStream stream = fileUrl.openStream();
		try {
			ObjectMapper mapper = new ObjectMapper();
			if(objectClass!=null) {
				return mapper.readValue(stream, objectClass);
			} else {
				return mapper.readValue(stream, typeRef);
			}
		} finally {
			stream.close();
		}
	}
	
	
	public void save(T config) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.writeValue(configFile, config);
	}
	
}
