package heuristics;

public class Tuple {
	public int first;
	public int second;
	
	public Tuple(int first, int second) {
		this.first = first;
		this.second = second;
	}
	
	@Override
	public String toString() {
		return "(" + this.first + "," + this.second + ")";
	}
	
	public int getFirst() {
		return this.first;
	}
	
	public int getSecond() {
		return this.second;
	}
	
	public void setFirst(int first) {
		this.first = first;
	}
	
	public void setSecond(int second) {
		this.second = second;
	}
	
	@Override
    public boolean equals(Object o) {
 
        // If the object is compared with itself then return true 
        if (o == this) {
            return true;
        }
 
        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof Tuple)) {
            return false;
        }
         
        // typecast o to Complex so that we can compare data members
        Tuple c = (Tuple) o;
         
        // Compare the data members and return accordingly
        return Integer.compare(first, c.first) == 0
                && Integer.compare(second, c.second) == 0;
	}
	

}
