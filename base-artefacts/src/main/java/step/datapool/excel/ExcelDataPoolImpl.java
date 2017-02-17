/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.datapool.excel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.miscellaneous.ValidationException;
import step.core.variables.SimpleStringMap;
import step.datapool.DataSet;

public class ExcelDataPoolImpl extends DataSet {
	
	private static Logger logger = LoggerFactory.getLogger(ExcelDataPoolImpl.class);
	
	private Object lock;
	
	WorkbookSet workbookSet;
		
	Sheet sheet;
	
	int cursor;
		
	volatile boolean updated = false;
	
	static Pattern crossSheetPattern = Pattern.compile("^(.+?)::(.+?)$");
			
	public ExcelDataPoolImpl(JSONObject configuration) {
		super(configuration);
		this.lock = new Object();
	}

	@Override
	public void reset_() {
		synchronized (lock) {			
			String bookName = configuration.getString("file");
			String sheetName = configuration.has("worksheet")?configuration.getString("worksheet"):null;
			
			
			logger.debug("book: " + bookName + " sheet: " + sheetName);
			
			File workBookFile = ExcelFileLookup.lookup(bookName);
			
			workbookSet = new WorkbookSet(workBookFile, ExcelFunctions.getMaxExcelSize(), false, true);

			Workbook workbook = workbookSet.getMainWorkbook();
			
			if (sheetName==null){
				sheet = workbook.getSheetAt(0);
			} else {
				sheet = workbook.getSheet(sheetName);
				if (sheet == null){
					throw new ValidationException("The sheet " + sheetName + " doesn't exist in the workbook " + workBookFile.getName());
				}
			}
			
			if(configuration.getBoolean("headers")) {
				cursor = 0;
			} else {
				cursor = -1;
			}
		}
	}
	
	private int mapHeaderToCellNum(Sheet sheet, String header) {
		if(configuration.getBoolean("headers")) {
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
	
	private List<String> getHeaders() {
		List<String> headers = new ArrayList<>();
		Row row = sheet.getRow(0);
		for(Cell cell:row) {
			String key = ExcelFunctions.getCellValueAsString(cell, workbookSet.getMainFormulaEvaluator());
			headers.add(key);
		}
		return headers;
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
		public Set<String> keySet() {
			Set<String> headers = new HashSet<>(getHeaders());
			return headers;
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
