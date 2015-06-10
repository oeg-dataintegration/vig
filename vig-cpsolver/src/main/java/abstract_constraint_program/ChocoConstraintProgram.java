package abstract_constraint_program;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.IntConstraintFactory;
import org.chocosolver.solver.search.strategy.IntStrategyFactory;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VariableFactory;

public class ChocoConstraintProgram implements AbstractConstraintProgram<IntVar, Constraint>{

    private Solver solver;
    private List<IntVar> variables;
    private List<Constraint> constraints;
    
    private int postedSoFar;
    
    public ChocoConstraintProgram() {
	this.solver = new Solver(this.getClass().getName());
	this.variables = new ArrayList<>();
	this.constraints = new ArrayList<>();
	this.postedSoFar = 0;
    }

    @Override
    public ACPLongVar<IntVar> addLongVar(String varName, long min, long max) {
	
	assert( max > min );
	assert( max < Integer.MAX_VALUE ) : printChocoErr();
	
	IntVar x = VariableFactory.bounded(varName, (int)min, (int)max, solver);
	ACPLongVar<IntVar> wrapper = new ChocoACPLongVar(x);
	
	this.variables.add(x);
	
	return wrapper;
    }

    @Override
    public void addLongConstraint(ACPLongVar<IntVar> var1, ACPOperator operator,
	    ACPLongVar<IntVar> var2) {
	
	Constraint c = IntConstraintFactory.arithm(var1.getWrapped(), operator.getText(), var2.getWrapped());
	constraints.add(c);
	
    }

    @Override
    public void addScalarLongConstraint(List<Long> coeffs,
	    List<ACPLongVar<IntVar>> vars, Long value) {
	
	int[] coeffsArr = new int[coeffs.size()];
	for( int i = 0; i < coeffsArr.length; ++i ){
	    
	    assert( coeffs.get(i) < Integer.MAX_VALUE -1 ) : printChocoErr();
	    
	    coeffsArr[i] = coeffs.get(i).intValue();
	}
	
	List<IntVar> unwrapped = new ArrayList<IntVar>();
	
	for( ACPLongVar<IntVar> acpVar : vars ){
	    unwrapped.add(acpVar.getWrapped());
	}
	
	IntVar[] unwrappedArr = unwrapped.toArray(new IntVar[0]);
	
	if( ! (coeffsArr.length == unwrappedArr.length) ){
	    throw new ConstraintProgramException("Assertion failed: coefficients array size different from vars array size");
	}
	
	Constraint c = IntConstraintFactory.scalar(unwrappedArr, coeffsArr, VariableFactory.fixed(value.intValue(), solver));
    
	constraints.add(c);
    }

    @Override
    public void addLongConstraint(ACPLongVar<IntVar> var, ACPOperator operator, long value) {
	assert( value < Integer.MAX_VALUE -1 ) : printChocoErr();
	Constraint c = IntConstraintFactory.arithm(var.getWrapped(), operator.getText(), (int)value);
	constraints.add(c);
    }

    @Override
    public void post() {
	Constraint[] toPost = constraints.subList(postedSoFar, constraints.size()).toArray(new Constraint[0]);
	this.solver.post(toPost);
	postedSoFar = constraints.size();
    }
    
    // Assertions Handling
    private String printChocoErr() {
	return "Choco cannot hold values bigger than Integer";
    }

    @Override
    public boolean solve() {
//	this.solver.set(IntStrategyFactory.activity(this.variables.toArray(new IntVar[0]), 1));
//	this.solver.set(IntStrategyFactory.domOverWDeg(this.variables.toArray(new IntVar[0]), 1)); BAD
//	this.solver.set(IntStrategyFactory.impact(this.variables.toArray(new IntVar[0]), 1));
//	this.solver.set(IntStrategyFactory.lastConflict(this.solver));//(this.variables.toArray(new IntVar[0]), 1));
//	this.solver.set(IntStrategyFactory.lexico_Split(this.variables.toArray(new IntVar[0]))); Super BAD
	this.solver.set(IntStrategyFactory.custom(IntStrategyFactory.maxDomainSize_var_selector(), IntStrategyFactory.min_value_selector(), this.variables.toArray(new IntVar[0])));
	
	boolean result = this.solver.findSolution();
	return result;
    }
    
    @Override
    public String toString(){
	StringBuilder builder = new StringBuilder();
	builder.append("\nVariables:\n");
	builder.append(this.variables);
	builder.append("\nConstraints\n");
	builder.append(this.constraints);
	builder.append("\nPosted so far: "+this.postedSoFar);
	
	return builder.toString();
    }
    
    public String humanFormat(){
	String result = this.toString();
	int i = 0;
	for( Iterator<IntVar> it = this.variables.iterator(); it.hasNext(); ++i ){
	    String name = it.next().getName();
	    result = result.replaceAll(name, "X_"+i);
	}
	return result;
    }
}
