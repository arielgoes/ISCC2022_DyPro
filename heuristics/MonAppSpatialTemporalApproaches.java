package heuristics;

import java.util.ArrayList;
import main.MonitoringApp;
import main.NetworkInfrastructure;
import heuristics.Pair;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class MonAppSpatialTemporalApproaches{
	public ArrayList<Cycle> cycles;
	public ArrayList<MonitoringApp> clonedMonApps;
	public int[][] costShortestPath;
	public NetworkInfrastructure infra;
	long seed;
	public int capacityProbe;
	public boolean flagCreatePath = false; //either I let it like this, or return a vector [edge.first, edge.second, flagCreatePath]
	public boolean flagEqualDepot = false;
	
	public MonAppSpatialTemporalApproaches(NetworkInfrastructure infra, long seed, int capacityProbe) {
		this.cycles = new ArrayList<Cycle>();
		this.infra = infra;
		this.costShortestPath = new int[infra.size][infra.size];
		this.seed = seed;
		this.clonedMonApps = new ArrayList<MonitoringApp>();
		this.capacityProbe = capacityProbe;
		
		//start shortest path all for all
		//int count = 0;
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {
				ArrayList<Integer> shortPath = new ArrayList<Integer>();
				shortPath = this.infra.getShortestPath(i, j);
				////System.out.println("nodeA " + i + " nodeB: " + j + " shortPath: " + shortPath);
				if(shortPath.size() > 0) {
					
					if(i == j) {
						this.costShortestPath[i][j] = Integer.MAX_VALUE;
					}else if(i > j) {
						continue;
					}else{
						this.costShortestPath[i][j] = shortPath.size() - 1;
						this.costShortestPath[j][i] = shortPath.size() - 1;
					}
				}else {
					//count++;
					this.costShortestPath[i][j] = Integer.MAX_VALUE;
				}
			}
		}
		
		//print cost distance
		/*System.out.println("COST-DISTANCE");
		for(int i = 0; i < this.infra.size; i++) {
			for(int j = 0; j < this.infra.size; j++) {
				System.out.printf("[%d][%d]=%d ", i, j, this.costShortestPath[i][j]);
			}
			System.out.println("");
		}*/
	}

	
/*public void cloneMonitoringApps(ArrayList<MonitoringApp> monitoringApps) throws CloneNotSupportedException {
	for(int i = 0; i < monitoringApps.size(); i++) {
		MonitoringApp newMonApp = monitoringApps.get(i).clone();
		this.clonedMonApps.add(newMonApp);
	}
	
}*/

//this approach prioritizes to select the next node to be a "device to be collected". If none, it randomly chooses a next neighbour node to create a cycle
public ArrayList<Cycle> firstApproach(ArrayList<MonitoringApp> monitoringApps, int capacityProbe) throws CloneNotSupportedException {
	ArrayList<Cycle> cycles_sol = new ArrayList<Cycle>();
	System.out.println("before (firstApproach): " + this.clonedMonApps.size());
	this.clonedMonApps = monitoringApps;
	System.out.println("after  (firstApproach): " + monitoringApps.size());
	
	
	/*System.out.println("clonedMon: " + clonedMonApps);
	for(int i = 0; i < clonedMonApps.size(); i++) {
		System.out.println("mon spatial: " + clonedMonApps.get(i).spatialRequirements);
		System.out.println("mon dev: " + clonedMonApps.get(i).deviceToBeCollected);
	}
	//System.out.println("monApps: " + monApps);
	System.exit(0);*/
	
	boolean[][] collectedItems = new boolean[this.infra.size][this.infra.telemetryItemsRouter]; //device, item
	
	int item = -1;
	int fixedEdgeCost = 1;
	int currProbeCapacity = capacityProbe;
	int oldProbeCapacity = capacityProbe;
	int probeId = 0;
	int total_items = 0;
	boolean flagReturnDepot = false;
	
	// # of remaining items to be collected
	int remainingItems = 0;
	for(int i = 0; i < clonedMonApps.size(); i++) {
		for(int j = 0; j < clonedMonApps.get(i).spatialRequirements.size(); j++) {
			for(int k = 0; k < clonedMonApps.get(i).spatialRequirements.get(j).size(); k++) {
				remainingItems++;
			}
		}
	}
	
	total_items = remainingItems;
	
	do {
		System.out.println("---------------------------------NEW CYCLE-----------------------------------------------------------------");
		int currNode = -2;
		int lastNode = -2;
		int depot = -2;
		
		boolean[][] visited = new boolean[this.infra.size][this.infra.size];
		Pair<Integer,Integer> link = Pair.create(currNode, -1);
		
		//choose depot
		if(clonedMonApps.size() > 0) {
			depot = clonedMonApps.get(0).deviceToBeCollected.get(0);
			System.out.println("DEPOT... " + depot);
		}else {
			break;
		}
		
		Cycle c = new Cycle();
		c.capacity = capacityProbe;
		
		currNode = depot; // starting point]
		
		do {
		
			//for(int i = 0; i < clonedMonApps.size(); i++) {
			while(clonedMonApps.size() > 0) {
				int i = 0;
				
				//this loop guarantees the current tuples contain at least once the current node to be satisfied
				boolean flagFoundIt = false;
				for(i = 0; i < clonedMonApps.size(); i++) {
					//System.out.println("i (in): " + i + ", currNode: " + currNode);
					if(clonedMonApps.get(i).deviceToBeCollected.contains(currNode)) {
						flagFoundIt = true;
						break;
					}
				}
				//System.out.println("mon app: " + i + ", cloned size all: " + clonedMonApps.size());
				
				
				
				
				if(flagFoundIt) {
					//current tuples
					//System.out.println("curr tuples........................: " + clonedMonApps.get(i).spatialRequirements);
					//System.out.println("curr devices.......................: " + clonedMonApps.get(i).deviceToBeCollected);
					while(clonedMonApps.get(i).spatialRequirements.size() > 0) { 
						
						int y = 0;
						y = clonedMonApps.get(i).deviceToBeCollected.indexOf(currNode);
						if(y < 0) {
							break;
						}
						//System.out.println("who is 'y'...: " + y);
						
						int numItemsCurrSpatialDep = clonedMonApps.get(i).spatialRequirements.get(y).size();
						ArrayList<Integer> auxRemoveCurrTuple = new ArrayList<Integer>();
						
						
						//current tuple 
						for(int l = 0; l < clonedMonApps.get(i).spatialRequirements.get(y).size(); l++) {
							item = clonedMonApps.get(i).spatialRequirements.get(y).get(l);
							//System.out.println("item (out): " + item);
							
							if(collectedItems[currNode][item]) {
								//items to be removed
								auxRemoveCurrTuple.add(item);
								numItemsCurrSpatialDep -= 1;
								remainingItems -= 1;
							}
							
							//System.out.println("collected["+currNode+"]["+item+"]: " + collectedItems[currNode][item]);
							
							//add items to probe - if it has enough available space
							//System.out.println("currNode: " + currNode);
							if (currProbeCapacity - infra.sizeTelemetryItems[item] > 0 && !collectedItems[currNode][item]) {
								//System.out.println("item (in): " + item);
								currProbeCapacity -= infra.sizeTelemetryItems[item];
								collectedItems[currNode][item] = true;
								//System.out.println("(in)    collected["+currNode+"]["+item+"]: " + collectedItems[currNode][item]);
								numItemsCurrSpatialDep -= 1; // local counter (current tuple)
								//System.out.println("numItemsCurrSpatialDep: " + numItemsCurrSpatialDep);
								remainingItems -= 1; // global counter
								
								//cycle managing
								Tuple devItem = new Tuple(currNode, item);
								c.itemPerCycle.add(devItem);
								//System.out.println("devItem: " + devItem);
								c.capacity_used += infra.sizeTelemetryItems[item];
								
								//items to be removed
								auxRemoveCurrTuple.add(item);
								//System.out.println("auxRemoveCurrTuple (if): " + auxRemoveCurrTuple);
							}
						}
						
						//System.out.println("items to be removed array: " + auxRemoveCurrTuple);
						//System.out.println("remainingItems: " + remainingItems);
						
						//remove collected items
						
						
						for(int x = 0; x < auxRemoveCurrTuple.size(); x++) {
							if(clonedMonApps.get(i).spatialRequirements.get(y).contains(auxRemoveCurrTuple.get(x))) {
								//System.out.println("items to be removed array (if): " + auxRemoveCurrTuple.get(x));
								clonedMonApps.get(i).spatialRequirements.get(y).remove(auxRemoveCurrTuple.get(x));
							}
						}
						
						
						
						//System.out.println("numItemsCurrSpatialDep: " + numItemsCurrSpatialDep);
						//System.out.println("cloned spatial req: (before) " + clonedMonApps.get(i).spatialRequirements);
						//System.out.println("cloned dev req      (before): " + clonedMonApps.get(i).deviceToBeCollected);
						if(numItemsCurrSpatialDep == 0) { // e.g., before: Mon 0: < <2,3>, <4,5> > || after: Mon 0: < <4,5> >
							clonedMonApps.get(i).spatialRequirements.remove(y);
							clonedMonApps.get(i).deviceToBeCollected.remove(y);						
						}
						//System.out.println("cloned spatial req (after): " + clonedMonApps.get(i).spatialRequirements);
						//System.out.println("cloned dev req     (after): " + clonedMonApps.get(i).deviceToBeCollected);
	
						
						//Choose the next node - i.e., create a cycle link
						if(clonedMonApps.get(i).deviceToBeCollected.contains(currNode)) {
							continue;
						}else {
							link = generateLink(depot, currNode, lastNode, currProbeCapacity, fixedEdgeCost, visited, clonedMonApps.get(i).deviceToBeCollected);	
						}
						
						if(link.second == -99) { //no more edges to visit. So, return to the depot
							flagReturnDepot = true;
							//System.out.println("link.second == -99");
						}else {
							lastNode = link.first;
							currNode = link.second;
							visited[link.first][link.second] = true;
							c.links.add(link);
							//c.nodes.add(currNode);
						}
						
						//System.out.println("num items curr spatial dep (after): " + numItemsCurrSpatialDep);
						
					}
				
					//System.out.println("cloned spatial req (outer): " + clonedMonApps.get(i).spatialRequirements);
					if(clonedMonApps.get(i).spatialRequirements.size() == 0) {
						//flagReturnDepot = true;
						//System.out.println("currNode (if below): " + currNode);
						clonedMonApps.remove(i);
						continue;
					}
					
					
					if(flagReturnDepot) {
						//System.out.println("here");
						break;
					}
				}
				else {
					ArrayList<Integer> temp = new ArrayList<Integer>();
					link = generateLink(depot, currNode, lastNode, currProbeCapacity, fixedEdgeCost, visited, temp);
					if(link.second == -99) { //no more edges to visit. So, return to the depot
						flagReturnDepot = true;
						//System.out.println("link.second == -99");
					}else {
						lastNode = link.first;
						currNode = link.second;
						visited[link.first][link.second] = true;
						c.links.add(link);
						//c.nodes.add(currNode);
					}
					
					if(flagReturnDepot) {
						//System.out.println("here");
						break;
					}
				}
				
			}
		
		 //System.out.println("clonedMonApps: " + clonedMonApps);
		}while(clonedMonApps.size() > 0 && currProbeCapacity > capacityProbe/2 && !flagReturnDepot); //upforward steps
		//do-while - backwards steps - i.e., connecting the cycle towards the 'depot'
		
		//reset flag
		flagReturnDepot = false;
		
		//link the last node to the depot
		Pair<Integer,Integer> lastLinkCycle = Pair.create(-1, -1);
		lastLinkCycle = c.links.get(c.links.size()-1);
		
		if(lastLinkCycle.second != depot) {
			//reconstruct the path to depot using the "costShortestPath" structure
			ArrayList<Integer> shortPath = new ArrayList<Integer>();
			
			//System.out.println("edge.first: " + edge.first + ", edge.second: " + edge.second);
			//System.out.println("remainingEdges: " + remainingEdges);
			
			//System.out.println("----------------------------------------RETURNING----------------------------------------");
			
			//if both nodes are feasible
			if(link.first >= 0 && link.second >= 0) {
				shortPath = infra.getShortestPath(link.second, depot);
			}else { //if only the first is feasible
				shortPath = infra.getShortestPath(link.first, depot);
			}

			//System.out.println("shortPath: " + shortPath);
			for(int k = 0; k < shortPath.size() - 1; k++) {
				int node1 = shortPath.get(k);
				int node2 = shortPath.get(k+1);
				Pair<Integer,Integer> p = Pair.create(node1, node2);
				c.links.add(p); //add to current circuit
				
				//if the edge contains the 'depot' it's already 
				if(node1 != depot && node2 != depot) {
					currProbeCapacity -= fixedEdgeCost;	
				}
				
			}
		}
		
		
		//add cycle to the list of cycles
		c.cycle_id = probeId;
		cycles_sol.add(c);
		probeId++;
		cycles_sol = addNodesToCycle(cycles_sol);
		//c.printCycleWithCapacity();
		currProbeCapacity = oldProbeCapacity; //reset probe capacity
	
	}while(this.clonedMonApps.size() > 0 || remainingItems > 0);
	
	System.out.println("cloned size: " + clonedMonApps.size());
	System.out.println("original size: " + monitoringApps.size());
	cycles_sol = simpleOptimizer(cycles_sol); //OPTIMIZER (REDUCE # OF CYCLES)

	
	return cycles_sol;
}


//reduces the number of cycles in the current solution
public ArrayList<Cycle> simpleOptimizer(ArrayList<Cycle> cycles_sol) {
	System.out.println("-----------------------SIMPLE OPTIMIZER-----------------------");
	int indexHighest = 0;
	int highestCapUsed = Integer.MIN_VALUE; 
	ArrayList<Cycle> cyclesToBeRemoved = new ArrayList<Cycle>();
	
	//get the index of the cycle collecting most of items
	for(int i = 0; i < cycles_sol.size(); i++) {
		if(cycles_sol.get(i).capacity_used > highestCapUsed) {
			highestCapUsed = cycles_sol.get(i).capacity_used;
			indexHighest = i;
		}
	}
	
	for(int i = 0; i < cycles_sol.size(); i++) {
		if(i != indexHighest) {
			ArrayList<Tuple> itemsToBeRemoved = new ArrayList<Tuple>();
			for(int j = 0; j < cycles_sol.get(i).itemPerCycle.size(); j++) { //try to add items to the most utilized cycle
				Tuple devItem = cycles_sol.get(i).itemPerCycle.get(j); 
				
				//check the existing links and available space (resilient capacity) and remove items from the least utilized cycle
				if(cycles_sol.get(indexHighest).nodes.contains(devItem.getDevice())) {
					int resilientCap = cycles_sol.get(indexHighest).capacity - cycles_sol.get(indexHighest).capacity_used;
					//System.out.println("resilientCap: " + resilientCap);
					if(resilientCap - infra.sizeTelemetryItems[devItem.getItem()] >= 0) {
						//most utilized cycle
						cycles_sol.get(indexHighest).itemPerCycle.add(devItem); //add item
						cycles_sol.get(indexHighest).capacity_used += infra.sizeTelemetryItems[devItem.getItem()]; //update capacity
						
						//least utilized cycle
						itemsToBeRemoved.add(devItem); //remove item (later)
						cycles_sol.get(i).capacity_used -= infra.sizeTelemetryItems[devItem.getItem()]; //update capacity
					}
				}
			}
			for(int x = 0; x < itemsToBeRemoved.size(); x++) {
				if(cycles_sol.get(i).itemPerCycle.contains(itemsToBeRemoved.get(x))) {
					cycles_sol.get(i).itemPerCycle.remove(itemsToBeRemoved.get(x));
				}
			}
		}
		if(cycles_sol.get(i).itemPerCycle.size() == 0) {
			cyclesToBeRemoved.add(cycles_sol.get(i));
		}
	}
	
	//remove empty cycles (i.e., collecting no items)
	for(int x = 0; x < cyclesToBeRemoved.size(); x++) {
		if(cycles_sol.contains(cyclesToBeRemoved.get(x))) {
			cycles_sol.remove(cyclesToBeRemoved.get(x));
		}
	}
	
	//maybe it should be necessary to update probe id before returning it... idk yet...
	return cycles_sol;
}


public Pair<Integer,Integer> generateLink(int depot, int node1, int lastNode, int currProbeCapacity, int fixedEdgeCost,
		boolean[][] visited, ArrayList<Integer> deviceToBeCollected) {
	
	Pair<Integer,Integer> edge = Pair.create(node1, -99);
	Random rnd = new Random(this.seed);
	ArrayList<Integer> neighboursList = new ArrayList<Integer>();
	
	//search for neighbours... avoid loop on random node selection
	for(int j = 0; j < this.infra.size; j++) {
		if(node1 != j && this.infra.graph[node1][j] == 1 &&
			!visited[node1][j] && j != lastNode && j != depot) {
			
			neighboursList.add(j);
		}	
	}
	
	//check if any neighbour is also a "device to be collected"
	for(int i = 0; i < deviceToBeCollected.size(); i++) {
		if(neighboursList.contains(deviceToBeCollected.get(i))) { // if the neighbour is also a requirement device, use it to create the next link
			edge.second = deviceToBeCollected.get(i);
			//System.out.println("edge1: " + edge);
			break;
		}
	}
	
	//System.out.println("neighbours: " + neighboursList);
	
	//if there is at least one feasible neighbour
	if(!neighboursList.isEmpty() && edge.second != -99) {
		visited[node1][edge.second] = true;
		currProbeCapacity -= fixedEdgeCost;
		//System.out.println("edge2: " + edge);
		return edge;
	}else if(!neighboursList.isEmpty()) {
		edge.second = neighboursList.get(rnd.nextInt(neighboursList.size()));
		visited[node1][edge.second] = true;
		//System.out.println("edge3: " + edge);
		return edge;
	}
	
	// if pair is (node, -99) at the end, it means it completed a circuit and there is no remaining edge to the current node,...
	// ... so, one have to choose how to treat this issue.
	//System.out.println("edge4: " + edge);
	return edge;
}


//this method takes a new set of random monitoring apps and try to reconstruct probe cycles from previous mon app attributions (e.g., using 'firstApproach')
public void dynamicMonAppProbeGenerator(ArrayList<MonitoringApp> newMonApps) throws CloneNotSupportedException {
	
	this.clonedMonApps = newMonApps;
	
	ArrayList<int[]> satisfiedTuples = new ArrayList<int[]>();
	boolean[][] collected = new boolean[this.infra.size][this.infra.telemetryItemsRouter];
	
	//Check what spatial requirements are already satisfied by the previous probe scheme
	for(int i = this.cycles.size() - 1; i >= 0; --i){
		for(int j = this.cycles.get(i).itemPerCycle.size() - 1; j >= 0; --j) {
			Tuple devItem = this.cycles.get(i).itemPerCycle.get(j);
			//System.out.println("devItem: " + devItem);
			
			boolean monAppContainsDevItem = false; 
			//iterate over all mon apps and delete all spatial requirements containing the exactly same tuple (dev, item)
			for(int a = 0; a < this.clonedMonApps.size(); a++) {
				for(int b = 0; b < this.clonedMonApps.get(a).spatialRequirements.size(); b++) {
					for(int c = 0; c < this.clonedMonApps.get(a).spatialRequirements.get(b).size(); c++) {							
						//System.out.println("a: " + a + ", b: " + b + ", c: " + c);
						if(this.clonedMonApps.get(a).deviceToBeCollected.get(b) == devItem.getDevice() &&
								this.clonedMonApps.get(a).spatialRequirements.get(b).get(c) == devItem.getItem()) {
							//System.out.println("devItem: " + devItem);
							//System.out.println("a: " + a + ", b: " + b + ", c: " + c);
							int[] aux = new int[3]; // int[0]: mon app id, int[1]: spatial req id, int[2]: item position
							aux[0] = a;
							aux[1] = b;
							aux[2] = c;
							satisfiedTuples.add(aux);
							collected[devItem.getDevice()][devItem.getItem()] = true; //mark as already collected (i.e., by previous probe)
						}else if(this.clonedMonApps.get(a).deviceToBeCollected.contains(devItem.getDevice()) &&
								 	this.clonedMonApps.get(a).spatialRequirements.get(b).contains(devItem.getItem())){ 
							monAppContainsDevItem = true;
							//this.cycles.get(i).itemPerCycle.remove(devItem);
						}
					}
				}
			}
			
			//delete non-utilized spatial requirements collected from the previous solution and decrease 'capacity_used'
			if(!monAppContainsDevItem) {
				this.cycles.get(i).capacity_used -= infra.sizeTelemetryItems[devItem.getItem()];
				this.cycles.get(i).itemPerCycle.remove(devItem);
			}
		}
	}
	
	//set collected items to '-1'
	for(int i = 0; i < satisfiedTuples.size(); i++) {
		int monAppId = satisfiedTuples.get(i)[0];
		int spatialReqId = satisfiedTuples.get(i)[1];
		int itemId = satisfiedTuples.get(i)[2];
		//System.out.println("monAppId: " + monAppId + ", spatialReqId: " + spatialReqId + ", itemId: " + itemId);
		
		//mark items as '-1' to be removed later (i.e., in order to not lose the stored indexes - 'aux' array)
		for(int a = 0; a < this.clonedMonApps.size(); a++) {
			for(int b = 0; b < this.clonedMonApps.get(a).spatialRequirements.size(); b++) {
				for(int c = 0; c < this.clonedMonApps.get(a).spatialRequirements.get(b).size(); c++) {
					if(a == monAppId && b == spatialReqId && c == itemId) {
						this.clonedMonApps.get(a).spatialRequirements.get(b).set(c, -1);
					}
				}
				boolean allCollected = true;
				for(int x = 0; x < this.clonedMonApps.get(a).spatialRequirements.get(b).size(); x++) {
					if(this.clonedMonApps.get(a).spatialRequirements.get(b).get(x) != -1) {
						allCollected = false;
					}
				}
				if(a == monAppId && b == spatialReqId && allCollected) {
					this.clonedMonApps.get(a).deviceToBeCollected.set(b, -1);
				}
			}
		}
	}
	

	//remove marked items (i.e., '-1' items)
	for(int i = 0; i < satisfiedTuples.size(); i++) {
		int monAppId = satisfiedTuples.get(i)[0];
		int spatialReqId = satisfiedTuples.get(i)[1];
		//int itemId = satisfiedTuples.get(i)[2];
		
		//remove items
		for(int a = 0; a < this.clonedMonApps.size(); a++) {
			for(int b = 0; b < this.clonedMonApps.get(a).spatialRequirements.size(); b++) {
				for(int c = 0; c < this.clonedMonApps.get(a).spatialRequirements.get(b).size(); c++) {
					int itemIndex = this.clonedMonApps.get(a).spatialRequirements.get(b).indexOf(-1);					
					if(a == monAppId && b == spatialReqId && itemIndex != -1) { //itemIndex != -1 -> it means there is a valid index where the item was set to '-1'
						//System.out.println("a: " + a + ", b: " + b + ", itemIndex: " + itemIndex);
						this.clonedMonApps.get(a).spatialRequirements.get(b).remove(itemIndex);
					}
				}
			}
		}
	}
	
	//remove empty tuples and empty mon apps (if any) - iterating it backwards allows me to remove and iterate simultaneously
	for(int i = this.clonedMonApps.size() - 1; i >= 0; --i) {
		for(int j = this.clonedMonApps.get(i).spatialRequirements.size() - 1; j >= 0; --j) {
			if(this.clonedMonApps.get(i).spatialRequirements.get(j).isEmpty()) {
				//System.out.println("i: " + i + ", j: " + j);
				this.clonedMonApps.get(i).spatialRequirements.remove(j);
				this.clonedMonApps.get(i).deviceToBeCollected.remove(j);
			}
		}
		//then, remove empty mon apps (if any) - i.e., mon apps where all spatial requirements are already satisfied
		if(this.clonedMonApps.get(i).spatialRequirements.isEmpty()) {
			this.clonedMonApps.remove(i);
		}
	}
	
	
	//try to insert unsatisfied tuple-items into the existing probe paths
	/*for(int i = this.clonedMonApps.size() - 1; i >= 0; --i) {
		for(int j = this.clonedMonApps.get(i).spatialRequirements.size() - 1; j >= 0; --j) {
			for(int k = this.clonedMonApps.get(i).spatialRequirements.get(j).size() - 1; k >= 0; --k) {
				int dev = this.clonedMonApps.get(i).deviceToBeCollected.get(j);
				int item = this.clonedMonApps.get(i).spatialRequirements.get(j).get(k);
				
				//iterate cycles
				for(int a = this.cycles.size() - 1; a >= 0; --a) {
					//if the path contains the node/device and has enough space, collect it and mark it as collected
					if(this.cycles.get(a).hasDevice(dev) &&  
							this.cycles.get(a).capacity_used + infra.sizeTelemetryItems[item] <= this.cycles.get(a).capacity) {
						
						Tuple devItem = new Tuple(dev, item);
		
						if(!this.cycles.get(a).itemPerCycle.contains(devItem) && !collected[devItem.getDevice()][devItem.getItem()]) {
							this.cycles.get(a).itemPerCycle.add(devItem);
							this.cycles.get(a).capacity_used += infra.sizeTelemetryItems[item];
							collected[devItem.getDevice()][devItem.getItem()] = true;
						}
						this.clonedMonApps.get(i).spatialRequirements.get(j).remove(k); //remove item
					}
				}
			}
			if(this.clonedMonApps.get(i).spatialRequirements.get(j).isEmpty()) { //satisfy the spatial requirement -- i.e., delete it from mon app
				this.clonedMonApps.get(i).spatialRequirements.remove(j);
				this.clonedMonApps.get(i).deviceToBeCollected.remove(j);
			}
		}
		if(this.clonedMonApps.get(i).spatialRequirements.isEmpty()) {
			this.clonedMonApps.remove(i);
		}
	}*/
 
	//NOTE: Check if all items are being collected correctly
	/*for(int i = this.clonedMonApps.size() - 1; i >= 0; --i) {
		for(int j = this.clonedMonApps.get(i).spatialRequirements.size() - 1; j >= 0; --j) {
			for(int k = this.clonedMonApps.get(i).spatialRequirements.get(j).size() - 1; k >= 0; --k) {
				System.out.println("this item must be collected!");
			}
		}
	}*/
	
	//Create new probe paths if necessary 
	//NOTE2: Comment the for-group above to simulate a scenario where there are remaining items to be collected
	
	//this method return a arraylist of cycles. I must add these cycles to my solution
	ArrayList<Cycle> tempCycles = new ArrayList<Cycle>();
	tempCycles = firstApproach(this.clonedMonApps, this.capacityProbe);
	for(Cycle c: tempCycles) {
		this.cycles.add(c);
	}
	

	//post processing
	System.out.println("cycles size (after dynMon): " + this.cycles.size());
	for(int i = 0; i < this.cycles.size(); i++) {
		this.cycles.get(i).printCycleWithCapacity();	
	}
	
	
}


//(NOT WORKING CORRECTLY YET)
//NOTE: Need to check all changes made on firstApproach and apply it here
//this approach always creates the shortest path to the next spatial requirement node to create cycles 
public ArrayList<Cycle> secondApproach(ArrayList<MonitoringApp> monitoringApps, int capacityProbe) throws CloneNotSupportedException {
	ArrayList<Cycle> cycles_sol = new ArrayList<Cycle>();
	//ArrayList<ArrayList<Integer>> auxSpatialRequirements = ArrayList<ArrayList<Integer>> 
	
	this.clonedMonApps = monitoringApps;
	
	boolean[][] collectedItems = new boolean[this.infra.size][this.infra.telemetryItemsRouter]; //device, item
	
	int item = -1;
	int fixedEdgeCost = 1;
	int currProbeCapacity = capacityProbe;
	int oldProbeCapacity = capacityProbe;
	int probeId = 0;
	boolean flagReturnDepot = false;
	int total_items = 0;
	
	// # of remaining items to be collected
	int remainingItems = 0;
	for(int i = 0; i < clonedMonApps.size(); i++) {
		for(int j = 0; j < clonedMonApps.get(i).spatialRequirements.size(); j++) {
			for(int k = 0; k < clonedMonApps.get(i).spatialRequirements.get(j).size(); k++) {
				remainingItems++;
			}
		}
	}
	
	total_items = remainingItems;
	
	//System.out.println("<TEMP> items' size");
	/*for(int i = 0; i < infra.telemetryItemsRouter; i++) {
		System.out.println("item " + i + ", weight: " + infra.sizeTelemetryItems[i]);
	}*/
	
	do {
		
		
		System.out.println("---------------------------------NEW CYCLE-----------------------------------------------------------------");
		int currNode = -2;
		int lastNode = -2;
		int depot = -2;
		
		boolean[][] visited = new boolean[this.infra.size][this.infra.size];
		Pair<Integer,Integer> link = Pair.create(currNode, -1);
		
		//choose depot
		if(clonedMonApps.size() > 0) {
			depot = clonedMonApps.get(0).deviceToBeCollected.get(0);
			//System.out.println("DEPOT... " + depot);
		}else {
			break;
		}
		
		Cycle c = new Cycle();
		c.capacity = capacityProbe;
		
		currNode = depot; // starting point]
		
		do {
		
			//for(int i = 0; i < clonedMonApps.size(); i++) {
			while(clonedMonApps.size() > 0) {
				int i = 0;
				System.out.println("curr tuples........................: " + clonedMonApps.get(i).spatialRequirements);
				//iterate over all sets of spatial requirements
				while(clonedMonApps.get(i).spatialRequirements.size() > 0) {
					
					//System.out.println("currNode.........: " + currNode);
					//for(int k = 0; k < clonedMonApps.get(i).spatialRequirements.size(); k++) { //iterate over mon apps' spatial requirements (item tuples)
					//System.out.println("curr dev.................: " + clonedMonApps.get(i).deviceToBeCollected.get(k));
					//System.out.println("probe capacity: " + currProbeCapacity);
					//System.out.println("remainingItems: " + remainingItems);
					//System.out.println("cloned size: " + clonedMonApps.size());
					
					
					//int numItemsCurrSpatialDep = clonedMonApps.get(i).spatialRequirements.get(k).size();
					int numItemsCurrSpatialDep = clonedMonApps.get(i).spatialRequirements.get(0).size();
					//System.out.println("num item curr spatial dep comecooo: " + numItemsCurrSpatialDep);
					ArrayList<Integer> auxRemoveCurrTuple = new ArrayList<Integer>();
					//System.out.println("numItemsSpatialDep: " + numItemsCurrSpatialDep);
					
					//current tuple 
					for(int l = 0; l < clonedMonApps.get(i).spatialRequirements.get(0).size(); l++) {
						item = clonedMonApps.get(i).spatialRequirements.get(0).get(l);
						//System.out.println("item (out): " + item);
						//add items to probe - if it has enough available space
						
						if(collectedItems[currNode][item]) {
							//items to be removed
							auxRemoveCurrTuple.add(item);
							numItemsCurrSpatialDep -= 1;
							remainingItems -= 1; 
						}
						
						//System.out.println("collected[" + currNode + "][" + item + "]: " + collectedItems[currNode][item]);
						if (currProbeCapacity - infra.sizeTelemetryItems[item] > 0 && !collectedItems[currNode][item]) {
							//System.out.println("item (in): " + item);
							currProbeCapacity -= infra.sizeTelemetryItems[item];
							collectedItems[currNode][item] = true;
							numItemsCurrSpatialDep -= 1; // local counter (current tuple)
							//System.out.println("numItemsCurrSpatialDep: " + numItemsCurrSpatialDep);
							remainingItems -= 1; // global counter
							
							//cycle managing
							Tuple devItem = new Tuple(currNode, item);
							c.itemPerCycle.add(devItem);
							c.capacity_used += infra.sizeTelemetryItems[item];
							
							//items to be removed
							auxRemoveCurrTuple.add(item);
						}
					}
					
					//System.out.println("auxRemoveCurrTuple: " + auxRemoveCurrTuple);
					//remove collected items
					/*for(int x = 0; x < auxRemoveCurrTuple.size(); x++) {
						if(clonedMonApps.get(i).spatialRequirements.get(0).contains(auxRemoveCurrTuple.get(x))) {
							clonedMonApps.get(i).spatialRequirements.get(0).remove(auxRemoveCurrTuple.get(x));
						}
					}*/
					
					//System.out.println("numitemscurrspatialdep: " + numItemsCurrSpatialDep);
					//System.out.println("num items curr spatial dep asdjhasdjhaskjdhasd: " + numItemsCurrSpatialDep);
					//System.out.println("cloned spatial req: (before) " + clonedMonApps.get(i).spatialRequirements);
					//System.out.println("cloned dev req      (before): " + clonedMonApps.get(i).deviceToBeCollected);
					if(numItemsCurrSpatialDep == 0) { // e.g., before: Mon 0: < <2,3>, <4,5> > || after: Mon 0: < <4,5> >
						clonedMonApps.get(i).spatialRequirements.remove(0);
						clonedMonApps.get(i).deviceToBeCollected.remove(0);	
					}
					//System.out.println("cloned spatial req (after): " + clonedMonApps.get(i).spatialRequirements);
					//System.out.println("cloned dev req     (after): " + clonedMonApps.get(i).deviceToBeCollected);

					
					//Choose the next node - i.e., create a cycle link
					flagCreatePath = false;
					link = generateLink2(depot, currNode, lastNode, currProbeCapacity, fixedEdgeCost, visited, clonedMonApps.get(i).deviceToBeCollected, clonedMonApps); 
					//System.out.println("link: " + link);
					if(link.second == -99) { //no more edges to visit. So, return to the depot
						//System.out.println("flag to return = TRUE");
						flagReturnDepot = true;
					}else {
						//System.out.println("flagcreate(main): " + flagCreatePath);
						if(flagCreatePath) { //create shortest path to edge.second
							ArrayList<Integer> shortPath = new ArrayList<Integer>();
							
							if(link.first == link.second && link.second == depot) {
								flagReturnDepot = true;
								continue;
							}
							
							else if(link.first == link.second && link.second != depot) {
								continue;
							}
							
							//if both nodes are feasible
							else if(link.first >= 0 && link.second >= 0 && link.second != depot) {
								//System.out.println("link (else):" + link + ", visited?: " + visited[link.first][link.second] + ", depot: " + depot);
								shortPath = infra.getShortestPath(link.first, link.second);
								//create pairs from shortest path
								int node1 = -33;
								int node2 = -66;
								//System.out.println("shortPath: " + shortPath);
								for(int z = 0; z < shortPath.size() - 1; z++) {
									node1 = shortPath.get(z);
									node2 = shortPath.get(z+1);
									Pair<Integer,Integer> p = Pair.create(node1, node2);
									visited[node1][node2] = true;
									c.links.add(p); //add to current circuit
									
								} //OBSERVACAO: EXISTE UM PROBLEMA NO CRITERIO DE ESCOLHA DO DISPOSITIVOS PQ ELE ESTA OS DEVICES SATISFEITOS DE UMA FORMA ESTRANHA
								lastNode = node1;
								currNode = node2;
								//System.out.println("path: " + c.links);
								//System.out.println("lastNode: " + lastNode);
								//System.out.println("currNode: " + currNode);
							}else { //if only the first is feasible (i.e., 'edge.second = -88' case)
								flagReturnDepot = true;
							}	
						}else {
							c.links.add(link);
							visited[link.first][link.second] = true;
							lastNode = link.first;
							currNode = link.second;
						}	
					}
					
					//System.out.println("remaining items (after): " + remainingItems);
				}
				
				
				//System.out.println("cloned spatial req (outer): " + clonedMonApps.get(i).spatialRequirements);
				if(clonedMonApps.get(i).spatialRequirements.size() == 0) {
					flagReturnDepot = true;
					clonedMonApps.remove(i);
				}
				
				if(flagReturnDepot) {
					break;
				}

				
			}
		
		 //System.out.println("clonedMonApps: " + clonedMonApps);
		}while(clonedMonApps.size() > 0 && currProbeCapacity > capacityProbe/2 && !flagReturnDepot); //upforward steps
		//do-while - backwards steps - i.e., connecting the cycle towards the 'depot'
		
		//reset flagReturnDepot
		flagReturnDepot = false;
		
		//link the last node to the depot
		Pair<Integer,Integer> lastLinkCycle = Pair.create(-1, -1);
		lastLinkCycle = c.links.get(c.links.size()-1);
		
		if(lastLinkCycle.second != depot) {
			//reconstruct the path to depot using the "costShortestPath" structure
			ArrayList<Integer> shortPath = new ArrayList<Integer>();
			
			//System.out.println("edge.first: " + edge.first + ", edge.second: " + edge.second);
			//System.out.println("remainingEdges: " + remainingEdges);
			
			//if both nodes are feasible
			if(link.first >= 0 && link.second >= 0) {
				shortPath = infra.getShortestPath(link.second, depot);
			}else { //if only the first is feasible
				shortPath = infra.getShortestPath(link.first, depot);
			}
			//System.out.println("shortPath (outer): " + shortPath);
			//clonedMonApps.get(0).printMonitoringApps(clonedMonApps);
			
			for(int k = 0; k < shortPath.size() - 1; k++) {
				int node1 = shortPath.get(k);
				int node2 = shortPath.get(k+1);
				Pair<Integer,Integer> p = Pair.create(node1, node2);
				c.links.add(p); //add to current circuit
				visited[node1][node2] = true;
				
				//if the edge contains the 'depot' it's already 
				if(node1 != depot && node2 != depot) {
					currProbeCapacity -= fixedEdgeCost;	
				}
				
				
				//TO BE CODED: TRY TO COLLECT ITEMS WHILE RETURNING TO DEPOT
				//...
				int r = 0;
				ArrayList<Integer> auxRemoveCurrTuple = new ArrayList<Integer>();
						
				int indexNode1 = -1;
				int indexNode2 = -1;
				for(r = 0; r < clonedMonApps.size(); r++) {
					System.out.println("devices1: " + clonedMonApps.get(r).deviceToBeCollected);
					indexNode1 = clonedMonApps.get(r).deviceToBeCollected.indexOf(node1); // -1, if not found / index #, if found
					System.out.println("node1: " + node1);
					System.out.println("indexNode1: " + indexNode1);
					break;
				}
				
				if(r == clonedMonApps.size()) {
					r = r - 1;	
				}
				
				//check node1
				if(indexNode1 != -1) {
					int nodeAux = clonedMonApps.get(r).deviceToBeCollected.get(indexNode1);
					int spatialDepCurr = clonedMonApps.get(r).deviceToBeCollected.size();
					for(int s = 0; s < clonedMonApps.get(r).spatialRequirements.get(indexNode1).size(); s++) {
						item = clonedMonApps.get(r).spatialRequirements.get(indexNode1).get(s);
						
						if(collectedItems[nodeAux][item]) {
							//items to be removed
							auxRemoveCurrTuple.add(item);
							spatialDepCurr -= 1;
							remainingItems -= 1; 
						}
						
						//add items to probe - if it has enough available space
						if (currProbeCapacity - infra.sizeTelemetryItems[item] > 0 && !collectedItems[nodeAux][item]) {
							//System.out.println("item (in): " + item);
							currProbeCapacity -= infra.sizeTelemetryItems[item];
							collectedItems[nodeAux][item] = true;
							spatialDepCurr -= 1; // local counter (current tuple)
							//System.out.println("numItemsCurrSpatialDep: " + numItemsCurrSpatialDep);
							remainingItems -= 1; // global counter
							
							//cycle managing
							Tuple devItem = new Tuple(nodeAux, item);
							c.itemPerCycle.add(devItem);
							c.capacity_used += infra.sizeTelemetryItems[item];
							
							//items to be removed
							auxRemoveCurrTuple.add(item);
							//clonedMonApps.get(i).spatialRequirements.get(k).remove(l);
						}
					}
					
					//remove collected items
					for(int x = 0; x < auxRemoveCurrTuple.size(); x++) {
						if(clonedMonApps.get(r).spatialRequirements.get(indexNode1).contains(auxRemoveCurrTuple.get(x))) {
							clonedMonApps.get(r).spatialRequirements.get(indexNode1).remove(auxRemoveCurrTuple.get(x));
						}
					}
					
					if(spatialDepCurr == 0) { // e.g., before: Mon 0: < <2,3>, <4,5> > || after: Mon 0: < <4,5> >
						clonedMonApps.get(r).spatialRequirements.remove(indexNode1);
						clonedMonApps.get(r).deviceToBeCollected.remove(indexNode1);						
					}
					
				}
				
				auxRemoveCurrTuple = new ArrayList<Integer>();
				
				for(r = 0; r < clonedMonApps.size(); r++) {
					System.out.println("devices2: " + clonedMonApps.get(r).deviceToBeCollected);
					indexNode2 = clonedMonApps.get(r).deviceToBeCollected.indexOf(node2); // -1, if not found / index #, if found
					System.out.println("node2: " + node2);
					System.out.println("indexNode2: " + indexNode1);
					break;
				}
				
				if(r == clonedMonApps.size()) {
					r = r - 1;	
				}
				
				if(indexNode2 != -1) { //check node2
					int nodeAux = clonedMonApps.get(r).deviceToBeCollected.get(indexNode2);
					int spatialDepCurr = clonedMonApps.get(r).deviceToBeCollected.size();
					for(int s = 0; s < clonedMonApps.get(r).spatialRequirements.get(indexNode2).size(); s++) {
						item = clonedMonApps.get(r).spatialRequirements.get(indexNode2).get(s);
						
						if(collectedItems[nodeAux][item]) {
							//items to be removed
							auxRemoveCurrTuple.add(item);
							spatialDepCurr -= 1;
							remainingItems -= 1; 
						}
						
						//add items to probe - if it has enough available space
						if (currProbeCapacity - infra.sizeTelemetryItems[item] > 0 && !collectedItems[nodeAux][item]) {
							//System.out.println("item (in): " + item);
							currProbeCapacity -= infra.sizeTelemetryItems[item];
							collectedItems[nodeAux][item] = true;
							spatialDepCurr -= 1; // local counter (current tuple)
							//System.out.println("numItemsCurrSpatialDep: " + numItemsCurrSpatialDep);
							remainingItems -= 1; // global counter
							
							//cycle managing
							Tuple devItem = new Tuple(nodeAux, item);
							c.itemPerCycle.add(devItem);
							c.capacity_used += infra.sizeTelemetryItems[item];
							
							//items to be removed
							auxRemoveCurrTuple.add(item);
							//clonedMonApps.get(i).spatialRequirements.get(k).remove(l);
						}
					}
					
					//remove collected items
					for(int x = 0; x < auxRemoveCurrTuple.size(); x++) {
						if(clonedMonApps.get(r).spatialRequirements.get(indexNode2).contains(auxRemoveCurrTuple.get(x))) {
							clonedMonApps.get(r).spatialRequirements.get(indexNode2).remove(auxRemoveCurrTuple.get(x));
						}
					}
					
					if(spatialDepCurr == 0) { // e.g., before: Mon 0: < <2,3>, <4,5> > || after: Mon 0: < <4,5> >
						clonedMonApps.get(r).spatialRequirements.remove(indexNode2);
						clonedMonApps.get(r).deviceToBeCollected.remove(indexNode2);						
					}
					
				}else {
					continue;
				}
				
			}
		}
		
		
		//add cycle to the list of cycles
		c.cycle_id = probeId;
		cycles_sol.add(c);
		probeId++;
		c.printCycleWithCapacity();
		currProbeCapacity = oldProbeCapacity; //reset probe capacity
	
	}while(clonedMonApps.size() > 0 && remainingItems > 0); //NOTE: THIS APPROACH IS GENERATING CYCLES OF SIZE ONE!!

	
	//count collected items
	System.out.println("remainingItems: " + remainingItems + ", total items: " + total_items);
	
	
	return cycles_sol;
	
}
	


public Pair<Integer,Integer> generateLink2(int depot, int node1, int lastNode, int currProbeCapacity, int fixedEdgeCost,
		boolean[][] visited, ArrayList<Integer> deviceToBeCollected, ArrayList<MonitoringApp> clonedMonApps) {
	

	Pair<Integer,Integer> edge = Pair.create(node1, -99);
	Random rnd = new Random(this.seed);
	ArrayList<Integer> neighboursList = new ArrayList<Integer>();
	
	//get neighbours (to avoid random loops)
	for(int j = 0; j < this.infra.size; j++) {
		if(node1 != j && this.infra.graph[node1][j] == 1 &&
			!visited[node1][j] && j != lastNode && j != depot) {
			
			neighboursList.add(j);
		}
	}
	
	//check if any neighbour is also a "device to be collected"
	for(int i = 0; i < deviceToBeCollected.size(); i++) {
		if(neighboursList.contains(deviceToBeCollected.get(i))) { // if the neighbour is also a requirement device, use it to create the next link
			edge.second = deviceToBeCollected.get(i);
			visited[node1][edge.second] = true;
			currProbeCapacity -= fixedEdgeCost;
			//System.out.println("node1: " + node1 + ", edge.second: " + edge.second);
			return edge;
		}
	}
	
	//System.out.println("GENERATE LINK: ......" );
	//System.out.println("neighbours: " + neighboursList);
	//System.out.println("link (in): " + edge);
	//System.out.println("deviceToBeCollected: " + deviceToBeCollected);
	//System.out.println("edge.second (out): " + edge.second);
	//if there is at least one feasible neighbour and is not a 'device to be collected'
	if(!neighboursList.isEmpty() && edge.second == -99) { //look for the shortest path to the next spatial requirement
		
		if(deviceToBeCollected.size() > 0) {
			edge.second = deviceToBeCollected.get(rnd.nextInt(deviceToBeCollected.size()));
			//System.out.println("edgeee.seeconod:" + edge.second);
			if(deviceToBeCollected.size() > 1) {
				//System.out.println("device size: " + deviceToBeCollected);
				while(edge.second == depot) {
					
					edge.second = deviceToBeCollected.get(rnd.nextInt(deviceToBeCollected.size()));	
				}
			}else if(deviceToBeCollected.size() == 1 && edge.second == depot) {
				this.flagEqualDepot = true;
				return edge;
			}
		}
		
		if (edge.second != depot){
			this.flagCreatePath = true;	
		}
		//return a flag to create a shortest path to it, since it is not a neighbour
		//System.out.println("node1: " + node1 + ", edge.second: " + edge.second);
		
		
		return edge;
	}
	
	// if pair is (node, -2) at the end, it means it completed a circuit and there is no remaining edge to the current node,...
	// ... so, one have to choose how to treat this issue.
	//System.out.println("flagCreatepath (generate link): " + flagCreatePath);
	//System.out.println("node1: " + node1 + ", edge.second: " + edge.second);
	return edge;
}



public ArrayList<Cycle> addNodesToCycle(ArrayList<Cycle> cycles_sol) {
	ArrayList<Integer> path;
	for(int i = 0; i < cycles_sol.size(); i++) {	
		path = new ArrayList<Integer>();
		
		int j = 0;
		for(; j < cycles_sol.get(i).links.size(); j++) {
			path.add(cycles_sol.get(i).links.get(j).first);
		}
		path.add(cycles_sol.get(i).links.get(j-1).second);
		
		cycles_sol.get(i).nodes = path;
		//j = 0;
	}
	
	return cycles_sol;
}

	


}