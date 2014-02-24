package core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import connection.DBMSConnection;
import core.test.GeneratorTest;
import basicDatatypes.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import utils.Pair;
import utils.Statistics;

public class Generator {
	protected DBMSConnection dbmsConn;
	protected RandomDBValuesGenerator random;
	protected Distribution distribution;
	
	protected static Logger logger = Logger.getLogger(GeneratorTest.class.getCanonicalName());
	
	// Internal state
	protected Map<String, Queue<ResultSet>> chasedValues; // Pointers to ResultSets storing chased values for each column
	protected Map<String, ResultSet> duplicateValues;     // Pointers to ResultSets storing duplicate values for each column
		
	public Generator(DBMSConnection dbmsConn){
		this.dbmsConn = dbmsConn;
		random = new RandomDBValuesGenerator();
		this.distribution = new Distribution(dbmsConn);
		
		chasedValues = new HashMap<String, Queue<ResultSet>>();
		duplicateValues = new HashMap<String, ResultSet>();
		
		logger.setLevel(Level.INFO);
	}
		
	/**
	 * @author tir
	 * @param tableName
	 * @param domainIndependentCols : May be null, and it specifies what columns HAVE TO be considered as domainIndependent.
	 * @param duplicateRatio : Probability of generating a duplicate value while populating the columns
	 * 
	 * Pump the table, column by column. Each element will be randomly chosen 
	 * according to the distribution of the database. Primary keys, instead, will
	 * be newly generated
	 * 
	 * Domain independent columns will be generated by taking values from the domain 
	 * (that is retrieved by a projection on the database column
	 *  -- therefore I assume that the db is NON-EMPTY [as in FactPages])
	 * 
	 * Domain independent columns can also be inferred, by looking at the projection and comparing it 
	 * against the total number of tuples in the table <b>tableName</b>
	 */
	public List<Schema> pumpTable(int nRows, Schema schema){		
		
		// INIT

		chasedValues.clear();  
		duplicateValues.clear();
		
		Map<String, Integer> mNumChases = new HashMap<String, Integer>(); // It holds the number of chased elements for each column
		Map<String, Integer> mNumDuplicates = new HashMap<String, Integer>(); // It holds the number of duplicates that have to be inserted
		Map<String, Integer> mNumDuplicatesFromFresh = new HashMap<String, Integer>(); // It holds the number of duplicates
		                                                                               // ---among fresh values--- that need to be inserted
		
		Map<String, Integer> mFreshGenerated = new HashMap<String, Integer>(); // It keeps fresh generated values, column by column
		Map<String, Queue<String>> mFreshDuplicates = new HashMap<String, Queue<String>>(); // TODO Is this too much memory consuming? Looks like no
		
		Map<String, List<String>> mPksDuplicatesTracker = new HashMap<String, List<String>>(); // TODO This might be consuming a lot of memory

		// Fill chased
		for( Column column : schema.getColumns() ){
			chasedValues.put(column.getName(), fillChase(column, schema.getTableName(), mNumChases));
			column.incrementCurrentChaseCycle();
			mFreshGenerated.put(column.getName(), 0);
			mFreshDuplicates.put(column.getName(), new LinkedList<String>());
		}
		
		if( nRows == 0 ){ // This is a pure-chase phase. And it is also THE ONLY place where I need to chase
			// I need to generate (at least) as many rows as the maximum among the chases
			int max = 0;
			for( String key: mNumChases.keySet() ){
				if( max < mNumChases.get(key) )
					max = mNumChases.get(key);
			}
			nRows = max; // Set the number of rows that need to be inserted.
		}
		
		// Fill duplicates
		for( Column column : schema.getColumns() ){
			duplicateValues.put(column.getName(), fillDuplicates(column, schema.getTableName(), nRows, mNumDuplicates, mNumDuplicatesFromFresh));
		}
		
		Map<String, ResultSet> referencedValues = new HashMap<String, ResultSet>();
				
		// ----- END INIT ----- //
		
		List<Schema> tablesToChase = new LinkedList<Schema>();
		
		String templateInsert = dbmsConn.createInsertTemplate(schema);
		PreparedStatement stmt = null;
		
		try {
			stmt = dbmsConn.getPreparedStatement(templateInsert);
			logger.debug(templateInsert);
			
			// Disable auto-commit
			dbmsConn.setAutoCommit(false);
			
			// Idea: I can say that nRows = number of things that need to be chased, when the maximum
			// cycle is reached. To test this out
			for( int j = 1; j <= nRows; ++j ){
				
				int columnIndex = 0;
				List<String> primaryDuplicateValues = new ArrayList<String>();
				for( Column column : schema.getColumns() ){
										
					boolean stopChase = (column.referencesTo().size() > 0) && column.getMaximumChaseCycles() < column.getCurrentChaseCycle();
					
					if( mNumChases.containsKey(column.getName()) && canAdd(nRows, j, mNumChases.get(column.getName())) )  {
						
						dbmsConn.setter(stmt, ++columnIndex, column.getType(), pickNextChased(schema, column)); // Ensures to put all chased elements, in a uniform way w.r.t. other columns
						
					}
					else if( canAdd(nRows, j, mNumDuplicates.get(column.getName()) ) ){
						
						// If, in all columns but one of the primary key I've put duplicates, 
						// pay attention to how you pick the last column. You might generate
						// a duplicate row if you do not do it correctly
						if( (primaryDuplicateValues.size() == schema.getPks().size() - 1) && column.isPrimary() ){
							// TODO This part is VERY EXPENSIVE. 
							//      Do not 
							// Force commit, because we need to check the content of the database
							stmt.executeBatch();
							dbmsConn.commit();
							
							
							// Force either
							// Check if the key constructed so far is already in the database
							Pair<Boolean, String> isDistinct_usedQuery = checkIfDistinctPk(schema, primaryDuplicateValues);
							
							boolean isDistinct = isDistinct_usedQuery.first;
							if( !isDistinct ){
								// There is the need to either
								// 1) Pick a duplicate in this value that leads to a fresh pk tuple
								// 2) Generate a fresh value
								
								// Trying to pick a duplicate in this value that leads to a fresk pk tuple
								String nextDuplicate = pickDuplicateForPk(schema, column, isDistinct_usedQuery.second);
								
								if( nextDuplicate != null ){ // We managed to find a suitable value
									dbmsConn.setter(stmt, ++columnIndex, column.getType(), nextDuplicate);
								}
								else{ // Generate a fresh value
									String generatedRandom = random.getRandomValue(column, nRows);
									dbmsConn.setter(stmt, ++columnIndex, column.getType(), generatedRandom);
									
									Statistics.addInt(schema.getTableName()+"."+column.getName()+" fresh values", 1);
									
									// New values inserted imply new column to chase
									for( QualifiedName qN : column.referencesTo() ){
										if( !tablesToChase.contains(dbmsConn.getSchema(qN.getTableName())) ){
											tablesToChase.add(dbmsConn.getSchema(qN.getTableName()));
										}
									}
								}
							}
							else{ // Add a duplicate in the normal way
								Statistics.addInt(schema.getTableName()+"."+column.getName()+" canAdd", 1);
								
								String nextDuplicate = pickNextDupFromOldValues(schema, column, (column.getCurrentChaseCycle() > column.getMaximumChaseCycles()));
								if( nextDuplicate == null ){ // Necessary to start picking duplicates from freshly generated values
									if( !mFreshDuplicates.containsKey(column.getName()) )
										logger.error("No fresh duplicates available for column "+column.getName() );
									nextDuplicate = mFreshDuplicates.get(column.getName()).poll();
									logger.debug("Polling");
									if( nextDuplicate == null ) {
										logger.error("No good poll action");	
									}
								}
								dbmsConn.setter(stmt, ++columnIndex, column.getType(), nextDuplicate); // Ensures to put all chased elements, in a uniform way w.r.t. other columns
							}
						}else{
							
							Statistics.addInt(schema.getTableName()+"."+column.getName()+" canAdd", 1);
							
							String nextDuplicate = pickNextDupFromOldValues(schema, column, (column.getCurrentChaseCycle() > column.getMaximumChaseCycles()));
							if( nextDuplicate == null ){ // Necessary to start picking duplicates from freshly generated values
								if( !mFreshDuplicates.containsKey(column.getName()) )
									logger.error("No fresh duplicates available for column "+column.getName() );
								nextDuplicate = mFreshDuplicates.get(column.getName()).poll();
								logger.debug("Polling");
								if( nextDuplicate == null ) {
									logger.error("No good poll action");	
								}
							}
							if( column.isPrimary() ){
								primaryDuplicateValues.add(nextDuplicate);
							}
							dbmsConn.setter(stmt, ++columnIndex, column.getType(), nextDuplicate); // Ensures to put all chased elements, in a uniform way w.r.t. other columns
						}
					}
					else if( stopChase ){
						// We cannot take a chase value, neither we can pick a duplicate. The only way out is 
						// to tale the necessary number of elements (non-duplicate with this column) from the referenced column(s)
						dbmsConn.setter(stmt, ++columnIndex, column.getType(), pickFromReferenced(schema, column, referencedValues));
					}
					else{ // Add a random value; if I want to duplicate afterwards, keep it in freshDuplicates list
						
						String generatedRandom = random.getRandomValue(column, nRows);
						dbmsConn.setter(stmt, ++columnIndex, column.getType(), generatedRandom);
						
						Statistics.addInt(schema.getTableName()+"."+column.getName()+" fresh values", 1);
						
						// Add the random value to the "toInsert" duplicates
						int freshGenerated = mFreshGenerated.get(column.getName());
						if( freshGenerated++ < mNumDuplicatesFromFresh.get(column.getName()) ){
							logger.debug(mNumDuplicatesFromFresh.get(column.getName()));
							mFreshGenerated.put(column.getName(), freshGenerated);
							mFreshDuplicates.get(column.getName()).add(generatedRandom);
						}
						// New values inserted imply new column to chase
						for( QualifiedName qN : column.referencesTo() ){
							if( !tablesToChase.contains(dbmsConn.getSchema(qN.getTableName())) ){
								tablesToChase.add(dbmsConn.getSchema(qN.getTableName()));
							}
						}
					}
				}
				stmt.addBatch();
				if( j % 1000000 == 0 ){ // Let's put a limit to the dimension of the stmt 
					stmt.executeBatch();	
					dbmsConn.commit();
				}
			} 
			stmt.executeBatch();	
			dbmsConn.commit();
		} catch (SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		dbmsConn.setAutoCommit(true);
		return tablesToChase; 
	}
	
	protected String pickDuplicateForPk(Schema schema, Column column, String notInQuery) {
		
			
		// SELECT column.getName from schemaName where column.getName NOT IN (notInQuery)
		Template temp = new Template("SELECT ? FROM ? WHERE ? NOT IN (?) LIMIT 1");
		
		temp.setNthPlaceholder(1, column.getName());
		temp.setNthPlaceholder(2, schema.getTableName());
		temp.setNthPlaceholder(3, column.getName());
		temp.setNthPlaceholder(4, notInQuery);
		
		PreparedStatement stmt = dbmsConn.getPreparedStatement(temp);
		
		logger.debug(temp.getFilled());
		
		try {
			ResultSet rs = stmt.executeQuery();
			if( rs.next() ){
				return rs.getString(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected Pair<Boolean, String> checkIfDistinctPk(Schema s, List<String> primaryDuplicateValues) {
		
		Pair<Boolean, String> result;
		
		// SELECT z FROM table WHERE (x, y, z) \in Table
		
		StringBuilder builder = new StringBuilder();
		String lastKeyColumn = s.getPks().get(s.getPks().size()-1).getName();
		
		builder.append("SELECT "+ lastKeyColumn + " FROM " + s.getTableName() +" WHERE ");
		for( int i = 0; i < primaryDuplicateValues.size(); ++i ){
			String left = s.getPks().get(i).getName();
			String right = primaryDuplicateValues.get(i);
			builder.append(left + "=" + right);
			
			if( i != (primaryDuplicateValues.size() -1) )
				builder.append(", ");
		}
		
		logger.debug(builder);
		
		result = new Pair<Boolean, String>(true, builder.toString());
		
		PreparedStatement stmt = dbmsConn.getPreparedStatement(builder.toString());
		
		try {
			ResultSet rs = stmt.executeQuery();
			
			if( rs.next() ){
				result.first = false;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	/**
	 * This method, for the moment, assumes that it is possible
	 * to reference AT MOST 1 TABLE.
	 * NOT VERY EFFICIENT. If slow, then refactor as the others
	 * @param schema
	 * @param column
	 * @return
	 */
	protected String pickFromReferenced(Schema schema, Column column, Map<String, ResultSet> referencedValues) {
		
		String result = null;
		
		if( !referencedValues.containsKey(column.getName()) ){
			
			// SELECT referencedColumn FROM referencedTable WHERE referencedColumn NOT IN (select thisColumn from thisTable)
			Template templ = new Template("SELECT DISTINCT ? FROM ? WHERE ? NOT IN (SELECT ? FROM ?)");
			
			if( !column.referencesTo().isEmpty() ){
				QualifiedName refQN = column.referencesTo().get(0);
				templ.setNthPlaceholder(1, refQN.getColName());
				templ.setNthPlaceholder(2, refQN.getTableName());
				templ.setNthPlaceholder(3, refQN.getColName());
				templ.setNthPlaceholder(4, column.getName());
				templ.setNthPlaceholder(5, schema.getTableName());
			}
			else{
				logger.error("Cannot access a referenced field");
			}
			
			PreparedStatement stmt = dbmsConn.getPreparedStatement(templ);
			try {
				referencedValues.put(column.getName(), stmt.executeQuery());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		ResultSet rs = referencedValues.get(column.getName());
		
		try {
			if( !rs.next() ){
				logger.error("Not possible to add a non-duplicate value");
				throw new SQLException();
			}
			result = rs.getString(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	/**
	 * 
	 * @param column
	 * @param tableName
	 * @param nRowsToInsert
	 * @return A result set containing the number of duplicates---taken from the original set--- that need to be inserted
	 */
	protected ResultSet fillDuplicates(Column column, String tableName, int nRowsToInsert, 
			Map<String, Integer> mNumDuplicates, Map<String, Integer> mNumDuplicatesFromFresh) {
		
		ResultSet result = null;
		
		float ratio; // Ratio of the duplicates
		boolean maxChaseReached = false;
		// If generating fresh values will lead to a chase, and the maximum number of chases is reached
		if( (column.referencesTo().size() > 0) && column.getMaximumChaseCycles() < column.getCurrentChaseCycle() ){
			// It has NOT to produce fresh values
			// However, if the field is a allDifferent() then I cannot close the chase with a duplicate
			if( column.isAllDifferent() ){
				mNumDuplicates.put(column.getName(), 0);
				mNumDuplicatesFromFresh.put(column.getName(), 0);
				return null; // Either no duplicates or no row at all
			}
			// Else, it is ok to close the cycle with a duplicate
			ratio = 1;
			maxChaseReached = true;
		}
		else{
			
			// First of all, I need to understand the distribution of duplicates. Window analysis!
			ratio = distribution.naiveStrategy(column.getName(), tableName);
			Statistics.addFloat(tableName+"."+column.getName()+" dups ratio", ratio);
		}
		
		if( (ratio == 0) && !maxChaseReached){ // Is not( maxChaseReached ) necessary here? 
			mNumDuplicates.put(column.getName(), 0);
			mNumDuplicatesFromFresh.put(column.getName(), 0);
			return null; // Either no duplicates or no row at all
		}
		
		int curRows = distribution.nRows(column.getName(), tableName);
		int curDuplicates = (curRows - distribution.sizeProjection(column.getName(), tableName));
		
		// This is the total number of duplicates that have to be in the final table
		int nDups = maxChaseReached ? nRowsToInsert + curDuplicates : (int)((nRowsToInsert + curRows) * ratio); 
		                                                                   
		
		Statistics.addInt(tableName+"."+column.getName()+"final total dups", nDups);
		
		int toAddDups = nDups - curDuplicates;
		
		Statistics.addInt(tableName+"."+column.getName()+"to add dups", toAddDups);
		
		mNumDuplicates.put(column.getName(), toAddDups);
		
		
		// Now, establish how many rows I want to take from the current content
		int nDupsFromCurCol = /*Math.round*/(int)((curRows / (curRows + nRowsToInsert)) * ratio);
		if( maxChaseReached )
			nDupsFromCurCol = toAddDups;
		
		// And how many rows I want to take from fresh randomly generated values.
		mNumDuplicatesFromFresh.put(column.getName(), toAddDups - nDupsFromCurCol);
		
		// Point to the rows that I want to duplicate
		if( curRows <= nDupsFromCurCol ){
			logger.debug("curRows= "+curRows+", nDupsFromCol= "+nDupsFromCurCol);
			nDupsFromCurCol = curRows; // Just take all of them.
		}
		int indexMin = curRows - nDupsFromCurCol == 0 ? 0 : random.getRandomInt(curRows - nDupsFromCurCol);
		
 		String queryString = "SELECT "+column.getName()+ " FROM "+tableName+" LIMIT "+indexMin+", "+nDupsFromCurCol;
		
		try{
			PreparedStatement stmt = dbmsConn.getPreparedStatement(queryString);
			result = stmt.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * Side effect on <b>numChased</b>
	 * @param column
	 * @param tableName
	 * @param numChased
	 * @return
	 */
	protected Queue<ResultSet> fillChase(Column column, String tableName, Map<String, Integer> mNumChases) {
		Queue<ResultSet> result = new LinkedList<ResultSet>(); 
		
		// SELECT referredByCol FROM referredByTable WHERE referredByCol NOT IN (SELECT column.name() FROM schema.name()); 
		Template query = new Template("SELECT DISTINCT ? FROM ? WHERE ? NOT IN (SELECT ? FROM ?)");
		Template queryCount = new Template("SELECT COUNT(DISTINCT ?) FROM ? WHERE ? NOT IN (SELECT ? FROM ?)");
		
		for( QualifiedName referencedBy : column.referencedBy() ){
			// Fill the query
			query.setNthPlaceholder(1,referencedBy.getColName());
			query.setNthPlaceholder(2, referencedBy.getTableName());
			query.setNthPlaceholder(3, referencedBy.getColName());
			query.setNthPlaceholder(4, column.getName());
			query.setNthPlaceholder(5, tableName);
			
			queryCount.setNthPlaceholder(1,referencedBy.getColName());
			queryCount.setNthPlaceholder(2, referencedBy.getTableName());
			queryCount.setNthPlaceholder(3, referencedBy.getColName());
			queryCount.setNthPlaceholder(4, column.getName());
			queryCount.setNthPlaceholder(5, tableName);
			
			try {
				PreparedStatement stmt = dbmsConn.getPreparedStatement(query);
				ResultSet rs = stmt.executeQuery();
				result.add(rs);
				
				PreparedStatement stmt1 = dbmsConn.getPreparedStatement(queryCount);
				ResultSet rs1 = stmt1.executeQuery();
				if(rs1.next()){
					if(mNumChases.containsKey(column.getName())){
						mNumChases.put(column.getName(), mNumChases.get(column.getName()) + rs.getInt(1)); // Add to the current value
					}
					else{
						mNumChases.put(column.getName(), rs1.getInt(1)); // Create a new entry for the column
					}
				}
				else{
					logger.error("Empty resultset.");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * 
	 * @param schema
	 * @param column
	 * @param force It forces to pick an element even if the cursor already reached the end
	 * @return
	 */
	protected String pickNextDupFromOldValues(Schema schema, Column column, boolean force) {
		
		ResultSet duplicatesToInsert = duplicateValues.get(column.getName());
		if(duplicatesToInsert == null) return null;
		String result = null;
		
		try {
			boolean hasNext = duplicatesToInsert.next();
			if( !hasNext && force ){
				duplicatesToInsert.beforeFirst();
				if( !duplicatesToInsert.next() )
					logger.error("No duplicate element can be forced");
			}
			else if( !hasNext && !force ){
				return null;
			}
			result = duplicatesToInsert.getString(1);
			if(force) // rebring the pointer to the end
				duplicatesToInsert.afterLast();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	protected String pickNextChased(Schema schema, Column column) {
		
		Queue<ResultSet> chased = chasedValues.get(column.getName());
		ResultSet curSet = chased.peek();

		if(curSet == null){ // This shall not happen
			logger.debug("Problem: Picked a null in chased vector"); 
			return null;
		}
		try {
			if(curSet.next()){
				return curSet.getString(1);
			}
			else{ // Pick next ResultSet
				chased.poll();
				curSet = chased.peek();
				if(curSet == null ) return null;
				
				if( curSet.next() == false ) 
					logger.debug("Problem: No element in a non-empty ResultSet");
				curSet.getString(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}		
		return null;
	}

	protected boolean canAdd(int total, int current, int modulo) {
		if(modulo == 0) return false;
//		if(current == total-1 && modulo != 0) return true;
		float thing = (float)current % ((float)total / (float)modulo);
		logger.debug(thing);
		return thing < 1;
	}
};