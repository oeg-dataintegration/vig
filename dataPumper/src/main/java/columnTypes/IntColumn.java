package columnTypes;

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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import basicDatatypes.MySqlDatatypes;
import basicDatatypes.Schema;
import basicDatatypes.Template;
import connection.DBMSConnection;

public class IntColumn extends IncrementableColumn<Long> {
	
	private int datatypeLengthFirstArgument;
	private int datatypeLengthSecondArgument;
	
	private long modulo;
	
	public IntColumn(String name, MySqlDatatypes type, int index, int datatypeLengthFirst, int datatypeLengthSecondArgument) {
		super(name, type, index);
		domain = null;
		this.max = null;
		this.min = null;
		this.lastFreshInserted = null;
		
		this.datatypeLengthFirstArgument = datatypeLengthFirst;
		this.datatypeLengthSecondArgument = datatypeLengthSecondArgument;
		
		fillModulo();
		
		index = 0;
	}
	
	public IntColumn(String name, MySqlDatatypes type, int index) {
		super(name, type, index);
		domain = null;
		this.max = null;
		this.min = null;
		this.lastFreshInserted = null;
		
		this.datatypeLengthFirstArgument = Integer.MAX_VALUE;
		this.datatypeLengthSecondArgument = 0;
		
		modulo = Long.MAX_VALUE;
		
		index = 0;
	}
	
	private void fillModulo() {
		
		StringBuilder builder = new StringBuilder();
		
		for( int i = 0; i < (datatypeLengthFirstArgument - datatypeLengthSecondArgument); ++i ){
			builder.append("9");
		}
		
		modulo = Long.parseLong(builder.toString());
		
	}

	@Override
	public String getNextFreshValue(){
		Long toInsert = this.getLastFreshInserted();
		
		do{
			toInsert = increment(toInsert);
			
			while( toInsert.compareTo(this.getCurrentMax()) == 1 && this.hasNextMax() )
				this.nextMax();
		}
		while(toInsert.compareTo(this.getCurrentMax()) == 0);
				
		this.setLastFreshInserted(toInsert);
		
		return Long.toString(toInsert);
	}

	@Override
	public void fillDomain(Schema schema, DBMSConnection db) {
		
		PreparedStatement stmt = db.getPreparedStatement("SELECT DISTINCT "+getName()+ " FROM "+schema.getTableName()+ " WHERE "+getName()+" IS NOT NULL");
		List<Long> values = null;
		
		try {
			ResultSet result = stmt.executeQuery();
			
			values = new ArrayList<Long>();
		
			while( result.next() ){
				values.add(result.getLong(1));
			}
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		setDomain(values);
	}

	@Override
	public void fillDomainBoundaries(Schema schema, DBMSConnection db) {
		
		if( lastFreshInserted != null ) return; // Boundaries already filled 
		
		Template t = new Template("select ? from "+schema.getTableName()+";");
		PreparedStatement stmt;
		
		t.setNthPlaceholder(1, "min("+getName()+"), max("+getName()+")");
		
		stmt = db.getPreparedStatement(t);
		
		ResultSet result;
		try {
			result = stmt.executeQuery();
			if( result.next() ){
				setMinValue(result.getLong(1));
				setMaxValue(result.getLong(2));
				setLastFreshInserted(result.getLong(1));
			}
			else{
				setMinValue(Long.valueOf(0));
				setMaxValue(Long.MAX_VALUE);
				setLastFreshInserted(Long.valueOf(0));
			}
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Long increment(Long toIncrement) {
		return ((toIncrement + 1) % modulo);
	}

	@Override
	public Long getCurrentMax() {
		if( domain.size() == 0 )
			return Long.MAX_VALUE;
		return domainIndex < domain.size() ? domain.get(domainIndex) : domain.get(domainIndex -1);
	}

	@Override
	public String getNextChased(DBMSConnection db, Schema schema) {
		String result = cP.pickChase(db, schema);
		
		if( result == null ) return null;
		
		long resultI = Long.parseLong(result);
		
		if( resultI > lastFreshInserted )
			lastFreshInserted = resultI;
		
		return result;
	}

	@Override
	public void proposeLastFreshInserted(String inserted) {
		
		String inserted1 = inserted;
		
		inserted1 = inserted.substring(0, inserted.indexOf("."));
			
		long resultI = Long.parseLong(inserted1);
		
		if( resultI > lastFreshInserted )
			lastFreshInserted = resultI;
	}
}
