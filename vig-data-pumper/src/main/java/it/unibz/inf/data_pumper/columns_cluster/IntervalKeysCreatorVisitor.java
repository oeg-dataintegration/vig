package it.unibz.inf.data_pumper.columns_cluster;

import it.unibz.inf.data_pumper.columns.intervals.Interval;
import it.unibz.inf.data_pumper.utils.traversers.Node;
import it.unibz.inf.data_pumper.utils.traversers.visitors.Visitor;

import java.util.List;


/**
 * 
 * @author 
 * 
 * This class identifies intervals in multi-interval columns, and store relevant information
 * about these intervals in IntervalKey objects. Further, it creates <i>this.numAnonymousIntervals</i> 
 * new intervals T_1, ..., T_numAn. such that 
 * <ul>
 * <li> <i>T_1.minEncoding = this.maxEncodingInIntervals</i> </li>
 * <li> <i>T_n.maxEncoding = T_n+1.minEncoding -1</i> </li>
 * </ul>
 */
 class IntervalKeysCreatorVisitor implements Visitor{

    private List<SimpleIntervalKey> keys;
    private boolean anonymousIntervalsCreated;
    private long maxEncodingInIntervals;
    private int numAnonymousIntervals;

     IntervalKeysCreatorVisitor(int numAnonymousIntervals, long maxEncodingInIntervals, List<SimpleIntervalKey> result){
	this.keys = result;
	this.anonymousIntervalsCreated = false;
	this.maxEncodingInIntervals = maxEncodingInIntervals;
	this.numAnonymousIntervals = numAnonymousIntervals;
    }


    @Override
    public void visit(Node node) {
	ColumnPumperInCluster<?> cPIC = (ColumnPumperInCluster<?>) node;
	if( !anonymousIntervalsCreated ){
	    createAnonymousIntervals(cPIC);
	    anonymousIntervalsCreated = true;
	}

	if( !cPIC.isSingleInterval() ){
	    visitMulti(cPIC);
	}

    }

    private void visitMulti(ColumnPumperInCluster<?> cPIC){

	for( Interval<?> i : cPIC.cP.getIntervals() ){
	    if( !alreadyAdded(i.getKey()) ){ 
		this.keys.add(new SimpleIntervalKey(i));
	    }
	}
    }

    private boolean alreadyAdded(String key){
	boolean contained = false;
	for( SimpleIntervalKey current : this.keys ){
	    if( current.toString().equals(key) ){
		contained = true;
		break;
	    }
	}
	return contained;
    }

    private void createAnonymousIntervals(ColumnPumperInCluster<?> cPIC) {
	
	if( this.numAnonymousIntervals == 0 ) return;
	
	// Try the cast to int
	// Cast Check
	
	// Make 2*numAnonymousIntervals new variables
	assert(this.maxEncodingInIntervals < Integer.MAX_VALUE ) : "Values bigger than Integers are not allowed, cPIC: " + cPIC;
	    
	
	long offset = (this.maxEncodingInIntervals) / this.numAnonymousIntervals;

	for( int i = 0; i < this.numAnonymousIntervals; ++i ){
	    long lwBound = this.maxEncodingInIntervals + (offset * i);
	    long upBound = this.maxEncodingInIntervals + (offset * (i+1));

	    SimpleIntervalKey key = new SimpleIntervalKey(CPConstants.ANONYMOUS_ID+"_"+i, lwBound, upBound);
	    this.keys.add(key);
	}
    }
};
