package geometry;

import java.util.ArrayList;
import java.util.List;

public class MultiPolygon implements Comparable<MultiPolygon>{

	private List<Polygon> polygons;
	
	public MultiPolygon(String multiPolygonWKT){
		polygons = new ArrayList<Polygon>();
		parse(multiPolygonWKT);
	}
	
	/**
	 * Rectangle constructor
	 * @param minX
	 * @param minY
	 * @param maxX
	 * @param maxY
	 */
	
	public static MultiPolygon getInstanceFromRectangle(long minX, long minY, long maxX, long maxY){
		StringBuilder rectangleWKT = new StringBuilder();
		
		rectangleWKT.append("Multipolygon(((" + minX + " " + minY);
		rectangleWKT.append(",");
		rectangleWKT.append(maxX + " " + minY);
		rectangleWKT.append(",");
		rectangleWKT.append(maxX + " " + maxY);
		rectangleWKT.append(",");
		rectangleWKT.append(minX + " " + minY);
		rectangleWKT.append(")))");
		
		return new MultiPolygon(rectangleWKT.toString());
	}
	
	protected void parse(String multiPolygonWKT){
		int indexOuterFirstParenthesis = multiPolygonWKT.indexOf("(") + 1;
		int indexClosingParenthesis = multiPolygonWKT.lastIndexOf(")");
		
		String pieces = multiPolygonWKT.substring(indexOuterFirstParenthesis, indexClosingParenthesis);
		
		String[] splits = pieces.split("\\),\\(");
	
		for( String s : splits ){
			String toWorkWith = s;
			if( !s.startsWith("(") )
				toWorkWith = "(" + s;
			if( !s.endsWith(")") )
				toWorkWith = toWorkWith + ")";
			
			polygons.add(new Polygon("Polygon"+toWorkWith));
		}		
	}
	
	public List<Polygon> toPolygonsList(){
		return polygons;
	}
	
	@Override
	public int compareTo(MultiPolygon toCompare) {
		// Similar to the ordering for linestrings
		// This ordering relies on a MIN_X, MIN_Y, MAX_Y and MAX_Y
		// (1 1) < (1 2) < (1 1, 1 1) < (1 1, 1 9) < (1 2, 1 1) ecc.
		
		if( polygons.size() < toCompare.polygons.size() )
			return -1;
		if( polygons.size() == toCompare.polygons.size() ){
			
			for( int i = 0; i < polygons.size(); ++i ){
				if( polygons.get(i).compareTo(polygons.get(i) ) == -1 ){
					return -1;
				}
				else if( polygons.get(i).compareTo(polygons.get(i) ) == 1 ){
					return 1;
				}
			}
			return 0;
		}
		return 1;
	}
	
	private String toWKT(){
		
		StringBuilder builder = new StringBuilder();
		
		builder.append("Multipolygon(");
		
		for( Polygon p : polygons ){
			builder.append(p.toPointList());
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
