package it.unibz.inf.data_pumper.core.main;

import it.unibz.inf.data_pumper.basic_datatypes.QualifiedName;
import it.unibz.inf.data_pumper.column_types.ColumnPumper;
import it.unibz.inf.data_pumper.column_types.exceptions.BoundariesUnsetException;
import it.unibz.inf.data_pumper.column_types.exceptions.ValueUnsetException;
import it.unibz.inf.data_pumper.column_types.intervals.Interval;
import it.unibz.inf.data_pumper.configuration.Conf;
import it.unibz.inf.data_pumper.connection.DBMSConnection;
import it.unibz.inf.data_pumper.connection.exceptions.InstanceNullException;
import it.unibz.inf.vig_mappings_analyzer.core.JoinableColumnsFinder;
import it.unibz.inf.vig_mappings_analyzer.datatypes.Argument;
import it.unibz.inf.vig_mappings_analyzer.datatypes.Field;
import it.unibz.inf.vig_mappings_analyzer.datatypes.FunctionTemplate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;

/**
 * @author tir
 *
 */
public class DatabasePumperOBDA extends DatabasePumperDB {
	
	// Aggregated classes
	private CorrelatedColumnsExtractor cCE;
		
	public DatabasePumperOBDA() {
		super();
		try {
			JoinableColumnsFinder jCF = new JoinableColumnsFinder(Conf.getInstance().mappingsFile());
			this.cCE = new CorrelatedColumnsExtractor(jCF);
		} catch (Exception e) {
			e.printStackTrace();
			DatabasePumperOBDA.closeEverything();
		}
	}
	
	@Override
	protected <T> void establishColumnBounds(List<ColumnPumper<? extends Object>> listColumns) throws ValueUnsetException, DEBUGEXCEPTION, InstanceNullException, SQLException{
		for( ColumnPumper<? extends Object> cP : listColumns ){
			cP.fillFirstIntervalBoundaries(cP.getSchema(), dbOriginal);
		}
		// At this point, each column is initialized with statistical information
		// like null, dups ratio, num rows and freshs to insert, etc.
		
		
		// search for correlated columns and order them by fresh values to insert
		List<CorrelatedColumnsList<T>> correlatedCols = this.cCE.extractCorrelatedColumns();
		
		// Now, correlatedCols contains sets of correlated columns (closed under referencesTo and referredBy). :) :) :) :)
//		// I need to identify the intervals, now.
//		identifyIntervals(correlatedCols); 
		
		try {
			updateColumnBoundsWRTCorrelated(correlatedCols);
		} catch (BoundariesUnsetException | DEBUGEXCEPTION e) {
			e.printStackTrace();
			DatabasePumper.closeEverything();
			System.exit(1);
		}
	}
	
//	/**
//	 * Identify the intervals, and put into each of them the number of fresh values to insert
//	 * 
//	 * 
//	 * @param correlatedCols
//	 * @throws DEBUGEXCEPTION 
//	 * @throws InstanceNullException 
//	 */
//	@SuppressWarnings("rawtypes")
//    private void identifyIntervals(
//            List<CorrelatedColumnsList> correlatedCols) throws DEBUGEXCEPTION, InstanceNullException {
//	    
//	    class LocalUtils{
//	        private Interval obtainIntersectionInterval(Interval g, Interval toAdd) throws DEBUGEXCEPTION{
//	            Interval newInt = null; 
//	            switch(g.getType()){
//                    case BIGINT: case DOUBLE: 
//                        newInt = new BigDecimalInterval(g.getKey() + "-" + toAdd.getKey(), g.getType(), 0); 
//                        break;
//                    case CHAR:
//                        break;
//                    case DATETIME:
//                        newInt = new DatetimeInterval(g.getKey() + "-" + toAdd.getKey(), g.getType(), 0);
//                        break;
//                    case INT:
//                        newInt = new IntInterval(g.getKey() + "-" + toAdd.getKey(), g.getType(), 0); 
//                        break;
//                    case LINESTRING:
//                        break;
//                    case LONGTEXT:
//                        break;
//                    case MULTILINESTRING:
//                        break;
//                    case MULTIPOLYGON:
//                        break;
//                    case POINT:
//                        break;
//                    case POLYGON:
//                        break;
//                    case TEXT:
//                        break;
//                    case VARCHAR:
//                        newInt = new StringInterval(g.getKey() + "-" + toAdd.getKey(), g.getType(), 0);
//                        break;
//                    default:
//                        break;
//	                
//	            }
//	            if( newInt == null ){ 
//	                throw new DEBUGEXCEPTION();
//	            }
//	            return newInt;
//	        }
//
//	        @SuppressWarnings("unchecked")
//            void addNewIntervalToCPs(Interval toAdd) throws InstanceNullException {
//	            String[] splits = toAdd.getKey().split("-");
//	            for( String s : splits ){
//	                String[] splits1 = s.split(".");
//	                QualifiedName qF = new QualifiedName(splits1[0], splits1[1]);
//	                ColumnPumper cP = DBMSConnection.getInstance().getSchema(qF.getTableName()).getColumn(qF.getColName());
//	                cP.getIntervals().add(toAdd);
//	            }
//            }
//
//            public void checkIfEmpty(
//                    Interval toAdd, Interval curInt) throws InstanceNullException {
//                String[] splits = toAdd.getKey().split("-");
//                for( String s : splits ){
//                    String[] splits1 = s.split(".");
//                    QualifiedName qF = new QualifiedName(splits1[0], splits1[1]);
//                    ColumnPumper cP = DBMSConnection.getInstance().getSchema(qF.getTableName()).getColumn(qF.getColName());
//                    splits1 = curInt.getKey().split(".");
//                    QualifiedName curQF = new QualifiedName(splits1[0], splits1[1]);
//                    ColumnPumper curCP = DBMSConnection.getInstance().getSchema(curQF.getTableName()).getColumn(curQF.getColName());
//                    // TODO findNElementsInRatio
//                }
//            }
//	    }
//	    
//	    LocalUtils utils = new LocalUtils();
//	    
//	    for( CorrelatedColumnsList cCL : correlatedCols ){
//	        Queue<Interval> groups = new LinkedList<Interval>();
//	        // Add all intervals with a single column
//	        for( int i = 0; i < cCL.size(); ++i ){
//	            ColumnPumper cP = cCL.get(i);
//	            groups.add(cP.getIntervals().get(0));
//	        }
//	        
//	        while( groups.isEmpty() ){
//	            Interval g = groups.poll();
//
//	            // Add intervals with n columns
//	            for( int i = 0; i < cCL.size(); ++i ){
//	                ColumnPumper cP = cCL.get(i);           
//	                Interval curInt = cP.getIntervals().get(0);
//	                // If this combination has not been considered already
//	                if( !g.getKey().contains(curInt.getKey()) ){
//	                    Interval toAdd = utils.obtainIntersectionInterval(g, curInt);
//	                    utils.checkIfEmpty(toAdd, curInt);
//	                    utils.addNewIntervalToCPs(toAdd);
//	                    groups.add(toAdd);
//	                }
//	            }
//	        }
//	    }
//	}

    /**
	 * Update the boundaries of those columns in a correlated set
	 * @param correlatedCols (It MUST include foreign keys)
	 * @throws ValueUnsetException 
	 * @throws BoundariesUnsetException 
	 * @throws DEBUGEXCEPTION 
     * @throws InstanceNullException 
     * @throws SQLException 
	 */
	private <T> void updateColumnBoundsWRTCorrelated(
			List<CorrelatedColumnsList<T>> correlatedCols) 
			        throws ValueUnsetException, BoundariesUnsetException, DEBUGEXCEPTION, SQLException, InstanceNullException {
		
//	    LocalUtils utils  = new LocalUtils(this);
	    
	    for( CorrelatedColumnsList<T> cCL : correlatedCols){
	        IntervalsBoundariesFinder<T> utils = new IntervalsBoundariesFinder<T>(this);
	        List<Interval<T>> insertedIntervals = new LinkedList<Interval<T>>();
	        for( int i = 0; i < cCL.size(); ++i ){
	            ColumnPumper<T> cP = cCL.get(i);
	            utils.insert(insertedIntervals, cP);
	        }
	    }
	    
	    
	    
//		for( CorrelatedColumnsList cCL : correlatedCols ){
//			for( int i = 1; i < cCL.size(); ++i ){
//				setInContiguousInterval(i, cCL);
//				ColumnPumper referenced = cCL.get(i-1);
//				ColumnPumper current = cCL.get(i);
//
//				// Check if there is a fk constraint (all values are shared)
//				QualifiedName refName = new QualifiedName(referenced.getSchema().getTableName(), referenced.getName());
//				if( current.referencesTo().contains(refName) ){ 
//					long minEncoding = referenced.getMinEncoding();
//					// TODO Continua!!
//					continue;
//				}
////				Statistics.addInt("unskipped_correlated", 1);
//				int numSharedFreshs = findNumSharedFreshsToInsert(current, referenced);
//				long maxEncoding = referenced.getMaxEncoding();	
//				if( maxEncoding != Long.MAX_VALUE){
//					long newMinEncoding = maxEncoding - numSharedFreshs;
//					current.updateMinValueByEncoding(maxEncoding - numSharedFreshs);
//					current.updateMaxValueByEncoding(newMinEncoding + current.getNumFreshsToInsert());
//				}
//				else throw new DEBUGEXCEPTION();
//			}
//		}
		
	}

//	private void setInContiguousInterval(int curIndex,
//			CorrelatedColumnsList cCL) throws ValueUnsetException, BoundariesUnsetException {
//		
//		ColumnPumper current = cCL.get(curIndex);
//		int numFreshs = current.getNumFreshsToInsert();
//		
//		long min = current.getMinEncoding();
//		long max = current.getMaxEncoding();
//		long intervalLength = max - min;
//		
//		// TODO Use an SMT Solver
//	}

//	private int findNumSharedFreshsToInsert(ColumnPumper col, ColumnPumper referenced) {
//		int numSharedFreshs = 0;
//		try {
//			float sharedRatio = this.tStatsFinder.findSharedRatio(col, referenced);
//			numSharedFreshs = (int) (col.getNumFreshsToInsert() * sharedRatio);
//		} catch (SQLException | ValueUnsetException | InstanceNullException e) {
//			e.printStackTrace();
//			DatabasePumper.closeEverything();
//			System.exit(1);
//		}
//		return numSharedFreshs;
//	}
	
}

class IntervalsBoundariesFinder<T>{
    
    private DatabasePumperOBDA dbPumperInstance;
    
    public IntervalsBoundariesFinder(
            DatabasePumperOBDA databasePumperOBDA) {
        dbPumperInstance = databasePumperOBDA;
    }

    /**
     * Side effect on insertedIntervals and cP and related ColumnPumper objects
     * @throws DEBUGEXCEPTION 
     * @throws ValueUnsetException 
     * @throws InstanceNullException 
     * @throws SQLException 
     * @throws BoundariesUnsetException 
     */
    void insert(List<Interval<T>> insertedIntervals, ColumnPumper<T> cP) 
            throws DEBUGEXCEPTION, ValueUnsetException, SQLException, InstanceNullException, BoundariesUnsetException{
             
        long maxEncodingEncountered = 0;
        
        // Assert 
        if( cP.getIntervals().size() != 1 ){
            throw new DEBUGEXCEPTION("Intervals size != 1");
        }
        
        if( insertedIntervals.isEmpty() ){
            insertedIntervals.add(cP.getIntervals().get(0));
        }
        else{
            for( ListIterator<Interval<T>> it = insertedIntervals.listIterator() ; it.hasNext(); ){
                Interval<T> previouslyInserted = it.next();
                
                if( maxEncodingEncountered < previouslyInserted.getMaxEncoding() ) maxEncodingEncountered = previouslyInserted.getMaxEncoding();
                
                long nToInsertInPreviousInterval = makeIntersectionQuery(cP, previouslyInserted); // TODO Some kind of tabu to record useless intersection attempts?
                
                if( nToInsertInPreviousInterval > 0 ){ // Create a new "SubInterval"
                    
                    // Make sub interval ( with the right boundaries )
                    Interval<T> toInsert = makeSubInterval(previouslyInserted, cP, nToInsertInPreviousInterval);
                    
                    // Split
                    boolean killOldInterval = previouslyInserted.adaptBounds(toInsert);
                    
                    if( killOldInterval ){
                        it.remove();
                        previouslyInserted.suicide(); 
                    }
                    insertedIntervals.add(toInsert);
                }
            }
            
            long nFreshsInFirstInterval = cP.getNumFreshsToInsert() - cP.countFreshsInIntersectedIntervals();
            
            if( nFreshsInFirstInterval > 0 ){
                // Find fresh values for cP.getIntervals.get(0);
                cP.getIntervals().get(0).updateMinEncodingAndValue(maxEncodingEncountered + 1);
                cP.getIntervals().get(0).updateMaxEncodingAndValue( (maxEncodingEncountered + 1) + nFreshsInFirstInterval );
            }
        }
    }

    /** 
     *  It creates a new interval <b>result</b> starting from the boundaries of a given
     *  <b>previouslyInserted</b> interval. Then, it adds <b>result</b> to <b>cP.getIntervals()</b>.
     *  
     * @param previouslyInserted
     * @param cP
     * @param nToInsertInPreviouslyInserted
     * @return
     * @throws BoundariesUnsetException 
     */
    private Interval<T> makeSubInterval(
            Interval<T> previouslyInserted, ColumnPumper<T> cP, long nToInsertInPreviouslyInserted) 
                    throws BoundariesUnsetException {
        
        Interval<T> result = previouslyInserted.getCopyInstance();
        result.updateMaxEncodingAndValue(previouslyInserted.getMinEncoding() + nToInsertInPreviouslyInserted);
        result.addInvolvedColumnPumper(cP);
        
        return result;
    }

    private long makeIntersectionQuery(ColumnPumper<T> cP, Interval<T> previouslyInserted) 
            throws SQLException, InstanceNullException, ValueUnsetException {
        
        long result = 0;
        
        List<ColumnPumper<T>> cols = new ArrayList<ColumnPumper<T>>();
        
        cols.add(cP);
        cols.addAll(previouslyInserted.getInvolvedColumnPumpers());
        this.dbPumperInstance.tStatsFinder.findSharedRatio(cols);
        
        return result; 
    }
};


class CorrelatedColumnsList<T>{
	
	// This is always sorted w.r.t. the number of fresh values to insert
	private LinkedList<ColumnPumper<T>> columns;
	
	public CorrelatedColumnsList(){
		this.columns = new LinkedList<ColumnPumper<T>>();
	}
	
	public int size(){
		return columns.size();
	}
	
	public ColumnPumper<T> get(int index){
		return this.columns.get(index);
	}
	
	/**
	 * Insert cP in the list of correlated columns, while
	 * satisfying the ordering constraint on this.columns
	 * @param cP
	 */
	public void insert(ColumnPumper<T> cP){
		try{
			long nFreshs = cP.getNumFreshsToInsert();
			if( this.columns.size() == 0 ){
				this.columns.add(cP);
			}
			for( int i = 0; i < this.columns.size(); ++i ){
				ColumnPumper<T> el = this.columns.get(i);
				long elNFreshs = el.getNumFreshsToInsert();
				if( nFreshs > elNFreshs ){
					this.columns.add(i, cP);
					break;
				}
			}
		}catch(ValueUnsetException e){
			e.printStackTrace();
			DatabasePumper.closeEverything();
		}
	}
	
	public boolean isInCorrelated(ColumnPumper<T> cP){
		if( columns.contains(cP) ){
			return true;
		}
		return false;
	}
	
	@Override
	public String toString(){
		return columns.toString();
	}
	
};

class CorrelatedColumnsExtractor{
	
	private final JoinableColumnsFinder jCF;
	
	CorrelatedColumnsExtractor(JoinableColumnsFinder jCF) {
		this.jCF = jCF;
	}
	
	<T> List<CorrelatedColumnsList<T>> extractCorrelatedColumns() {
		List<CorrelatedColumnsList<T>> result = null;
		try {
			
			List<FunctionTemplate> templates = jCF.findFunctionTemplates();
			
			Set<Set<Field>> correlatedFields = extractCorrelatedFields(templates);
			
			Queue<Set<Field>> qCorrelatedFields = new LinkedList<Set<Field>>();
			for( Set<Field> fields : correlatedFields ){
				qCorrelatedFields.add(fields);
			}
			correlatedFields.clear();
			
			addForeignKeys(qCorrelatedFields);
			
			// merge 
			List<Set<Field>> maximalMerge = new ArrayList<Set<Field>>();
			maximalMerge(qCorrelatedFields, maximalMerge);
			
			result = constructCorrelatedColumnsList(maximalMerge);
			
		} catch (Exception e) {
			e.printStackTrace();
			DatabasePumperOBDA.closeEverything();
		}
		return result;
	}

	/**
	 * It adds to each set of correlated fields (Set<Field>) all correlated columns that derive
	 * 
	 * @param qCorrelatedFields
	 * @throws InstanceNullException
	 */
	private void addForeignKeys(Queue<Set<Field>> qCorrelatedFields) throws InstanceNullException {
	    
	    Queue<Set<Field>> result = new LinkedList<Set<Field>>();
	    
	    while( !qCorrelatedFields.isEmpty() ){
	        Set<Field> sF = qCorrelatedFields.poll();
	        
	        Queue<Field> queueOfFields = new LinkedList<Field>(sF);
            
	        while( queueOfFields.isEmpty() ){
	            Field f = queueOfFields.poll();
	            Queue<QualifiedName> correlated = new LinkedList<QualifiedName>();
                correlated.add(new QualifiedName(f.tableName, f.colName));
                
                List<QualifiedName> correlatedMax = new ArrayList<QualifiedName>();
                
                // Side effect on correlatedMax
                correlatedThroughForeignKeys(correlated, correlatedMax);
                
                // Insert all columns that are correlated because of a foreign key constraint
                for( QualifiedName qN : correlatedMax ){
                    Field toInsert = new Field(qN.getTableName(), qN.getColName());
                    sF.add(toInsert); // Try to insert if new
                }
	        }
	        result.add(sF);
	    }
	    qCorrelatedFields.addAll(result);
	}

    private void correlatedThroughForeignKeys(Queue<QualifiedName> correlated, List<QualifiedName> result) throws InstanceNullException {
        
        if( correlated.isEmpty() ) return;
        
        QualifiedName current = correlated.poll();
        if( result.contains(current) ){
            correlatedThroughForeignKeys(correlated, result);  // Recursion
        }
        else{
            ColumnPumper<? extends Object> cP = DBMSConnection.getInstance().getSchema(current.getTableName()).getColumn(current.getColName());
            correlated.addAll(cP.referencedBy());
            correlated.addAll(cP.referencesTo());
            result.add(current);
            correlatedThroughForeignKeys(correlated, result);  // Recursion
        }
    }

    private <T> List<CorrelatedColumnsList<T>> constructCorrelatedColumnsList(List<Set<Field>> maximalMerge) {
		
		List<CorrelatedColumnsList<T>> result = new ArrayList<CorrelatedColumnsList<T>>();
		
		for( Set<Field> correlatedColsList : maximalMerge ){
			CorrelatedColumnsList<T> cCL = new CorrelatedColumnsList<T>();
			for( Field f : correlatedColsList ){
				try {
					@SuppressWarnings("unchecked")
                    ColumnPumper<T> cP = (ColumnPumper<T>) DBMSConnection.getInstance().getSchema(f.tableName).getColumn(f.colName);
					cCL.insert(cP);
				} catch (InstanceNullException e) {
					e.printStackTrace();
					DatabasePumperOBDA.closeEverything();
				}
			}
			result.add(cCL);
		}
		return result;
	}

	private void maximalMerge(Queue<Set<Field>> current, List<Set<Field>> result) {
		
		if(!current.isEmpty()){
			Set<Field> first = current.remove();
			boolean changed = false;
			for( Iterator<Set<Field>> iterator = current.iterator(); iterator.hasNext(); ){
				Set<Field> elem = iterator.next();
				for( Field f : first ){
					if( elem.contains(f) ){
						first.addAll(elem);
						changed = true;
						iterator.remove();
						break;
					}
				}
			}
			if( changed ){ // It could merge with more
				current.add(first);
			}
			else{
				result.add(first);
			}
			maximalMerge(current, result);
		}
				
	}

	private Set<Set<Field>> extractCorrelatedFields(
			List<FunctionTemplate> templates) {
		
		Set<Set<Field>> result = new HashSet<Set<Field>>();
				
		for( FunctionTemplate fT : templates ){
			for( int i = 0; i < fT.getArity(); ++i ){
				Argument arg = fT.getArgumentOfIndex(i);
				
				Set<Field> fillingFields = arg.getFillingFields();
				
				if( fillingFields.size() > 1 ){
					result.add(fillingFields);
				}
			}
		}
		return result;
	}	
}