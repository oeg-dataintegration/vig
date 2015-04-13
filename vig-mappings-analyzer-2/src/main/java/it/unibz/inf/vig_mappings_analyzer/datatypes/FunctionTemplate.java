package it.unibz.inf.vig_mappings_analyzer.datatypes;

import it.unibz.inf.vig_mappings_analyzer.core.exceptions.WrongArityException;
import it.unibz.krdb.obda.model.Function;

import java.util.ArrayList;
import java.util.List;

public class FunctionTemplate{
	private String templateString = null;
	private List<Argument> arguments; 
	private int arity;
	private boolean isURI;
	
	public FunctionTemplate(Function f){
		templateString = cleanURIFromVariables(f.toString());
		arity = f.getArity() -1;
		arguments = new ArrayList<Argument>();
	}
	
	public String getTemplateString(){
		return this.templateString;
	}
	
	boolean isUriOrDatatype(){
		return templateString != null;
	}
	
	public void addArgument(Argument arg) throws WrongArityException{
		if( this.arity == arguments.size() ) throw new WrongArityException("Trying to add an argument beyond the term arity");
		this.arguments.add(arg);
	}
	
	public boolean hasArgumentOfIndex(int index){
		if( arguments.size() > index ) return true;
		return false;
	}
	
	public Argument getArgumentOfIndex(int index){
		return this.arguments.get(index);
	}
	
	public int getArity(){
		return this.arity;
	}
	
	@Override
	public boolean equals(Object other) {
		boolean result = true;
		if( other instanceof FunctionTemplate ){
			FunctionTemplate that = (FunctionTemplate) other;
			boolean sameTemplate = this.templateString.equals(that.templateString);
			boolean sameArity = this.arity == that.arity;
			if( sameTemplate && sameArity ){
				for( int i = 0; i < this.arguments.size(); ++i ){
					Argument thisArg = this.arguments.get(i);
					Argument thatArg = ((FunctionTemplate) other).arguments.get(i);
					if( !thisArg.hasSameFillingFields(thatArg) ){
						result = false;
						break;
					}
				}
			}
			else result = false;
		}
		else result = false;
		return result;
	}
	
	public boolean isURI(){
		return this.isURI;
	}
	
	@Override
	public int hashCode() {
		return templateString.hashCode();
	};
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\nTEMPLATE: " + this.templateString.toString() + "\n");
		builder.append("ARGS: " + this.arguments.toString());
		return builder.toString();
	}
	
	/**
	 * 
	 * @param uri
	 * @return It removes the variable names from the URI or the datatype, so that then it is 
	 *         sufficient to do string equality in order to understand whether the terms can join.
	 */
	private String cleanURIFromVariables(String uri){
		// URI("http://sws.ifi.uio.no/data/npd-v2/wellbore/{}/core/{}",wlbNpdidWellbore,wlbCoreNumber), 
		// http://www.w3.org/2001/XMLSchema#decimal(wlbCoreIntervalBottomFT)
		String result = null;
		
		if( uri.startsWith("URI(") ){
			this.isURI = true;
			int begin = uri.indexOf("\"") + 1;
			int end = uri.lastIndexOf("\"");
			result = uri.substring(begin, end);
		}
		else{
			this.isURI = false;
			int endIndex = uri.indexOf("(");
			String prefix = uri.substring(0, endIndex);
			result = prefix + "()";
		}
		return result;
	}
};