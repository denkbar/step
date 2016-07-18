package step.commons.conf;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWatchService extends Thread {
	
	private static final Logger logger = LoggerFactory.getLogger(FileWatchService.class);
		
	private final HashMap<File, Subscription> subscriptions = new HashMap<>();
	
	private int interval = 1000;
	
	private static FileWatchService INSTANCE = new FileWatchService();
	
	public static FileWatchService getInstance() {
		return INSTANCE;
	}
	
	private FileWatchService() {
		super();
		
		start();
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	@Override
	public void run() {
		super.run();
		
		try {
			while(true) {
				Thread.sleep(interval);
				
				synchronized (subscriptions) {
					for(Entry<File, Subscription> entry:subscriptions.entrySet()) {
						if(entry.getKey().lastModified()>entry.getValue().lastupdate) {
							logger.info("Reloading file: " + entry.getKey().getAbsolutePath());
							entry.getValue().lastupdate = entry.getKey().lastModified();
							try {
								entry.getValue().callback.run();
							} catch (Exception e) {
								logger.error("An error occurred while calling callback for file " + entry.getKey(), e);
							}
						}
					}
				}
			}
		} catch (InterruptedException e) {
			logger.error("Thread interrupted", e);
		}
	}

	private class Subscription {
		
		long lastupdate;
		
		Runnable callback;

		public Subscription(long lastupdate, Runnable callback) {
			super();
			this.lastupdate = lastupdate;
			this.callback = callback;
		}
	}
	
	public void register(File file, Runnable callback) {
		synchronized (subscriptions) {
			logger.debug("Registering file " + file);
			subscriptions.put(file, new Subscription(file.lastModified(), callback));
		}
	}
	
	public void unregister(File file) {
		synchronized (subscriptions) {
			logger.debug("Unregistering file " + file);
			subscriptions.remove(file);
		}
	}
}
