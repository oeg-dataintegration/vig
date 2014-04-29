package mappings;

import configuration.Conf;
import connection.DBMSConnection;
import utils.MyHashMapList;
import main.java.csvPlayer.core.CSVPlayer;
import core.TuplesToCSV;

/**
 * 
 * @author tir
 * @note This is a singleton class
 */
public class MappingAnalyzer {
	private final String obdaFile;
	private final TupleStore store;
	private final DBMSConnection dbmsConn;
	
	private static String outCSVFile = "resources/mappingsCSV.csv";
	private static MappingAnalyzer instance = null;
	
	private MappingAnalyzer(DBMSConnection dbmsConn){
		
		this.obdaFile = Conf.mappingsFile();
		this.dbmsConn = dbmsConn;
		
		TuplesToCSV tuplesExtractor = new TuplesToCSV(obdaFile, outCSVFile);
		try {
			tuplesExtractor.play();
		} catch (Exception e) {
			e.printStackTrace();
		}
		CSVPlayer csvParser = new CSVPlayer(outCSVFile);
		MyHashMapList<String, String> tuplesHash = 
				new MyHashMapList<String, String>(csvParser.printCSVFile());
		
		this.store = TupleStore.getInstance(tuplesHash);
	}
	
	public static MappingAnalyzer getInstance(){
		return instance;
	}
	
	public static void setInstance(DBMSConnection dbmsConn, String obdaFile){
		if( instance != null ) return;
		instance = new MappingAnalyzer(dbmsConn);
	}

	public void initTuples(){
		
	}
	public DBMSConnection getDBMSConnection(){
		return this.dbmsConn;
	}
	
	public TupleStore getTupleStoreInstance(){
		return store;
	}
	
}
