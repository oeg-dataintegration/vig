package it.unibz.inf.data_pumper.columns.intervals;

import it.unibz.inf.data_pumper.columns.ColumnPumper;
import it.unibz.inf.data_pumper.tables.MySqlDatatypes;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;

public class StringIntervalFixedDomain extends StringInterval {

    private List<String> fixedDomainValues;
    
    public StringIntervalFixedDomain(String key, MySqlDatatypes type,
	    long nValues, int datatypeLength,
	    List<ColumnPumper<String>> involvedCols) {
	super(key, type, nValues, datatypeLength, involvedCols);
	
	fixedDomainValues = new ArrayList<String>();
    }
    
    @Override
    public long getMaxEncoding() {
        return this.maxEncoding;
    }
    
    @Override
    public String encode(long value){
	return fixedDomainValues.get((int) value);
    }

    @Override
    public Interval<String> getCopyInstance() {
        
        StringInterval result =
                new StringIntervalFixedDomain(
                        this.getKey(), this.getType(), 
                        this.nFreshsToInsert, this.datatypeLength, 
                        new LinkedList<>(this.intervalColumns));
        result.updateMinEncodingAndValue(this.minEncoding);
        result.updateMaxEncodingAndValue(this.maxEncoding);
        
        return result;
    }
    
}