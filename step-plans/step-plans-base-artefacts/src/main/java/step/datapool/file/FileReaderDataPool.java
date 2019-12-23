package step.datapool.file;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.attachments.FileResolver;
import step.datapool.DataSet;

public abstract class FileReaderDataPool extends DataSet<FileDataPool> {

	private static final Logger logger = LoggerFactory.getLogger(FileReaderDataPool.class);
	
	public FileReaderDataPool(FileDataPool configuration) {
		super(configuration);
	}

	BufferedReader br;
	String filePath;

	int lineNr;

	@Override
	public void init() {
		super.init();
		
		String file = this.configuration.getFile().get();
		if (file == null || file.length() < 1)
			throw new RuntimeException("file path is incorrect.");
		
		FileResolver fileResolver = context.get(FileResolver.class);
		filePath = fileResolver.resolve(file).getAbsolutePath();
		
		initReader();
		doFirst_();
	}
	
	private void initReader(){
		FileReader in = null;
		try {
			in = new FileReader(filePath);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Could not open file :" + filePath + ". error was:" + e.getMessage());
		}
		br = new BufferedReader(in);
		this.lineNr = 1;
	}

	@Override
	public void reset() {
		try {
			br.close();
		} catch (IOException e) {
			logger.error("Error while closing reader", e);
		}
		
		initReader();
		doFirst_();
	}

	public abstract void doFirst_();

	@Override
	public Object next_() {
		String line = readOneLine();
		if (line == null)
			return null;
		else
			return postProcess(line);
	}
	
	protected String readOneLine(){
		String line;
		try {
			line = br.readLine();
		} catch (IOException e) {
			throw new RuntimeException("Could not read line from file " + this.filePath + ". Error was:" + e.getMessage());
		}
		this.lineNr++;
		return line;
	}

	public abstract Object postProcess(String line);

	@Override
	public void close() {
		super.close();
		try {
			if(br!=null) {
				br.close();				
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not close reader properly for file " +  this.filePath + ". Error was:" + e.getMessage());
		}
	}

}
