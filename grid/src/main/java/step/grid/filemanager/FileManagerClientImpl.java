package step.grid.filemanager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import step.commons.helpers.FileHelper;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

public class FileManagerClientImpl implements FileManagerClient {
	
	File dataFolder;
	
	FileProvider fileProvider;

	public FileManagerClientImpl(File dataFolder, FileProvider fileProvider) {
		super();
		this.dataFolder = dataFolder;
		this.fileProvider = fileProvider;
	}

	static class FileInfo {
		
		String uid;
		
		File file;
		
		long lastModified;

		public FileInfo(String uid, File file, long lastModified) {
			super();
			this.uid = uid;
			this.file = file;
			this.lastModified = lastModified;
		}
	}
	
	private Map<String, FileInfo> cache = new ConcurrentHashMap<>();
	
	@Override
	public File requestFile(String uid, long lastModified) {
		FileInfo fileInfo = cache.get(uid);
		if(fileInfo==null) {
			fileInfo = requestFileAndUpdateCache(uid, lastModified);
		} else {
			if(lastModified>fileInfo.lastModified) {
				fileInfo = requestFileAndUpdateCache(uid, lastModified);
			} else {
				// local file is up to date
			}
		}
		return fileInfo.file;
	}

	private FileInfo requestFileAndUpdateCache(String uid, long lastModified) {
		File file = requestControllerFile(uid);
		return updateCache(uid, file, lastModified);
	}

	private FileInfo updateCache(String uid, File file, long lastModified) {
		FileInfo fileInfo;
		fileInfo = new FileInfo(uid, file, lastModified);
		cache.put(uid, fileInfo);
		return fileInfo;
	}
	
	private File requestControllerFile(String fileId) {
		Attachment attachment = fileProvider.getFileAsAttachment(fileId);
		
		byte[] bytes = AttachmentHelper.hexStringToByteArray(attachment.getHexContent());
		
		File container = new File(dataFolder+"/"+fileId);
		if(container.exists()) {
			container.delete();
		}
		container.mkdirs();
		container.deleteOnExit();		
		
		File file = new File(container+"/"+attachment.getName());
		if(attachment.getIsDirectory()) {
			FileHelper.extractFolder(bytes, file);
		} else {
			try {
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
				bos.write(bytes);
				bos.close();
			} catch (IOException ex) {
				
			}						
		}
		return file.getAbsoluteFile();	
	}
}
