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
import java.util.Set;
import java.util.Map.Entry;


public class MonAppSpatialTemporalApproaches{
	public ArrayList<Cycle> cycles;
	public ArrayList<MonitoringApp> monApps;
	public int[][] costShortestPath;
	public NetworkInfrastructure infra;
	long seed;
	public int capacityProbe;
	public boolean flagCreatePath = false; //either I let it like this, or return a vector [edge.first, edge.second, flagCreatePath]
	public boolean flagEqualDepot = false;
	public int seedChanger; //each time a link is created, we change the seed, incrementing it by 1
	
	public MonAppSpatialTemporalApproaches(NetworkInfrastructure infra, long seed, int capacityProbe) {
		this.cycles = new ArrayList<Cycle>();
		this.infra = infra;
		this.costShortestPath = new int[infra.size][infra.size];
		this.seed = seed;
		this.monApps = new ArrayList<MonitoringApp>();
		this.capacityProbe = capacityProbe;
		this.seedChanger = 0;
		
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
	
	
public ArrayList<MonitoringApp> cloneMonitoringApps(ArrayList<MonitoringApp> monitoringApps) throws CloneNotSupportedException {
	ArrayList<MonitoringApp> clonedMonApps = new ArrayList<MonitoringApp>();

	for(int i = 0; i < monitoringApps.size(); i++) {
		MonitoringApp newMonApp = monitoringApps.get(i).clone();
		clonedMonApps.add(newMonApp);
		this.monApps.add(newMonApp);
	}
	
	return clonedMonApps;
}

//this approach prioritizes to select the next node to be a "device to be collected". If none, it randomly chooses a next neighbour node to create a cycle
public ArrayList<Cycle> firstApproach(ArrayList<MonitoringApp> monitoringApps, int capacityProbe) throws CloneNotSupportedException {
	
	ArrayList<Cycle> cycles_sol = new ArrayList<Cycle>();
	this.monApps = monitoringApps;
	
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
	for(int i = 0; i < monApps.size(); i++) {
		for(int j = 0; j < monApps.get(i).spatialRequirements.size(); j++) {
			for(int k = 0; k < monApps.get(i).spatialRequirements.get(j).size(); k++) {
				remainingItems++;
			}
		}
	}
	
	total_items = remainingItems;
	
	do {
		//System.out.println("---------------------------------NEW CYCLE-----------------------------------------------------------------");
		int currNode = -2;
		int lastNode = -2;
		int depot = -2;
		
		boolean[][] visited = new boolean[this.infra.size][this.infra.size];
		Pair<Integer,Integer> link = Pair.create(currNode, -1);
		
		//choose depot
		if(monApps.size() > 0) {
			depot = monApps.get(0).deviceToBeCollected.get(0);
			//System.out.println("DEPOT... " + depot);
		}else {
			break;
		}
		
		Cycle c = new Cycle();
		c.capacity = capacityProbe;
		
		currNode = depot; // starting point]
		
		do {
		
			//for(int i = 0; i < monApps.size(); i++) {
			while(monApps.size() > 0) {
				int i = 0;
				
				//this loop guarantees the current tuples contain at least once the current node to be satisfied
				boolean flagFoundIt = false;
				for(i = 0; i < monApps.size(); i++) {
					//System.out.println("i (in): " + i + ", currNode: " + currNode);
					if(monApps.get(i).deviceToBeCollected.contains(currNode)) {
						flagFoundIt = true;
						break;
					}
				}
				//System.out.println("mon app: " + i + ", cloned size all: " + monApps.size());
				
				if(flagFoundIt) {
					//current tuples
					//System.out.println("curr tuples........................: " + monApps.get(i).spatialRequirements);
					//System.out.println("curr devices.......................: " + monApps.get(i).deviceToBeCollected);
					while(monApps.get(i).spatialRequirements.size() > 0) { 
						
						int y = 0;
						y = monApps.get(i).deviceToBeCollected.indexOf(currNode);
						if(y < 0) {
							break;
						}
						//System.out.println("who is 'y'...: " + y);
						
						int numItemsCurrSpatialDep = monApps.get(i).spatialRequirements.get(y).size();
						ArrayList<Integer> auxRemoveCurrTuple = new ArrayList<Integer>();
						
						
						//current tuple 
						for(int l = 0; l < monApps.get(i).spatialRequirements.get(y).size(); l++) {
							item = monApps.get(i).spatialRequirements.get(y).get(l);
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
							if(monApps.get(i).spatialRequirements.get(y).contains(auxRemoveCurrTuple.get(x))) {
								//System.out.println("items to be removed array (if): " + auxRemoveCurrTuple.get(x));
								monApps.get(i).spatialRequirements.get(y).remove(auxRemoveCurrTuple.get(x));
							}
						}

						
						//System.out.println("numItemsCurrSpatialDep: " + numItemsCurrSpatialDep);
						//System.out.println("cloned spatial req: (before) " + monApps.get(i).spatialRequirements);
						//System.out.println("cloned dev req      (before): " + monApps.get(i).deviceToBeCollected);
						if(numItemsCurrSpatialDep == 0) { // e.g., before: Mon 0: < <2,3>, <4,5> > || after: Mon 0: < <4,5> >
							monApps.get(i).spatialRequirements.remove(y);
							monApps.get(i).deviceToBeCollected.remove(y);						
						}
						//System.out.println("cloned spatial req (after): " + monApps.get(i).spatialRequirements);
						//System.out.println("cloned dev req     (after): " + monApps.get(i).deviceToBeCollected);
	
						
						//Choose the next node - i.e., create a cycle link
						if(monApps.get(i).deviceToBeCollected.contains(currNode)) {
							continue;
						}else {
							link = generateLink(depot, currNode, lastNode, currProbeCapacity, fixedEdgeCost, visited, monApps.get(i).deviceToBeCollected);	
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
				
					//System.out.println("cloned spatial req (outer): " + monApps.get(i).spatialRequirements);
					if(monApps.get(i).spatialRequirements.size() == 0) {
						//flagReturnDepot = true;
						//System.out.println("currNode (if below): " + currNode);
						monApps.remove(i);
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
		
		 //System.out.println("monApps: " + monApps);
		}while(monApps.size() > 0 && currProbeCapacity > capacityProbe/2 && !flagReturnDepot); //upforward steps
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
	
	}while(this.monApps.size() > 0 || remainingItems > 0);

	
	for(int i = cycles_sol.size() - 1; i >= 0 ; --i) {
		if(cycles_sol.get(i).itemPerCycle.isEmpty()) {
			cycles_sol.remove(i);
		}
	}
	
	System.out.println("cycles size (before simpleOptimizer): " + cycles_sol.size());
	//cycles_sol = simpleOptimizer(cycles_sol); //OPTIMIZER (REDUCE # OF CYCLES)	
	
	System.out.println("cycles size (after simpleOptimizer): " + cycles_sol.size());
	//print cycles
	/*for(int i = 0; i < cycles_sol.size(); i++) {
		cycles_sol.get(i).printCycleWithCapacity();	
	}*/

	
	return cycles_sol;
}


//reduces the number of cycles in the current solution
public ArrayList<Cycle> simpleOptimizer(ArrayList<Cycle> cycles_sol) {
	System.out.println("-----------------------SIMPLE OPTIMIZER-----------------------");
	
	//remove useless cycles - i.e., not collecting items
	/*for(int i = cycles_sol.size() - 1; i >= 0 ; --i) {
		if(cycles_sol.get(i).itemPerCycle.isEmpty()) {
			cycles_sol.remove(i);
		}
	}*/
	
	
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
				if(cycles_sol.get(indexHighest).nodes.contains(devItem.getFirst())) {
					int resilientCap = cycles_sol.get(indexHighest).capacity - cycles_sol.get(indexHighest).capacity_used;
					//System.out.println("resilientCap: " + resilientCap);
					if(resilientCap - infra.sizeTelemetryItems[devItem.getSecond()] >= 0) {
						//most utilized cycle
						cycles_sol.get(indexHighest).itemPerCycle.add(devItem); //add item
						cycles_sol.get(indexHighest).capacity_used += infra.sizeTelemetryItems[devItem.getSecond()]; //update capacity
						
						//least utilized cycle
						itemsToBeRemoved.add(devItem); //remove item (later)
						cycles_sol.get(i).capacity_used -= infra.sizeTelemetryItems[devItem.getSecond()]; //update capacity
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
	
	//update probe id
	for(int i = 0; i < cycles_sol.size(); i++) {
		cycles_sol.get(i).cycle_id = i;
	}
	
	return cycles_sol;
}


public Pair<Integer,Integer> generateLink(int depot, int node1, int lastNode, int currProbeCapacity, int fixedEdgeCost,
		boolean[][] visited, ArrayList<Integer> deviceToBeCollected) {
	
	Pair<Integer,Integer> edge = Pair.create(node1, -99);
	Random rnd = new Random(this.seed + this.seedChanger);
	this.seedChanger++;
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


//(NOT WORKING CORRECTLY YET)
//NOTE: Need to check all changes made on firstApproach and apply it here
//this approach always creates the shortest path to the next spatial requirement node to create cycles 
public ArrayList<Cycle> secondApproach(ArrayList<MonitoringApp> monitoringApps, int capacityProbe) throws CloneNotSupportedException {
	ArrayList<Cycle> cycles_sol = new ArrayList<Cycle>();
	//ArrayList<ArrayList<Integer>> auxSpatialRequirements = ArrayList<ArrayList<Integer>> 
	
	this.monApps = monitoringApps;
	
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
	for(int i = 0; i < monApps.size(); i++) {
		for(int j = 0; j < monApps.get(i).spatialRequirements.size(); j++) {
			for(int k = 0; k < monApps.get(i).spatialRequirements.get(j).size(); k++) {
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
		if(monApps.size() > 0) {
			depot = monApps.get(0).deviceToBeCollected.get(0);
			//System.out.println("DEPOT... " + depot);
		}else {
			break;
		}
		
		Cycle c = new Cycle();
		c.capacity = capacityProbe;
		
		currNode = depot; // starting point]
		
		do {
		
			//for(int i = 0; i < monApps.size(); i++) {
			while(monApps.size() > 0) {
				int i = 0;
				System.out.println("curr tuples........................: " + monApps.get(i).spatialRequirements);
				//iterate over all sets of spatial requirements
				while(monApps.get(i).spatialRequirements.size() > 0) {
					
					//System.out.println("currNode.........: " + currNode);
					//for(int k = 0; k < monApps.get(i).spatialRequirements.size(); k++) { //iterate over mon apps' spatial requirements (item tuples)
					//System.out.println("curr dev.................: " + monApps.get(i).deviceToBeCollected.get(k));
					//System.out.println("probe capacity: " + currProbeCapacity);
					//System.out.println("remainingItems: " + remainingItems);
					//System.out.println("cloned size: " + monApps.size());
					
					
					//int numItemsCurrSpatialDep = monApps.get(i).spatialRequirements.get(k).size();
					int numItemsCurrSpatialDep = monApps.get(i).spatialRequirements.get(0).size();
					//System.out.println("num item curr spatial dep comecooo: " + numItemsCurrSpatialDep);
					ArrayList<Integer> auxRemoveCurrTuple = new ArrayList<Integer>();
					//System.out.println("numItemsSpatialDep: " + numItemsCurrSpatialDep);
					
					//current tuple 
					for(int l = 0; l < monApps.get(i).spatialRequirements.get(0).size(); l++) {
						item = monApps.get(i).spatialRequirements.get(0).get(l);
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
						if(monApps.get(i).spatialRequirements.get(0).contains(auxRemoveCurrTuple.get(x))) {
							monApps.get(i).spatialRequirements.get(0).remove(auxRemoveCurrTuple.get(x));
						}
					}*/
					
					//System.out.println("numitemscurrspatialdep: " + numItemsCurrSpatialDep);
					//System.out.println("num items curr spatial dep asdjhasdjhaskjdhasd: " + numItemsCurrSpatialDep);
					//System.out.println("cloned spatial req: (before) " + monApps.get(i).spatialRequirements);
					//System.out.println("cloned dev req      (before): " + monApps.get(i).deviceToBeCollected);
					if(numItemsCurrSpatialDep == 0) { // e.g., before: Mon 0: < <2,3>, <4,5> > || after: Mon 0: < <4,5> >
						monApps.get(i).spatialRequirements.remove(0);
						monApps.get(i).deviceToBeCollected.remove(0);	
					}
					//System.out.println("cloned spatial req (after): " + monApps.get(i).spatialRequirements);
					//System.out.println("cloned dev req     (after): " + monApps.get(i).deviceToBeCollected);

					
					//Choose the next node - i.e., create a cycle link
					flagCreatePath = false;
					link = generateLink2(depot, currNode, lastNode, currProbeCapacity, fixedEdgeCost, visited, monApps.get(i).deviceToBeCollected, monApps); 
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
				
				
				//System.out.println("cloned spatial req (outer): " + monApps.get(i).spatialRequirements);
				if(monApps.get(i).spatialRequirements.size() == 0) {
					flagReturnDepot = true;
					monApps.remove(i);
				}
				
				if(flagReturnDepot) {
					break;
				}

				
			}
		
		 //System.out.println("monApps: " + monApps);
		}while(monApps.size() > 0 && currProbeCapacity > capacityProbe/2 && !flagReturnDepot); //upforward steps
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
			//monApps.get(0).printMonitoringApps(monApps);
			
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
				for(r = 0; r < monApps.size(); r++) {
					System.out.println("devices1: " + monApps.get(r).deviceToBeCollected);
					indexNode1 = monApps.get(r).deviceToBeCollected.indexOf(node1); // -1, if not found / index #, if found
					System.out.println("node1: " + node1);
					System.out.println("indexNode1: " + indexNode1);
					break;
				}
				
				if(r == monApps.size()) {
					r = r - 1;	
				}
				
				//check node1
				if(indexNode1 != -1) {
					int nodeAux = monApps.get(r).deviceToBeCollected.get(indexNode1);
					int spatialDepCurr = monApps.get(r).deviceToBeCollected.size();
					for(int s = 0; s < monApps.get(r).spatialRequirements.get(indexNode1).size(); s++) {
						item = monApps.get(r).spatialRequirements.get(indexNode1).get(s);
						
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
							//monApps.get(i).spatialRequirements.get(k).remove(l);
						}
					}
					
					//remove collected items
					for(int x = 0; x < auxRemoveCurrTuple.size(); x++) {
						if(monApps.get(r).spatialRequirements.get(indexNode1).contains(auxRemoveCurrTuple.get(x))) {
							monApps.get(r).spatialRequirements.get(indexNode1).remove(auxRemoveCurrTuple.get(x));
						}
					}
					
					if(spatialDepCurr == 0) { // e.g., before: Mon 0: < <2,3>, <4,5> > || after: Mon 0: < <4,5> >
						monApps.get(r).spatialRequirements.remove(indexNode1);
						monApps.get(r).deviceToBeCollected.remove(indexNode1);						
					}
					
				}
				
				auxRemoveCurrTuple = new ArrayList<Integer>();
				
				for(r = 0; r < monApps.size(); r++) {
					System.out.println("devices2: " + monApps.get(r).deviceToBeCollected);
					indexNode2 = monApps.get(r).deviceToBeCollected.indexOf(node2); // -1, if not found / index #, if found
					System.out.println("node2: " + node2);
					System.out.println("indexNode2: " + indexNode1);
					break;
				}
				
				if(r == monApps.size()) {
					r = r - 1;	
				}
				
				if(indexNode2 != -1) { //check node2
					int nodeAux = monApps.get(r).deviceToBeCollected.get(indexNode2);
					int spatialDepCurr = monApps.get(r).deviceToBeCollected.size();
					for(int s = 0; s < monApps.get(r).spatialRequirements.get(indexNode2).size(); s++) {
						item = monApps.get(r).spatialRequirements.get(indexNode2).get(s);
						
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
							//monApps.get(i).spatialRequirements.get(k).remove(l);
						}
					}
					
					//remove collected items
					for(int x = 0; x < auxRemoveCurrTuple.size(); x++) {
						if(monApps.get(r).spatialRequirements.get(indexNode2).contains(auxRemoveCurrTuple.get(x))) {
							monApps.get(r).spatialRequirements.get(indexNode2).remove(auxRemoveCurrTuple.get(x));
						}
					}
					
					if(spatialDepCurr == 0) { // e.g., before: Mon 0: < <2,3>, <4,5> > || after: Mon 0: < <4,5> >
						monApps.get(r).spatialRequirements.remove(indexNode2);
						monApps.get(r).deviceToBeCollected.remove(indexNode2);						
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
	
	}while(monApps.size() > 0 && remainingItems > 0); //NOTE: THIS APPROACH IS GENERATING CYCLES OF SIZE ONE!!

	
	//count collected items
	System.out.println("remainingItems: " + remainingItems + ", total items: " + total_items);
	
	
	return cycles_sol;
	
}
	


public Pair<Integer,Integer> generateLink2(int depot, int node1, int lastNode, int currProbeCapacity, int fixedEdgeCost,
		boolean[][] visited, ArrayList<Integer> deviceToBeCollected, ArrayList<MonitoringApp> monApps) {
	

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



public void dynamicMonAppProbeGeneratorRestrict(ArrayList<MonitoringApp> newMonApps, int numMaxSpatialDependencies, int maxSizeSpatialDependency) throws CloneNotSupportedException {
	
	this.monApps = newMonApps;
	
	ArrayList<int[]> satisfiedTuples = new ArrayList<int[]>();
	
	//int[0]: mon app id, int[1]: dev id (spatial req id), int[2]: item position
	boolean[][][] collected = new boolean[this.monApps.size()][numMaxSpatialDependencies-1][maxSizeSpatialDependency]; 
	
	//Check what spatial requirements are already satisfied by the previous probe scheme
	for(int i = this.cycles.size() - 1; i >= 0; --i){
		for(int j = this.cycles.get(i).itemPerCycle.size() - 1; j >= 0; --j) {
			Tuple devItem = this.cycles.get(i).itemPerCycle.get(j);
			//System.out.println("devItem: " + devItem);
			
			//iterate over all mon apps and delete all spatial requirements containing the exactly same tuple (dev, item)
			for(int a = 0; a < this.monApps.size(); a++) {
				for(int b = 0; b < this.monApps.get(a).spatialRequirements.size(); b++) {
					for(int c = 0; c < this.monApps.get(a).spatialRequirements.get(b).size(); c++) {							
						//System.out.println("a: " + a + ", b: " + b + ", c: " + c);
						if(this.monApps.get(a).deviceToBeCollected.get(b) == devItem.getFirst() &&
								this.monApps.get(a).spatialRequirements.get(b).get(c) == devItem.getSecond()) {
							//System.out.println("devItem: " + devItem);
							//System.out.println("a: " + a + ", b: " + b + ", c: " + c);
							int[] aux = new int[3]; // int[0]: mon app id, int[1]: spatial req id, int[2]: item position
							aux[0] = a;
							aux[1] = b;
							aux[2] = c;
							satisfiedTuples.add(aux);
							collected[a][b][c] = true; //if the items is found in the cycle, mark it as already satisfied
						}
					}		
				}
			}
		}
	}
	
	
	//remove items collected for previous mon apps NOT included in the current ones
	for(int i = this.cycles.size() - 1; i >= 0; --i){
		for(int j = this.cycles.get(i).itemPerCycle.size() - 1; j >= 0; --j) {
			Tuple devItem = this.cycles.get(i).itemPerCycle.get(j);
			
			boolean monAppHasItem = false;
			for(int a = 0; a < this.monApps.size(); a++) {
				for(int b = 0; b < this.monApps.get(a).spatialRequirements.size(); b++) {
					for(int c = 0; c < this.monApps.get(a).spatialRequirements.get(b).size(); c++) {
						if(this.monApps.get(a).deviceToBeCollected.get(b) == devItem.getFirst() &&
								this.monApps.get(a).spatialRequirements.get(b).get(c) == devItem.getSecond()) {
							monAppHasItem = true;
							break;
						}
					}
				}
			}
			if(!monAppHasItem) {
				for(int x = this.cycles.size() - 1; x >= 0; --x) {
					
					if(this.cycles.get(x).itemPerCycle.contains(devItem)) {
						if(this.cycles.get(x).itemPerCycle.remove(devItem)) {
							//System.out.println("devItem removed: " + devItem);
							this.cycles.get(x).capacity_used -= infra.sizeTelemetryItems[devItem.getSecond()];
						}
					}
				}
			}
		}
	}

	
	//check spatial constraints - i.e., spatial requirement items must be collected by the same probe
	for(int i = 0; i < this.cycles.size(); i++) {
		System.out.println("Cycle: " + i + ":):):):):):):):):)");
		for(int a = this.monApps.size() - 1; a >= 0; --a) {
			for(int b = this.monApps.get(a).spatialRequirements.size() - 1; b >= 0 ; --b) {
				ArrayList<int[]> auxCurrSpatialItems = new ArrayList<int[]>();
				
				for(int c = this.monApps.get(a).spatialRequirements.get(b).size() - 1; c >= 0 ; --c) {
					int dev = this.monApps.get(a).deviceToBeCollected.get(b); 
					int item = this.monApps.get(a).spatialRequirements.get(b).get(c);
					Tuple devItem = new Tuple(dev,item);
					
					if(this.cycles.get(i).itemPerCycle.contains(devItem) && collected[a][b][c]) { //curr cycle collected the item
						//System.out.println("devItem found: " + devItem);
						int[] aux = new int[3]; //int[0]: probe ID, int[1]: device, int[2]: item
						aux[0] = this.cycles.get(i).cycle_id;
						aux[1] = dev;
						aux[2] = item;
						auxCurrSpatialItems.add(aux);
					}else if(!this.cycles.get(i).itemPerCycle.contains(devItem) && collected[a][b][c]) { //AT LEAST another cycle collected the item			
						for(int x = 0; x < this.cycles.size(); x++) {
							if(x != i) {
								if(this.cycles.get(x).itemPerCycle.contains(devItem) && collected[a][b][c]) {
									int[] aux = new int[3];
									aux[0] = this.cycles.get(x).cycle_id;
									aux[1] = dev;
									aux[2] = item;
									auxCurrSpatialItems.add(aux);
									break;
								}
							}
						}
					}else { //if no cycle has the item, then set its probe id to "-1" and try to reallocate it later (outter loop)
						int[] aux = new int[3];
						aux[0] = -1;
						aux[1] = dev;
						aux[2] = item;
						auxCurrSpatialItems.add(aux);
					}
					
				}
				
				/*for(int temp = 0; temp < auxCurrSpatialItems.size(); temp++) {
					System.out.println("probeID: " + auxCurrSpatialItems.get(temp)[0] + " // dev: " + auxCurrSpatialItems.get(temp)[1] +
							" // item: " + auxCurrSpatialItems.get(temp)[2]);
				}*/
				//System.out.println("array size: " + auxCurrSpatialItems.size() + ", curr spatial size: " + this.monApps.get(a).spatialRequirements.get(b).size());
				
				int currProbeId = this.cycles.get(i).cycle_id;
				boolean probeDiff = false; //if probe IDs are different, reallocate items
				for(int x = 0; x < auxCurrSpatialItems.size(); x++) {
					if(currProbeId != auxCurrSpatialItems.get(x)[0]) { //if at least one item is not collected, then I can't remove the spatial requirement
						//System.out.println("probeId: " + probeId + ", probeId2: " + auxCurrSpatialItems.get(x)[0]);
						probeDiff = true;
						break;
					}
				}
				
				//if all items are in the same probe (valid probe -- i.e., not '-1'), delete the spatial requirement and move on to the next spatial req
				if(!probeDiff && currProbeId != -1) {
					for(int c = monApps.get(a).spatialRequirements.get(b).size() - 1; c >= 0; --c) {
						collected[a][b][c] = true;
					}
					//this.monApps.get(a).spatialRequirements.remove(b);
					//this.monApps.get(a).deviceToBeCollected.remove(b);
				}else {
					//count the frequency of probes being used to collected the items from this spatial requirement
					Map<Integer,Integer> probeFrequency = new HashMap<Integer,Integer>(); // map (probeID, num items curr spatial req)
					for(int x = 0; x < auxCurrSpatialItems.size(); x++) {
						Integer count = probeFrequency.get(auxCurrSpatialItems.get(x)[0]);
						if(count == null) {
						    probeFrequency.put(auxCurrSpatialItems.get(x)[0], 1);
						}
						else {
						    probeFrequency.put(auxCurrSpatialItems.get(x)[0], count + 1);
						}
					}
					
					
					//get a valid probe ID collecting most of the items (does not include probe ID == '-1')
					int keyCollectingMost = -1;
					int mostItems = Integer.MIN_VALUE;
					for(Integer key : probeFrequency.keySet()) {
						if(key != -1 && probeFrequency.get(key) > mostItems) {
							mostItems = probeFrequency.get(key);
							keyCollectingMost = key;
						}
					}
					
		
					//if 'keyCollectingMost' is still '-1', all items from curr spatial req are not collected yet
					if(keyCollectingMost == -1) {
						//estimate what would cost to insert the whole unsatisfied spatial req into a cycle
						int cost = 0;
						for(int x = 0; x < auxCurrSpatialItems.size(); x++) {
							int item = auxCurrSpatialItems.get(x)[2];
							cost += infra.sizeTelemetryItems[item];
						}
						
						//find a cycle to fit all of them
						for(int x = 0; x < this.cycles.size(); x++) { 
							if(this.cycles.get(x).capacity_used + cost <= this.cycles.get(x).capacity) {
								for(int y = 0; y < auxCurrSpatialItems.size(); y++) {
									int dev = auxCurrSpatialItems.get(y)[1];
									int item = auxCurrSpatialItems.get(y)[2];
									Tuple devItem = new Tuple(dev,item);
									if(!this.cycles.get(x).itemPerCycle.contains(devItem) && this.cycles.get(x).nodes.contains(dev)) {
										this.cycles.get(x).itemPerCycle.add(devItem);
										this.cycles.get(x).capacity_used += infra.sizeTelemetryItems[item];	
									}
								}
								for(int c = monApps.get(a).spatialRequirements.get(b).size() - 1; c >= 0; --c) {
									collected[a][b][c] = true;
								}
								
								break;
							}
						}	
					}else { // at least one item is collected. So, try to fit the rest of them
	
						//get items' weight sum from other cycles - including non collected items (probeId '-1')
						int costOtherItems = 0;
						int costAllItems = 0;
						for(int x = 0; x < auxCurrSpatialItems.size(); x++) {
							int item = auxCurrSpatialItems.get(x)[2];
							if(auxCurrSpatialItems.get(x)[0] != keyCollectingMost) {
								costOtherItems += infra.sizeTelemetryItems[item];	
							}
							else {
								costAllItems += infra.sizeTelemetryItems[item];	
							}
						}
						costAllItems += costOtherItems;
						
						//try to fit the remaining items into the probe collecting most of the items
						if(this.cycles.get(keyCollectingMost).capacity_used + costOtherItems <= this.cycles.get(keyCollectingMost).capacity) {
							for(int y = 0; y < auxCurrSpatialItems.size(); y++) {
								int dev = auxCurrSpatialItems.get(y)[1];
								int item = auxCurrSpatialItems.get(y)[2];
								Tuple devItem = new Tuple(dev,item);
								if(!this.cycles.get(keyCollectingMost).itemPerCycle.contains(devItem)) {
									this.cycles.get(keyCollectingMost).itemPerCycle.add(devItem);
									this.cycles.get(keyCollectingMost).capacity_used += infra.sizeTelemetryItems[item];	
								}
								
								for(int c = monApps.get(a).spatialRequirements.get(b).size() - 1; c >= 0; --c) {
									collected[a][b][c] = true;
								}
								
							}
						}else{ //try to find another cycle and fit all items - including the partially collected
							   //(In this case, the probe collecting most items has not enough space to collect all items)
							
							// check the following: 
							//1. if: another spatial requirement needs this item, keep it in the last cycle and 
							//make a copy of it into the new one along with the rest of the items
							
							//2. else if: no other cycle needs it, remove it from the cycle and fit all of them into a new one (in the case of: there is
							//another cycle with available capacity)
							
							if(this.cycles.size() > 1) {
								boolean allFit = false; //tells whether the new cycle has available space to collected all items
								boolean hasAllDevices = true; //tells whether the new cycle contains all devices to collect all items
								
								for(int x = 0; x < this.cycles.size(); x++) {
									if(x != keyCollectingMost) {
										allFit = false; //tells whether the new cycle has available space to collected all items
										hasAllDevices = true; //tells whether the new cycle contains all devices to collect all items
										
										//pre-checking: if the new cycle has all the required devices to fit all the items										
										for(int y = 0; y < auxCurrSpatialItems.size(); y++) {
											Integer dev = auxCurrSpatialItems.get(y)[1];
											if(!this.cycles.get(x).nodes.contains(dev)) {
												hasAllDevices = true;
												break;
											}
										}
										
										if(!hasAllDevices) {
											break; //stop trying for once and go to the next spatial requirement
										}else if(this.cycles.get(x).capacity_used + costAllItems <= this.cycles.get(keyCollectingMost).capacity) {
											for(int y = 0; y < auxCurrSpatialItems.size(); y++) {
												int dev = auxCurrSpatialItems.get(y)[1];
												int item = auxCurrSpatialItems.get(y)[2];
												Tuple devItem = new Tuple(dev,item);
												
												if(!this.cycles.get(x).itemPerCycle.contains(devItem)) {
													this.cycles.get(x).itemPerCycle.add(devItem);
													this.cycles.get(x).capacity_used += infra.sizeTelemetryItems[item];	
												}
											}
											allFit = true;
											for(int c = monApps.get(a).spatialRequirements.get(b).size() - 1; c >= 0; --c) {
												collected[a][b][c] = true;
											}
											
										}
										if(allFit) { //sucessfully reallocated all items... go to the next spatial req
											break;
										}
									}	
								}
								
								
							}

						}
						
						
						if(probeFrequency.get(keyCollectingMost) == auxCurrSpatialItems.size() && keyCollectingMost != -1) {
							for(int c = monApps.get(a).spatialRequirements.get(b).size() - 1; c >= 0; --c) {
								collected[a][b][c] = true;
							}
							//this.monApps.get(a).spatialRequirements.remove(b);
							//this.monApps.get(a).deviceToBeCollected.remove(b);
						}else {
							//System.out.println("probeFrequency: " + probeFrequency);
							//System.out.println("key: " + keyCollectingMost);
							//System.out.println("value: " + probeFrequency.get(keyCollectingMost));
						}
					
					}
					
				}
				
			}
			
		}
		
	}
	
	
	//print collected
	/*for(int a = 0; a < collected.length; a++) {
		for(int b = 0; b < collected[a].length; b++) {
			for(int c = 0; c < collected[a][b].length; c++) {
				System.out.println("collected["+a+"]["+b+"]["+c+"] = " + collected[a][b][c]);
			}
		}
	}*/
	
	/*for(int i = 0; i < infra.telemetryItemsRouter; i++) {
		System.out.println("item: " + i + ": " + infra.sizeTelemetryItems[i]);
	}*/
	
	
	//remove empty cycles - if any
	for(int i = this.cycles.size() - 1; i >= 0; --i) {
		if(this.cycles.get(i).itemPerCycle.isEmpty()) {
			this.cycles.remove(i);
		}
	}
	
	
	
	//track fully satisfied spatial requirements to remove partially collected ones (or shadow ones -- i.e., satisfied twice or more)
	boolean[][][] whatProbesSatisfied = new boolean[this.cycles.size()][this.monApps.size()][numMaxSpatialDependencies]; //[index 0]: probe id, [index 1]: mon app, [index 2]: spatial req
	for(int a = 0; a < this.monApps.size(); a++) {
		for(int b = 0; b < this.monApps.get(a).spatialRequirements.size(); b++) {
			int sizeCurrSpatial = this.monApps.get(a).spatialRequirements.get(b).size();
			for(int i = 0; i < this.cycles.size(); i++) { //check whether the current cycle contains all items from the current spatial req
				int counterSizeSpatial = 0;
				for(int c = 0; c < this.monApps.get(a).spatialRequirements.get(b).size(); c++) {
					int dev = this.monApps.get(a).deviceToBeCollected.get(b);
					int item = this.monApps.get(a).spatialRequirements.get(b).get(c);
					
					if(this.cycles.get(i).itemPerCycle.contains(new Tuple(dev,item))) {
						counterSizeSpatial++;
					}
				}
				
				if(counterSizeSpatial == sizeCurrSpatial) { //cycle contains all the items of the current spatial req
					//[index 0]: probe id, [index 1]: mon app, [index 2]: spatial req
					whatProbesSatisfied[i][a][b] = true;
				}
				
			}
		}
	}
	

	
	
	
	//print what probes satisfied a given spatial req from a given mon app
	System.out.println("what probes satisfied a given spatial req from a given mon app: " );
	/*for(int a = 0; a < whatProbesSatisfied.length; a++) {
		for(int b = 0; b < whatProbesSatisfied[a].length; b++) {
			for(int c = 0; c < whatProbesSatisfied[a][b].length; c++) {
				System.out.println("whatProbesSatisfied["+a+"]["+b+"]["+c+"] = " + whatProbesSatisfied[a][b][c]);
			}
		}
	}*/
	
	//collected:           boolean[0]: mon app id, boolean[1]: dev id (spatial req id), boolean[2]: item position
	//whatProbesSatisfied: boolean[0]: probe id,   boolean[1]: mon app,                 boolean[2]: spatial req
	
	//until now... items may be reallocated into new probes... However, the copied items must be removed from older probe cycles
	//for(int c = 0; c < numMaxSpatialDependencies; c++) { //NOTE: actually, generatingMonApps is not respecting this variable correctly
	for(int c = 0; c < numMaxSpatialDependencies-1; c++) {
		for(int b = 0; b < this.monApps.size(); b++) {
			int counterProbes = 0;
			
			for(int a = 0; a < this.cycles.size(); a++) { //iterate probes
				System.out.println("whatProbesSatisfied["+a+"]["+b+"]["+c+"] = " + whatProbesSatisfied[a][b][c]);
				if(whatProbesSatisfied[a][b][c]) {
					counterProbes++;
				}
			}
			
			//if(counterProbes == 1) { //special case - there is only one probe collecting this spatial req
			//get the one where it is not true
			for(int a = 0; a < this.cycles.size(); a++) { //iterate all other probes, except the ones collecting the spatial req completely
				if(!whatProbesSatisfied[a][b][c]) {
					
					//count the frequency of devices (trying to find another spatial req)
					//countDevs says: there are N items in this cycle that need this device
					Map<Integer,Integer> countDevs = new HashMap<Integer,Integer>();
					int whichDevice = this.monApps.get(b).deviceToBeCollected.get(c);
					for(int x = 0; x < this.cycles.get(a).itemPerCycle.size(); x++) {
						Integer count = countDevs.get(this.cycles.get(a).itemPerCycle.get(x).getFirst());
						if(count == null && this.cycles.get(a).itemPerCycle.get(x).getFirst() == whichDevice) {
							countDevs.put(this.cycles.get(a).itemPerCycle.get(x).getFirst(), 1);
						}
						else if(this.cycles.get(a).itemPerCycle.get(x).getFirst() == whichDevice) {
							countDevs.put(this.cycles.get(a).itemPerCycle.get(x).getFirst(), count + 1);
						}
					}
					
					//print countDevs
					System.out.println("countDevs hashmap: " + countDevs);
					
					
					//check whether another cycle needs to maintain these items (using 'countDevs' info), otherwise delete all of them
					if(!countDevs.isEmpty()) {
						int countItems = countDevs.get(whichDevice);
						
						//get the smallest size of spatial req
						int minSizeSpatialReq = Integer.MAX_VALUE;
						for(int aa = 0; aa < this.monApps.size(); aa++) {
							for(int bb = 0; bb < this.monApps.get(aa).spatialRequirements.size(); bb++) {
								if(this.monApps.get(aa).spatialRequirements.get(bb).size() < minSizeSpatialReq) {
									minSizeSpatialReq = this.monApps.get(aa).spatialRequirements.get(bb).size();
								}
							}
						}
						
						//if the number of items with the same device is smaller than the smallest existing spatial req, 
						//then it is partially satisfying it and must be removed
						if(countItems < minSizeSpatialReq) {
							for(int x = this.cycles.get(a).itemPerCycle.size() - 1; x >= 0 ; --x) {
								int dev = this.cycles.get(a).itemPerCycle.get(x).getFirst();
								
								if(dev == whichDevice) {
									int item = this.cycles.get(a).itemPerCycle.get(x).getSecond();
									this.cycles.get(a).itemPerCycle.remove(x);
									this.cycles.get(a).capacity_used += infra.sizeTelemetryItems[item];
								}
								
							}
						}
					}	
				}
				
			}
				
			//if there are more than a single probe satisfying the same spatial req and check if they rest of them is oversatisfying it (twice or more)
			if(counterProbes > 1) {
				
				ArrayList<Integer> allProbesIdsCollecting = new ArrayList<Integer>(); //all probes IDs collecting the same spatial req
				for(int a = 0; a < this.cycles.size(); a++) {
					if(whatProbesSatisfied[a][b][c]) {
						allProbesIdsCollecting.add(a);
					}
				}
				
				//there must be only one cycle collecting the same spatial req
				while(allProbesIdsCollecting.size() > 1) {
					
					int idx0 = allProbesIdsCollecting.get(0); //first probe index
					int idx1 = allProbesIdsCollecting.get(1); //second probe index
					int countItemsIdx0 = 0; //count how many items it satisfies
					int countItemsIdx1 = 0; //count how many items it satisfies
					int devSpatialReq = this.monApps.get(b).deviceToBeCollected.get(c);
					
					
					for(int x = 0; x < this.cycles.get(idx0).itemPerCycle.size(); x++) {
						int dev0 = this.cycles.get(idx0).itemPerCycle.get(x).getFirst();
						if(dev0 == devSpatialReq) {
							countItemsIdx0++;
						}
					}
					
					for(int y = 0; y < this.cycles.get(idx1).itemPerCycle.size(); y++) {
						int dev1 = this.cycles.get(idx1).itemPerCycle.get(y).getFirst();						
						if(dev1 == devSpatialReq) {
							countItemsIdx1++;
						}	
					}
					
					System.out.println("countItemsIdx0: " + countItemsIdx0 + ", countItemsIdx1: " + countItemsIdx1);
					
					//if the first probe (higher capacity used) has all items and/or more items from the same device, delete the second cycle
					if(countItemsIdx0 >= countItemsIdx1) { 
						for(int y = this.cycles.get(idx1).itemPerCycle.size() - 1; y >= 0; --y) {
							int dev1 = this.cycles.get(idx1).itemPerCycle.get(y).getFirst();
							if(dev1 == devSpatialReq) {
								int item1 = this.cycles.get(idx1).itemPerCycle.get(y).getSecond();
								this.cycles.get(idx1).capacity_used += infra.sizeTelemetryItems[item1];
								this.cycles.get(idx1).itemPerCycle.remove(y);
							}
						}
						allProbesIdsCollecting.remove(1);
					}else { //countItemsIdx0 < countItemsIdx1 //
						//if the second probe (higher capacity used) has all items and more items from the same device, delete the first cycle
						for(int x = this.cycles.get(idx0).itemPerCycle.size() - 1; x >= 0; --x) {
							int dev0 = this.cycles.get(idx0).itemPerCycle.get(x).getFirst();
							if(dev0 == devSpatialReq) {
								int item0 = this.cycles.get(idx1).itemPerCycle.get(x).getSecond();
								this.cycles.get(idx0).capacity_used += infra.sizeTelemetryItems[item0];
								this.cycles.get(idx0).itemPerCycle.remove(x);
							}
						}
						allProbesIdsCollecting.remove(0);
					}
				}
				
			}
			
			
				
		}
	}
	
	
	//remove empty cycles - if any
	for(int i = this.cycles.size() - 1; i >= 0; --i) {
		if(this.cycles.get(i).itemPerCycle.isEmpty()) {
			this.cycles.remove(i);
		}
	}
	
	
	//remove everything already collected
	for(int i = this.monApps.size() - 1; i >= 0; --i) {
		for(int j = this.monApps.get(i).spatialRequirements.size() - 1; j >= 0; --j) {
			for(int k = this.monApps.get(i).spatialRequirements.get(j).size() - 1; k >= 0; --k) {
				if(collected[i][j][k]) {
					this.monApps.get(i).spatialRequirements.get(j).remove(k);
				}
			}
			if(this.monApps.get(i).spatialRequirements.get(j).isEmpty()) {
				this.monApps.get(i).spatialRequirements.remove(j);
				this.monApps.get(i).deviceToBeCollected.remove(j);
			}
		}
		if(this.monApps.get(i).spatialRequirements.isEmpty()) {
			this.monApps.remove(i);
		}
	}

	
	//Create new probe paths if necessary 
	//TIP: Comment the for-group above to simulate a scenario where there are remaining items to be collected
	
	//NOTE: MUST GUARANTEE ALL SPATIAL REQUIREMENT ITEMS ARE BEEN COLLECTED BY THE SAME PROBE IN THIS METHOD AND OPTIMIZATION METHOD
	
	//this method return a arraylist of cycles. These cycles are added to the existing solution
	/*ArrayList<Cycle> tempCycles = new ArrayList<Cycle>();
	tempCycles = firstApproach(this.monApps, this.capacityProbe);
	for(Cycle c: tempCycles) {
		this.cycles.add(c);
	}*/
	

	//post processing
	System.out.println("cycles size (after dynMon): " + this.cycles.size());
	for(int i = 0; i < this.cycles.size(); i++) {
		this.cycles.get(i).printCycleWithCapacity();	
	}
	
	
}



}