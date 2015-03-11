package core.main;

/*
 * #%L
 * dataPumper
 * %%
 * Copyright (C) 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import persistence.LogToFile;
import columnTypes.ColumnPumper;
import columnTypes.exceptions.BoundariesUnsetException;
import columnTypes.exceptions.ValueUnsetException;
import utils.TrivialQueue;
import basicDatatypes.Schema;
import connection.DBMSConnection;
import core.table.statistics.TableStatisticsFinder;
import core.table.statistics.TableStatisticsFinderImpl;
import core.table.statistics.exception.TooManyValuesException;

public class DatabasePumperDB extends DatabasePumper {
	
	private DBMSConnection dbOriginal;
	
	private static Logger logger = Logger.getLogger(DatabasePumperDB.class.getCanonicalName());	
	
	private final TableStatisticsFinder tStatsFinder;
	
	private final LogToFile persistence;
		
	public DatabasePumperDB(DBMSConnection dbOriginal){
		this.dbOriginal = dbOriginal;
		this.tStatsFinder = new TableStatisticsFinderImpl(dbOriginal);
		this.persistence = new LogToFile();
	}
	/**
	 * 
	 * @param dbOriginal
	 * @param db
	 * @param nRows
	 */
	public void pumpDatabase(double percentage){
		
		long startTime = System.currentTimeMillis();
		
		TrivialQueue<Schema> schemas = new TrivialQueue<Schema>();
		List<ColumnPumper> listColumns = new ArrayList<ColumnPumper>();
		initListAllColumns(listColumns, percentage);
		
		try {
			establishColumnBounds(listColumns);
		} catch (ValueUnsetException e) {
			e.printStackTrace();
			dbOriginal.close();
			System.exit(1);
		}
		
		// Init the queue
		for( String tableName : dbOriginal.getAllTableNames()){
			schemas.enqueue(dbOriginal.getSchema(tableName));
		}
		
		while(schemas.hasNext()){
			Schema schema = schemas.dequeue();
			
			int nRows = dbOriginal.getNRows(schema.getTableName());
			nRows = (int) (nRows * percentage);
			logger.info("Pump "+schema.getTableName()+" of "+nRows+" rows, please.");
			
			if( schema.getTableName().equals("bsns_arr_area") ){
				logger.debug("FIXME");
			}
			
			fillDomainsForSchema(schema, dbOriginal);			
			printDomain(schema);
			
			schema.reset();
		}
		long endTime = System.currentTimeMillis();
		
		logger.info("Database pumped in " + (endTime - startTime) + " msec.");
	}

	private void printDomain(Schema schema) {
				
		List<ColumnPumper> cols = schema.getColumns();
		
		StringBuilder line = new StringBuilder();
		try {
			persistence.openFile(schema.getTableName() + ".csv");
			for( int i = 0; i < cols.get(0).getNumRowsToInsert(); ++i ){
				line.delete(0, line.length());
				for( int j = 0; j < cols.size(); ++j ){
					if( j != 0 ) line.append("`");
					
					ColumnPumper col = cols.get(j);
					line.append(col.getNthInDomain(i));
				}
//				System.out.println(line);
				persistence.appendLine(line.toString());
			}
		} catch (ValueUnsetException | IOException e) {
			e.printStackTrace();
			dbOriginal.close();
			persistence.closeFile();
			System.exit(1);
		}
		
	}
	/**
	 * 
	 * This method puts in listColumns all the columns and initializes, for each of them, 
	 * the duplicates ratio and the number of values that need to be inserted. 
	 * 
	 * Finally, it starts establishing the column bounds
	 * 
	 * @param listColumns The output
	 * @param percentage The increment ratio
	 */
	private void initListAllColumns(List<ColumnPumper> listColumns, double percentage) {
		for( String tableName : dbOriginal.getAllTableNames()){
			Schema s = dbOriginal.getSchema(tableName);
			for( ColumnPumper c : s.getColumns() ){
				listColumns.add(c);
				float dupsRatio = tStatsFinder.findDuplicatesRatio(s, c);
				c.setDuplicatesRatio(dupsRatio);
				
				float nullRatio = tStatsFinder.findNullRatio(s, c);
				c.setNullRatio(nullRatio);
				
				int nRows = dbOriginal.getNRows(s.getTableName());
				nRows = (int) (nRows * percentage);
				try {
					c.setNumRowsToInsert(nRows);
				} catch (TooManyValuesException e) {
					e.printStackTrace();
					dbOriginal.close();
					System.exit(1);
				}
			}
		}	
		
		try {
			establishColumnBounds(listColumns);
		} catch (ValueUnsetException e) {
			e.printStackTrace();
			dbOriginal.close();
			System.exit(1);
		}
	}

	private void establishColumnBounds(List<ColumnPumper> listColumns) throws ValueUnsetException{
		for( ColumnPumper cP : listColumns ){
			cP.fillDomainBoundaries(cP.getSchema(), dbOriginal);
		}
	}
	
	private void fillDomainsForSchema(Schema schema, DBMSConnection originalDb){
		for( ColumnPumper column : schema.getColumns() ){
			if( column.getName().equals("baaName")){
				logger.debug("FIXME");
			}
			
			try {
				column.generateValues(schema, originalDb);
			} catch (BoundariesUnsetException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	protected void resetDuplicateValues(Schema schema){
		for( ColumnPumper c : schema.getColumns()){
			c.reset();
		}
	}

	

};