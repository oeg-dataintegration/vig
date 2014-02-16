package utils;

public class Pair<T,S> {
	public T first;
	public S second;
	
	public Pair(T first, S second){
		this.first = first;
		this.second = second;
	}
	
	public String toString(){
		return "["+first.toString()+", "+second.toString()+"]";
	}
};
