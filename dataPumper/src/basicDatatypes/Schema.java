package basicDatatypes;

import java.util.ArrayList;
import java.util.List;

import basicDatatypes.MySqlDatatypes;

public class Schema{
	private List<Column> columns;
	private final String tableName;  // Final in order to avoid the well-known "mutability" problem with the <i>equals</i> method.

	public Schema(String tableName){
		this.tableName = tableName;
		columns = new ArrayList<Column>();
	}
	
	public void addColumn(String colName, String typeString){
		
		if( typeString.startsWith("int") ) columns.add(new Column(colName, MySqlDatatypes.INT));
		else if( typeString.startsWith("char") ) columns.add(new Column(colName, MySqlDatatypes.CHAR));
		else if( typeString.startsWith("varchar") ) columns.add(new Column(colName, MySqlDatatypes.VARCHAR));
		else if( typeString.startsWith("text") ) columns.add(new Column(colName, MySqlDatatypes.TEXT));
		else if( typeString.startsWith("longtext") ) columns.add(new Column(colName, MySqlDatatypes.LONGTEXT));
		else if( typeString.startsWith("datetime") ) columns.add(new Column(colName, MySqlDatatypes.DATETIME));
		else if( typeString.startsWith("point") ) columns.add(new Column(colName, MySqlDatatypes.POINT));
		else if( typeString.startsWith("linestring") ) columns.add(new Column(colName, MySqlDatatypes.LINESTRING));
		else if( typeString.startsWith("multilinestring") ) columns.add(new Column(colName, MySqlDatatypes.MULTILINESTRING));
		else if( typeString.startsWith("polygon") ) columns.add(new Column(colName, MySqlDatatypes.POLYGON));
		else if( typeString.startsWith("multipolygon") ) columns.add(new Column(colName, MySqlDatatypes.MULTIPOLYGON));
	}
	
	public Column getColumn(String colName){
		for( Column col : columns ){
			if( col.getName().equals(colName) )
				return col;
		}
		return null;
	}
	/**
	 * Returns a list of all columns. Side-effects if the list is changed
	 * @return
	 */
	public List<Column> getColumns(){
		return columns;
	}	
	public String getTableName(){
		return tableName;
	}
	
	public int getNumColumns(){
		return columns.size();
	}
	
	public String toString(){
		return columns.toString();
	}

	public boolean isIndependent() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean equals(Object s){
		if(! (s instanceof Schema) ) return false;
		
		return this.getTableName().equals(((Schema)s).getTableName());
	}
	@Override
	public int hashCode(){
		return this.getTableName().hashCode();
		
	}
}