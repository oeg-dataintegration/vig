package it.unibz.inf.data_pumper.column_types.aggregate_types;

import it.unibz.inf.data_pumper.basic_datatypes.QualifiedName;
import it.unibz.inf.data_pumper.column_types.ColumnPumper;
import it.unibz.inf.data_pumper.connection.DBMSConnection;
import it.unibz.inf.data_pumper.utils.traversers.Node;

import java.util.LinkedList;
import java.util.List;

public class ColumnPumperInCluster<T> extends Node{
    public final ColumnPumper<T> cP;
    public final ColumnsCluster<T> cluster;
    private final boolean singleInterval;
    
    ColumnPumperInCluster(ColumnPumper<T> cP, ColumnsCluster<T> cluster){
	this.cP = cP;
	this.cluster = cluster;
	singleInterval = determineIfSingleInterval();
    }
        
    public boolean isSingleInterval(){
	return singleInterval;
    }
    
    private boolean determineIfSingleInterval(){
	boolean result = false;

	if( cP.getIntervals().size() == 1 ){
	    if( cP.getIntervals().get(0).getInvolvedColumnPumpers().size() == 1 ){
		if( cP.getIntervals().get(0).getInvolvedColumnPumpers().iterator().next().equals(cP) ){
		    result = true;
		}
	    }
	}
	return result;
    }
    
    private List<ColumnPumperInCluster<T>> refersToCPs() {
	List<ColumnPumperInCluster<T>> result = new LinkedList<>();
	for( QualifiedName qN : this.cP.referencesTo() ){
	    @SuppressWarnings("unchecked")
	    ColumnPumper<T> cP = (ColumnPumper<T>) DBMSConnection.getInstance().getSchema(qN.getTableName()).getColumn(qN.getColName());	    
	    if( cluster.getColumnPumpersInCluster().contains(cP) ){
		for( ColumnPumperInCluster<T> inCluster : cluster.getClusterCols() ){
		    if( inCluster.cP.equals(cP) ){
			result.add(inCluster);
			break;
		    }
		}
	    }
	    else{
		result.add(new ColumnPumperInCluster<T>(cP, cluster));
	    }
	}
	return result;
    }
    
    private List<ColumnPumperInCluster<T>> referredByCPs() {
	List<ColumnPumperInCluster<T>> result = new LinkedList<>();
	for( QualifiedName qN : this.cP.referencedBy() ){
	    @SuppressWarnings("unchecked")
	    ColumnPumper<T> cP = (ColumnPumper<T>) DBMSConnection.getInstance().getSchema(qN.getTableName()).getColumn(qN.getColName());
	    if( cluster.getColumnPumpersInCluster().contains(cP) ){
		for( ColumnPumperInCluster<T> inCluster : cluster.getClusterCols() ){
		    if( inCluster.cP.equals(cP) ){
			result.add(inCluster);
			break;
		    }
		}
	    }
	    else{
		result.add(new ColumnPumperInCluster<T>(cP, cluster));
	    }
	}
	return result;
    }
    
    @Override
    public String toString(){
	return cP.toString();
    }

    @Override
    public List<? extends Node> getOutNodes() {
	List<? extends Node> result = refersToCPs(); 
	return result;
    }

    @Override
    public List<? extends Node> getInNodes() {
	List<? extends Node> result = referredByCPs();
	return result;
    }
};