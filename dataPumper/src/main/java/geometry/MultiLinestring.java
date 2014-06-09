package geometry;

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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class MultiLinestring implements Comparable<MultiLinestring>{
	
	protected List<Linestring> linestrings;
	
	protected static Logger logger = Logger.getLogger(MultiLinestring.class.getCanonicalName());
	
	public MultiLinestring(String multiLinestringWKT){
		linestrings = new ArrayList<Linestring>();
		parse(multiLinestringWKT);
	}
	
	/**
	 * Rectangle constructor
	 * @param minX
	 * @param minY
	 * @param maxX
	 * @param maxY
	 */
	
	public static MultiLinestring getInstanceFromRectangle(BigDecimal minX, BigDecimal minY, BigDecimal maxX, BigDecimal maxY){
		StringBuilder rectangleWKT = new StringBuilder();
		
		rectangleWKT.append("Multilinestring((" + minX + " " + minY);
		rectangleWKT.append(",");
		rectangleWKT.append(maxX + " " + minY);
		rectangleWKT.append(",");
		rectangleWKT.append(maxX + " " + maxY);
		rectangleWKT.append(",");
		rectangleWKT.append(minX + " " + maxY);
		rectangleWKT.append(",");
		rectangleWKT.append(minX + " " + minY);
		rectangleWKT.append("))");
		
		return new MultiLinestring(rectangleWKT.toString());
	}
	
	protected void parse(String multiLinestringWKT){
		int indexOuterFirstParenthesis = multiLinestringWKT.indexOf("(") + 1;
		int indexClosingParenthesis = multiLinestringWKT.lastIndexOf(")");
		
		String pieces = multiLinestringWKT.substring(indexOuterFirstParenthesis, indexClosingParenthesis);
		
		String[] splits = pieces.split("\\),\\(");
	
		for( String s : splits ){
			String toWorkWith = s;
			if( !s.startsWith("(") )
				toWorkWith = "(" + s;
			if( !s.endsWith(")") )
				toWorkWith = toWorkWith + ")";
			
			linestrings.add(new Linestring("Linestring"+toWorkWith));
		}		
	}
	
	public List<Linestring> toLinestringList(){
		return linestrings;
	}
	
	@Override
	public int compareTo(MultiLinestring toCompare) {
		// Similar to the ordering for linestrings
		// This ordering relies on a MIN_X, MIN_Y, MAX_Y and MAX_Y
		// (1 1) < (1 2) < (1 1, 1 1) < (1 1, 1 9) < (1 2, 1 1) ecc.
		
		if( linestrings.size() < toCompare.linestrings.size() )
			return -1;
		if( linestrings.size() == toCompare.linestrings.size() ){
			
			for( int i = 0; i < linestrings.size(); ++i ){
				if( linestrings.get(i).compareTo(toCompare.linestrings.get(i)) == -1 ){
					return -1;
				}
				else if( linestrings.get(i).compareTo(toCompare.linestrings.get(i)) == 1 ){
					return 1;
				}
			}
			return 0;
		}
		return 1;
	}
	
	private String toWKT(){
		
		StringBuilder builder = new StringBuilder();
		
		builder.append("MultiLinestring(");
		
		for( Linestring l : linestrings ){
			builder.append(l.toPointList());
			builder.append(",");
		}
		builder.deleteCharAt(builder.lastIndexOf(","));
		builder.append(")");
		
		return builder.toString();
	}
	
	@Override
	public String toString(){
		return toWKT();
	}
}
