package core;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import utils.Pair;
import connection.DBMSConnection;
import core.test.GeneratorTest;

public class Distribution {
	private DBMSConnection dbmsConn;
	
	private static Logger logger = Logger.getLogger(Distribution.class.getCanonicalName());
	
	public Distribution(DBMSConnection dbmsConn){
		this.dbmsConn = dbmsConn;
	}
	
	/**
	 * Returns the percentage of duplicates in various fixed-size windows of the table
	 * TODO
	 * @param columnName
	 * @param tableName
	 * @return
	 */
	public float slidingWindowStrategy(String columnName, String tableName){
				
//		PreparedStatement stmt = dbmsConn.getPreparedStatement("SELECT COUNT("+columnName+") FROM "+tableName);
//		PreparedStatement stmtProj = dbmsConn.getPreparedStatement("SELECT COUNT(DISTINCT "+columnName+") FROM "+tableName);
		
		return 0; //TODO
		
	}
	
	public float naiveStrategy(String columnName, String tableName){
		
		int nRows = nRows(columnName, tableName);
		if( nRows == 0 ) return 0; // No rows in the table
		int sizeProjection = sizeProjection(columnName, tableName);
		
		float ratio = (float)(nRows - sizeProjection) / (float)nRows;
		
		return ratio; 
	}
	
	public int nRows(String columnName, String tableName){
		PreparedStatement stmt = dbmsConn.getPreparedStatement("SELECT COUNT("+columnName+") FROM "+tableName);
		int result = 0;
		try {
			ResultSet rs = stmt.executeQuery();
			if( rs.next() )
				result = rs.getInt(1);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public int sizeProjection(String columnName, String tableName){
		PreparedStatement stmt = dbmsConn.getPreparedStatement("SELECT COUNT(DISTINCT "+columnName+") FROM "+tableName);
		int result = 0;
		try {
			ResultSet rs = stmt.executeQuery();
			if( rs.next() )
				result = rs.getInt(1);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
};
