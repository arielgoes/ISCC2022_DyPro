package heuristics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Cycle implements Cloneable{
	public ArrayList<Tuple> itemPerCycle;
	public ArrayList<Integer> nodes; //nodes in the cycle;
	public ArrayList<Pair<Integer,Integer>> links; //links in the cycle... sometimes this representations is easier
	public int capacity;
	public int capacity_used;
	public int transportationCost;
	public ArrayList<Integer> pathOverhead;   //for comparison between OptPathPlan and others
	public int cycle_id;

public Cycle() {
	this.itemPerCycle = new ArrayList<Tuple>();
	this.nodes = new ArrayList<Integer>();
	this.links = new ArrayList<Pair<Integer,Integer>>();
}

public void printCycle() {
	System.out.println("CIRCUIT:");
	System.out.println("path (nodes): " + this.nodes);
	System.out.println("path (links): " + this.links);
	System.out.println("DEVICE-ITEM:");
	System.out.println("device-item: " + this.itemPerCycle);
	System.out.println("cycle id (probe id): " + this.cycle_id);
	
}

public void printCycleWithCapacity() {
	System.out.println("---------------------------------");
	System.out.println("CIRCUIT:");
	System.out.println("path (node): " + this.nodes);
	System.out.println("path (links): " + this.links);
	System.out.println("DEVICE-ITEMS:");
	System.out.println("device-items: " + this.itemPerCycle);
	System.out.println("Capacity used: " + this.capacity_used + ". Total Capacity: " + this.capacity);
	System.out.println("cycle id (probe id): " + this.cycle_id);
}

@Override
public Cycle clone() throws CloneNotSupportedException {
   try{
       Cycle clonedMyClass = (Cycle)super.clone();
       // if you have custom object, then you need create a new one in here
	       return clonedMyClass ;
	   } catch (CloneNotSupportedException e) {
	       e.printStackTrace();
	       return new Cycle();
	   }
}
	
public boolean hasItem(int item) {
	for(int i = 0; i < this.itemPerCycle.size(); i++) {
		if(this.itemPerCycle.get(i).getSecond() == item) {
			//System.out.println("asd: " + this.itemPerCycle.get(i).getItem() + " asd2: " + restrictionItem);
			return true;
		}
	}
	return false;
}

public int getCapacityUsed() {
	return this.capacity_used;
}

public boolean hasDevice(int dev) { //true if it has the input node/device
	if(this.nodes.contains(dev)) {
		return true;
	}
	return false;
}


public static Comparator<Cycle> CycleCapacityUsedDescending = new Comparator<Cycle>() {

	public int compare(Cycle c1, Cycle c2) {

	   int cycleusage1 = c1.getCapacityUsed();
	   int cycleusage2 = c2.getCapacityUsed();

	   /*For ascending order*/
	   //return cycleusage1-cycleusage2;

	   /*For descending order*/
	   return cycleusage2-cycleusage1;
   }
};

}
