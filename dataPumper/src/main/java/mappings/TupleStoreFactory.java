package mappings;

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

import utils.MyHashMapList;
import configuration.Conf;
import connection.DBMSConnection;
import core.CSVPlayer;
import core.TuplesToCSV;

/**
 * 
 * @author tir
 * @note This is a singleton class
 */
public class TupleStoreFactory {
	private final String obdaFile;
	private final TupleStore store;
	private final TuplesPicker picker;
	private final DBMSConnection dbmsConnOriginal;
	
	private static String outCSVFile = "resources/mappingsCSV.csv";
	private static TupleStoreFactory instance = null;
	
	private TupleStoreFactory(DBMSConnection dbmsConnOriginal){
		
		this.obdaFile = Conf.mappingsFile();
		this.dbmsConnOriginal = dbmsConnOriginal;
		this.picker = TuplesPicker.getInstance();
		
		TuplesToCSV tuplesExtractor = new TuplesToCSV(obdaFile, outCSVFile);
		try {
			tuplesExtractor.play();
		} catch (Exception e) {
			e.printStackTrace();
		}
		CSVPlayer csvParser = new CSVPlayer(outCSVFile);
		System.err.println(csvParser.printCSVFile());
		MyHashMapList<String, String> tuplesHash = 
				MyHashMapList.parse(csvParser.printCSVFile());
		
		this.store = TupleStore.getInstance(tuplesHash);
	}
	
	public static TupleStoreFactory getInstance(){
		return instance;
	}
	
	public static void setInstance(DBMSConnection dbmsConnOriginal, String obdaFile){
		if( instance != null ) return;
		instance = new TupleStoreFactory(dbmsConnOriginal);
	}

	public DBMSConnection getDBMSConnection(){
		return this.dbmsConnOriginal;
	}
	
	public TupleStore getTupleStoreInstance(){
		return store;
	}
	
	public TuplesPicker getTuplesPickerInstance(){
		return picker;
	}
	
}
