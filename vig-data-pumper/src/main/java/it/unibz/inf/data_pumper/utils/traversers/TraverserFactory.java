package it.unibz.inf.data_pumper.utils.traversers;

import java.util.List;

import it.unibz.inf.data_pumper.columns_cluster.EmptyVisitor;
import it.unibz.inf.data_pumper.utils.graphs.traversers.adjacence.BottomUpStrategy;
import it.unibz.inf.data_pumper.utils.graphs.traversers.adjacence.TopDownOrBottomUpStrategy;
import it.unibz.inf.data_pumper.utils.graphs.traversers.adjacence.TopDownStrategy;
import it.unibz.inf.data_pumper.utils.graphs.traversers.selection.BreadthFirstOrDepthFirstDataStructure;
import it.unibz.inf.data_pumper.utils.graphs.traversers.selection.BreadthFirstStructure;
import it.unibz.inf.data_pumper.utils.graphs.traversers.selection.DepthFirstStructure;
import it.unibz.inf.data_pumper.utils.traversers.visitors.BooleanVisitor;
import it.unibz.inf.data_pumper.utils.traversers.visitors.CollectVisitedVisitor;

public class TraverserFactory extends TraverserAbstractFactory {

    @Override
    public ListTraverser makeListTraverser(List<? extends Node> nodes) {
	ListTraverser t = new ListTraverser(nodes);
	return t;
    }

    @Override
    public GraphTraverser makeGraphTraverser(List<? extends Node> nodes,
	    TopDownOrBottomUpStrategy adjacenceSelectionStrategy,
	    BreadthFirstOrDepthFirstDataStructure<Node> toVisit) {
	GraphTraverser t = new GraphTraverser(nodes, adjacenceSelectionStrategy, toVisit);
	return t;
    }
    
    @Override
    public ReachConnectedTraverser makeReachConnectedTraverser(
	    List<? extends Node> list) {
	ReachConnectedTraverser result = new ReachConnectedTraverser(list);
	return result;
    }

    @Override
    public BottomUpStrategy makeBottomUpStrategy() {
	return new BottomUpStrategy();
    }

    @Override
    public TopDownStrategy makeTopDownStrategy() {
	return new TopDownStrategy();
    }

    @Override
    public BreadthFirstStructure<Node> makeBreadthFirstStructure() {
	return new BreadthFirstStructure<>();
    }

    @Override
    public DepthFirstStructure<Node> makeDepthFirstStructure() {
	return new DepthFirstStructure<>();
    }

    @Override
    public BooleanVisitor makeBooleanVisitor(ConditionChecker checker) {
	return new BooleanVisitor(checker);
    }

    @Override
    public CollectVisitedVisitor makeCollectVisitedVisitor() {
	return new CollectVisitedVisitor();
    }

    @Override
    public EmptyVisitor makeEmptyVisitor() {
	return EmptyVisitor.INSTANCE;
    }
};
