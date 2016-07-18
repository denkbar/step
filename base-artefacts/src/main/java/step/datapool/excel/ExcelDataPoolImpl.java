package step.datapool.excel;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.ForEachBlock;
import step.core.miscellaneous.ValidationException;
import step.core.variables.SimpleStringMap;
import step.datapool.DataSet;

public class ExcelDataPoolImpl extends DataSet {
	
	private static Logger logger = LoggerFactory.getLogger(ExcelDataPoolImpl.class);
	
	private Object lock;
	
	WorkbookSet workbookSet;
	
	ForEachBlock configuration;
	
	Sheet sheet;
	
	int cursor;
	
	private static String SHEETNAME_SEPARATOR = "::";
	
	volatile boolean updated = false;
	
	static Pattern crossSheetPattern = Pattern.compile("^(.+?)::(.+?)$");
			
	public ExcelDataPoolImpl(ForEachBlock configuration) {
		super();
		
		this.configuration = configuration;
		this.lock = new Object();
	}

	@Override
	public void reset_() {
		synchronized (lock) {			
			String workbookPath = configuration.getTable();
			workbookPath = workbookPath.trim();
			String bookName = "";
			String sheetName = "";
			
			/* In BookName und Sheetname splitten */
			String [] arr = workbookPath.split(SHEETNAME_SEPARATOR);
			if (arr != null && arr.length == 2){
				bookName = arr[0].trim();
				sheetName = arr[1].trim();
			} else if (arr != null && arr.length == 1){
				if (workbookPath.startsWith(SHEETNAME_SEPARATOR)){
					sheetName = arr[0].trim();
				} else{
					bookName = arr[0].trim();
				}
			}
			
			logger.debug("book: " + bookName + " sheet: " + sheetName);
			
			File workBookFile = ExcelFileLookup.lookup(bookName);
			
			workbookSet = new WorkbookSet(workBookFile, ExcelFunctions.getMaxExcelSize(), false, true);

			Workbook workbook = workbookSet.getMainWorkbook();
			
			if (sheetName.isEmpty()){
				sheet = workbook.getSheetAt(0);
			} else {
				sheet = workbook.getSheet(sheetName);
				if (sheet == null){
					throw new ValidationException("The sheet " + sheetName + " doesn't exist in the workbook " + workBookFile.getName());
				}
			}
			
			if(configuration.getHeader() == null || configuration.getHeader() == true) {
				cursor = 0;
			} else {
				cursor = -1;
			}
		}
	}
	
	private int mapHeaderToCellNum(Sheet sheet, String header) {
		if(configuration.getHeader() == null || configuration.getHeader() == true) {
			Row row = sheet.getRow(0);
			for(Cell cell:row) {
				String key = ExcelFunctions.getCellValueAsString(cell, workbookSet.getMainFormulaEvaluator());
				if(key!=null && key.equals(header)) {
					return cell.getColumnIndex();
				}
			}
			throw new ValidationException("The column " + header + " doesn't exist in sheet " + sheet.getSheetName());
		} else {
			return CellReference.convertColStringToIndex(header);
		}
	}
	
	private static final String SKIP_STRING = "@SKIP"; 

	@Override
	public Object next_() {		
		synchronized (lock) {			
			for(;;) {
				cursor++;
				if(cursor <= sheet.getLastRowNum()){
					Cell cell = (sheet.getRow(cursor)).getCell(0);
					if (cell != null){
						String value = ExcelFunctions.getCellValueAsString(cell, workbookSet.getMainFormulaEvaluator());
						if (value != null && !value.isEmpty()){
							if (value.equals(SKIP_STRING)) {
								continue;
							} else {
								return new RowWrapper(cursor);
							}
						} else {
							return null;
						}
					} else {
						return null;
					}
				} else {
					return null;			
				}
			}
		}
	}

	@Override
	public void save() {
		synchronized (lock) {
			if(updated) {
				try {
					workbookSet.save();
				} catch (IOException e) {
					throw new RuntimeException("Error writing file " + workbookSet.getMainWorkbookFile().getAbsolutePath(), e);
				}
			}
		}
	}

	@Override
	public void close() {
		synchronized (lock) {			
			workbookSet.close();
		}

		sheet = null;
	}
	
	private Cell getCellByID(int cursor, String name) {
		Sheet sheet;
		String colName;
		
		Matcher matcher = crossSheetPattern.matcher(name);
		if(matcher.find()) {
			String sheetName = matcher.group(1);
			colName = matcher.group(2);
			
			sheet = workbookSet.getMainWorkbook().getSheet(sheetName);

			if (sheet == null) {
				throw new ValidationException("The sheet " + sheetName
						+ " doesn't exist in the workbook " + workbookSet.getMainWorkbookFile().getName());
			}
		} else {
			sheet = this.sheet;
			colName = name;
		}
				
		int cellNum = mapHeaderToCellNum(sheet, colName);
		Row row = sheet.getRow(cursor);
		if(row==null) {
			row = sheet.createRow(cursor);
		}
		Cell cell = row.getCell(cellNum, Row.CREATE_NULL_AS_BLANK);
		
		return cell;
	}
	
	private class RowWrapper extends SimpleStringMap {
		
		private final int cursor;

		public RowWrapper(int cursor) {
			super();
			this.cursor = cursor;
		}

		@Override
		public String get(String key) {
			synchronized(lock) {
				Cell cell = getCellByID(cursor, key);
				return ExcelFunctions.getCellValueAsString(cell, workbookSet.getMainFormulaEvaluator());
			}
		}

		@Override
		public String put(String key, String value) {
			synchronized(lock) {
				Cell cell = getCellByID(cursor, key);
				if(cell!=null) {
					updated = true;
						cell.setCellValue(value);
						workbookSet.getMainFormulaEvaluator().notifyUpdateCell(cell);
					}
				return value;
			}	
		}
	}

}
