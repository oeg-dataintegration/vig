package it.unibz.inf.data_pumper.tables;

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

import it.unibz.inf.data_pumper.columns.BigDecimalColumn;
import it.unibz.inf.data_pumper.columns.ColumnPumper;
import it.unibz.inf.data_pumper.columns.DateTimeColumn;
import it.unibz.inf.data_pumper.columns.IntColumn;
import it.unibz.inf.data_pumper.columns.StringColumn;
import it.unibz.inf.data_pumper.tables.MySqlDatatypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

public class Schema{
    private List<ColumnPumper<? extends Object>> columns;
    private final String tableName;  // Final in order to avoid the well-known "mutability" problem with the <i>equals</i> method.
    private List<ColumnPumper<? extends Object>> primaryKey;

    // Fields related to the pumping
    private boolean filledFlag; // It keeps the information whether this schema has been already pumped once
    private int maxDupsRepetition;

    private int originalSize;

    private static Logger logger = Logger.getLogger(Schema.class.getCanonicalName());

    public Schema(String tableName){
	this.tableName = tableName;
	columns = new ArrayList<ColumnPumper<? extends Object>>();
	primaryKey = new ArrayList<ColumnPumper<? extends Object>>();
	filledFlag = false;
	maxDupsRepetition = 0;
	originalSize = 0;
    }

    public int getOriginalSize(){
	return originalSize;
    }

    public void setOriginalSize(int size){
	this.originalSize = size;
    }

    public int getMaxDupsRepetition(){
	return maxDupsRepetition;
    }

    public void setMaxDupsRepetition(int maxDupsRepetition){
	this.maxDupsRepetition = maxDupsRepetition;
    }

    public void setFilled(){
	filledFlag = true;
    }

    public boolean isFilled(){
	return filledFlag;
    }

    public void addColumn(String colName, String typeString, int index){

	if( typeString.startsWith("int") ) columns.add(new IntColumn(colName, MySqlDatatypes.INT, index, this));
	else if( typeString.startsWith("smallint") ) columns.add(new IntColumn(colName, MySqlDatatypes.INT, index, this));
	else if( typeString.startsWith("decimal") ) columns.add(new IntColumn(colName, MySqlDatatypes.INT, index, TypeStringParser.getFirstBinaryDatatypeSize(typeString), TypeStringParser.getSecondBinaryDatatypeSize(typeString), this));
	else if( typeString.startsWith("double") )
	    columns.add(new BigDecimalColumn(colName, MySqlDatatypes.DOUBLE, index, this));
	else if( typeString.startsWith("bigint") ) columns.add(new BigDecimalColumn(colName, MySqlDatatypes.BIGINT, index, this));
	else if( typeString.startsWith("char") ) columns.add(new StringColumn(colName, MySqlDatatypes.VARCHAR, index, TypeStringParser.getUnaryDatatypeSize(typeString), this));
	else if( typeString.startsWith("varchar") ) columns.add(new StringColumn(colName, MySqlDatatypes.VARCHAR, index, TypeStringParser.getUnaryDatatypeSize(typeString), this));
	else if( typeString.startsWith("text") ) columns.add(new StringColumn(colName, MySqlDatatypes.VARCHAR, index, this));
	else if( typeString.startsWith("longtext") ) columns.add(new StringColumn(colName, MySqlDatatypes.VARCHAR, index, this));
	else if( typeString.startsWith("datetime") ) columns.add(new DateTimeColumn(colName, MySqlDatatypes.DATETIME, index, this));
	else if( typeString.startsWith("date") ) columns.add(new DateTimeColumn(colName, MySqlDatatypes.DATETIME, index, this));
	else if( typeString.startsWith("timestamp") ) 
	    columns.add(new DateTimeColumn(colName, MySqlDatatypes.DATETIME, index, this));
	else{
	    logger.error("SUPPORT FOR TYPE: "+ typeString +" IS MISSING.");
	}
    }

    public ColumnPumper<? extends Object> getColumn(String colName){
	for( ColumnPumper<? extends Object> col : columns ){
	    if( col.getName().equals(colName) )
		return col;
	}
	return null;
    }
    /**
     * Returns a list of all columns. Side-effects if the list is changed
     * @return
     */
    public List<ColumnPumper<? extends Object>> getColumns(){
	return Collections.unmodifiableList(columns);
    }	
    public String getTableName(){
	return tableName;
    }

    public int getNumColumns(){
	return columns.size();
    }

    public String toString(){
	return tableName;
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

    public List<ColumnPumper<? extends Object>> getPk(){
	if( primaryKey.size() == 0 ){

	    // INIT
	    for( ColumnPumper<? extends Object> c : columns ){
		if( c.isPrimary() )
		    primaryKey.add(c);
	    }
	}
	return primaryKey;
    }

    public void resetColumnsDomains() {
	for( ColumnPumper<? extends Object> cP : columns ){
	    cP.resetDomain();
	}
    }
}
class TypeStringParser{

    static int getFirstBinaryDatatypeSize(String toParse){
	int indexStart = toParse.indexOf("(") + 1;
	int indexEnd = toParse.indexOf(",");

	return Integer.parseInt(toParse.substring(indexStart, indexEnd));
    }

    static int getSecondBinaryDatatypeSize(String toParse){
	int indexStart = toParse.indexOf(",") + 1;
	int indexEnd = toParse.indexOf(")");

	return Integer.parseInt(toParse.substring(indexStart, indexEnd));
    }

    static int getUnaryDatatypeSize(String toParse){
	int indexStart = toParse.indexOf("(") + 1;
	int indexEnd = toParse.indexOf(")");

	return Integer.parseInt(toParse.substring(indexStart, indexEnd));
    }
}