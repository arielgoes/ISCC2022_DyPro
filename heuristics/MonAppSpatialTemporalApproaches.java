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
	

//this approach prioritizes to select the next node to be a "device to be collected". If none, it randomly chooses a next neighbour node to create a cycle
public void firstApproach(ArrayList<MonitoringApp> monitoringApps, int capacityProbe, boolean firstIterProbeId) throws CloneNotSupportedException {
	//ArrayList<Cycle> cycles_sol = new ArrayList<Cycle>();
	this.monApps = monitoringApps;
	int fixedEdgeCost = 1;
	boolean flagReturnDepot = false;
	int probeId = 0;
	
	//pre-checking: updating probe id
	if(!firstIterProbeId) {
		//get highest existing probeID and increment it by 1
		int highest = Integer.MIN_VALUE;
		for(Cycle c : this.cycles) {
			if(c.cycle_id > highest) {
				highest = c.cycle_id;
			}
		}
		probeId = highest + 1;
	}
	
	//pre-checking: feasibility: compare probe capacity against the most consuming spatial req 
	for(int a = 0; a < this.monApps.size(); a++) {
		for(int b = 0; b < this.monApps.get(a).spatialRequirements.size(); b++) {
			int weightSum = 0;
			for(int c = 0; c < this.monApps.get(a).spatialRequirements.get(b).size(); c++) {
				int item = this.monApps.get(a).spatialRequirements.get(b).get(c);
				weightSum += infra.sizeTelemetryItems[item];
			}
			if(weightSum > this.capacityProbe - 2) {
				System.out.println("ERROR: PROBE CAPACITY IS TOO SMALL TO PROCEED! EXITING HEURISTIC!!");
				System.exit(1);
			}
		}
	}
	
	
	

	//creating cycles...
	while(!this.monApps.isEmpty()) {
		//System.out.println("--------------------NEW CYCLE--------------------");
		//System.out.println("mon app size: " + this.monApps.size());
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
		
		currNode = depot; // starting point
		
		while(monApps.size() > 0) {
			int i = 0;
			
			//System.out.println("monApps: " + this.monApps);
			
			//this loop guarantees the current tuples contain at least once the current node to be satisfied
			boolean foundIt = false;
			for(i = 0; i < monApps.size(); i++) {
				//System.out.println("i (in): " + i + ", currNode: " + currNode);
				if(monApps.get(i).deviceToBeCollected.contains(currNode)) {
					foundIt = true;
					break;
				}
			}
			
			if(foundIt) {
				//current tuples
				//System.out.println("curr tuples........................: " + monApps.get(i).spatialRequirements);
				//System.out.println("curr devices.......................: " + monApps.get(i).deviceToBeCollected);
				//System.out.println("currNode...........................: " + currNode);
				while(monApps.get(i).spatialRequirements.size() > 0) { 
					int y = 0;
					y = monApps.get(i).deviceToBeCollected.indexOf(currNode); //get the index of the spatial req pointed by the 'currNode'/device position
					if(y < 0) {
						break;
					}
					
					//sum spatial items' weight
					int weightSum = 0;
					
					for(int x = 0; x < this.monApps.get(i).spatialRequirements.get(y).size(); x++) {
						int item = this.monApps.get(i).spatialRequirements.get(y).get(x);
						weightSum += infra.sizeTelemetryItems[item]; 
					}
					
					
					//check probe capacity availability
					if(c.capacity_used + weightSum < c.capacity-1) {
						c.nodes.add(currNode);
						for(int x = 0; x < this.monApps.get(i).spatialRequirements.get(y).size(); x++) {
							int dev = this.monApps.get(i).deviceToBeCollected.get(y);
							int item = this.monApps.get(i).spatialRequirements.get(y).get(x);
							if(!c.itemPerCycle.contains(new Tuple(dev,item))) {
								c.itemPerCycle.add(new Tuple(dev,item));
								c.capacity_used += infra.sizeTelemetryItems[item];	
							}
							
						}
						//remove satisfied tuple
						this.monApps.get(i).spatialRequirements.remove(y);
						this.monApps.get(i).deviceToBeCollected.remove(y);
					}
					
					

					//check for other spatial requirements that could be simultaneously satisfied by the same device
					for(int aa = this.monApps.size()-1; aa >= 0; aa--) {
						for(int bb = this.monApps.get(aa).spatialRequirements.size()-1; bb >= 0; bb--) {
							int countItems = 0;
							int sizeSpatialItems = this.monApps.get(aa).spatialRequirements.get(bb).size();
							weightSum = 0;
							
							for(int cc = this.monApps.get(aa).spatialRequirements.get(bb).size()-1; cc >= 0; cc--) {
								int dev = this.monApps.get(aa).deviceToBeCollected.get(bb);
								if(c.nodes.contains(dev)) {
									int item = this.monApps.get(aa).spatialRequirements.get(bb).get(cc);
									if(c.itemPerCycle.contains(new Tuple(dev,item))){
										countItems++;
										weightSum += infra.sizeTelemetryItems[item];
									}
								}
							}
							
							if(countItems == sizeSpatialItems && weightSum < c.capacity_used-1) {
								c.capacity_used += weightSum;
								this.monApps.get(aa).spatialRequirements.remove(bb);
								this.monApps.get(aa).deviceToBeCollected.remove(bb);
							}
						}
					}	
						
					
					
					//create link
					//System.out.println("cap used: " + c.capacity_used + ", cap total: " + c.capacity);
					if(c.capacity_used < c.capacity) {
						//find a new node (create a link)
						link = generateLink(depot, currNode, lastNode, fixedEdgeCost, visited, monApps.get(i).deviceToBeCollected, c);
						//System.out.println("link: " + link);
						
						if(link.second == -99) { //no more edges to visit. So, return to the depot
							flagReturnDepot = true;
							//System.out.println("link.second == -99");
						}else {
							lastNode = link.first;
							currNode = link.second;
							visited[link.first][link.second] = true;
							c.links.add(link);
						}
						
						if(flagReturnDepot) {
							//System.out.println("here");
							break;
						}
					}else {
						//System.out.println("no capacity!!!");
						break;
					}
				}
				
				//System.out.println("here now");
				
				if(this.monApps.get(i).spatialRequirements.size() == 0) {
					//System.out.println("spatial size: " + this.monApps.get(i).spatialRequirements);
					monApps.remove(i);
				}else {
					break;
				}
				
				
			}else {
				//System.out.println("Does not have the currNode: " + currNode);
				break;
			}	
		}
	
		//Returning to DEPOT...
		
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
				if(node1 != depot || node2 != depot) {
					c.capacity_used += fixedEdgeCost;	
				}
				
			}
		}
		
		//add cycle to the list of cycles
		c.cycle_id = probeId;
		this.cycles.add(c);
		probeId++;
		this.cycles = addNodesToCycle(this.cycles);
		//c.printCycleWithCapacity();
	}
	
	Collections.sort(this.cycles, Cycle.CycleCapacityUsedDescending);	
	/*System.out.println("First Approach, cycles size: " + this.cycles.size());
	for(int i = 0; i < this.cycles.size(); i++) { 
		this.cycles.get(i).printCycleWithCapacity();
	}*/

}


public Pair<Integer,Integer> generateLink(int depot, int node1, int lastNode, int fixedEdgeCost,
		boolean[][] visited, ArrayList<Integer> deviceToBeCollected, Cycle c) {
		
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
			c.capacity_used += fixedEdgeCost;
			//System.out.println("edge2: " + edge);
			return edge;
		}else if(!neighboursList.isEmpty()) {
			edge.second = neighboursList.get(rnd.nextInt(neighboursList.size()));
			c.capacity_used += fixedEdgeCost;
			visited[node1][edge.second] = true;
			//System.out.println("edge3: " + edge);
			return edge;
		}
		
		// if pair is (node, -99) at the end, it means it completed a circuit and there is no remaining edge to the current node,...
		// ... so, one have to choose how to treat this issue.
		//System.out.println("edge4: " + edge);
		return edge;
}



//Try to insert remaining items into existing probes, on an attempt to avoid the generation of more probes -- i.e., by calling an heuristic method
public void insertRemainingItemsOptimizer(boolean isUsingdynMonApp, boolean[][][] collected) {
	//System.out.println("-----------------------SIMPLE OPTIMIZER-----------------------");
	
	//remove useless cycles - i.e., not collecting items
	for(int i = this.cycles.size() - 1; i >= 0 ; --i) {
		if(this.cycles.get(i).itemPerCycle.isEmpty()) {
			this.cycles.remove(i);
		}
	}
	
	if(!this.monApps.isEmpty()) {		
		for(int a = 0; a < this.monApps.size(); a++) {
			for(int b = this.monApps.get(a).spatialRequirements.size() - 1; b >= 0; --b) {
				
				ArrayList<Tuple> devItems = new ArrayList<Tuple>();
				int weightSum = 0;
				int dev = this.monApps.get(a).deviceToBeCollected.get(b);
				for(int c = this.monApps.get(a).spatialRequirements.get(b).size() - 1; c >= 0; --c) {
					if(this.monApps.get(a).spatialRequirements.get(b).get(c) != -1) {
						int item = this.monApps.get(a).spatialRequirements.get(b).get(c);
						devItems.add(new Tuple(dev,item));
						weightSum += infra.sizeTelemetryItems[item];	
					}
				}
				//System.out.println("devItems: " + devItems);
				
				for(int i = this.cycles.size() - 1; i >= 0; --i) {
					if(this.cycles.get(i).nodes.contains(dev) && this.cycles.get(i).capacity_used + weightSum <= this.cycles.get(i).capacity) {
						for(Tuple devItem : devItems) {
							if(!this.cycles.get(i).itemPerCycle.contains(devItem)) {
								this.cycles.get(i).itemPerCycle.add(devItem);
								this.cycles.get(i).capacity_used += infra.sizeTelemetryItems[devItem.getSecond()];
							}
						}
						
						if(isUsingdynMonApp) {
							for(int c = this.monApps.get(a).spatialRequirements.get(b).size() - 1; c >= 0; --c) {
								if(!collected[a][b][c]) {
									collected[a][b][c] = true;								
								}
							}
						}
						
						
						//this.monApps.get(a).deviceToBeCollected.remove(b);
						
						break;
					}
				}
				
				
			}
		}		
	}
	
	//remove useless cycles - i.e., not collecting items
	for(int i = this.cycles.size() - 1; i >= 0 ; --i) {
		if(this.cycles.get(i).itemPerCycle.isEmpty()) {
			this.cycles.remove(i);
		}
	}
}




public void secondApproach(ArrayList<MonitoringApp> monitoringApps, int capacityProbe, boolean firstIterProbeId) throws CloneNotSupportedException {
	//ArrayList<Cycle> cycles_sol = new ArrayList<Cycle>();
	this.monApps = monitoringApps;
	int fixedEdgeCost = 1;
	boolean flagReturnDepot = false;
	int probeId = 0;
	
	//pre-checking: updating probe id
	if(!firstIterProbeId) {
		//get highest existing probeID and increment it by 1
		int highest = Integer.MIN_VALUE;
		for(Cycle c : this.cycles) {
			if(c.cycle_id > highest) {
				highest = c.cycle_id;
			}
		}
		probeId = highest + 1;
	}
	
	//pre-checking: feasibility: compare probe capacity against the most consuming spatial req 
	for(int a = 0; a < this.monApps.size(); a++) {
		for(int b = 0; b < this.monApps.get(a).spatialRequirements.size(); b++) {
			int weightSum = 0;
			for(int c = 0; c < this.monApps.get(a).spatialRequirements.get(b).size(); c++) {
				int item = this.monApps.get(a).spatialRequirements.get(b).get(c);
				weightSum += infra.sizeTelemetryItems[item];
			}
			if(weightSum > this.capacityProbe - 2) {
				System.out.println("ERROR: PROBE CAPACITY IS TOO SMALL TO PROCEED! EXITING HEURISTIC!!");
				System.exit(1);
			}
		}
	}
	
	
	

	//creating cycles...
	while(!this.monApps.isEmpty()) {
		//System.out.println("--------------------NEW CYCLE--------------------");
		//System.out.println("mon app size: " + this.monApps.size());
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
		
		currNode = depot; // starting point
		
		while(monApps.size() > 0) {
			int i = 0;
			
			//this loop guarantees the current tuples contain at least once the current node to be satisfied
			boolean foundIt = false;
			for(i = 0; i < monApps.size(); i++) {
				//System.out.println("i (in): " + i + ", currNode: " + currNode);
				if(monApps.get(i).deviceToBeCollected.contains(currNode)) {
					foundIt = true;
					break;
				}
			}
			
			if(foundIt) {
				//current tuples
				//System.out.println("curr tuples........................: " + monApps.get(i).spatialRequirements);
				//System.out.println("curr devices.......................: " + monApps.get(i).deviceToBeCollected);
				//System.out.println("currNode...........................: " + currNode);
				while(monApps.get(i).spatialRequirements.size() > 0) { 
					int y = 0;
					y = monApps.get(i).deviceToBeCollected.indexOf(currNode); //get the index of the spatial req pointed by the 'currNode'/device position
					if(y < 0) {
						break;
					}
					
					//sum spatial items' weight
					int weightSum = 0;
					
					for(int x = 0; x < this.monApps.get(i).spatialRequirements.get(y).size(); x++) {
						int item = this.monApps.get(i).spatialRequirements.get(y).get(x);
						weightSum += infra.sizeTelemetryItems[item]; 
					}
					
					
					//check probe capacity availability
					if(c.capacity_used + weightSum < c.capacity-1) {
						c.nodes.add(currNode);
						for(int x = 0; x < this.monApps.get(i).spatialRequirements.get(y).size(); x++) {
							int dev = this.monApps.get(i).deviceToBeCollected.get(y);
							int item = this.monApps.get(i).spatialRequirements.get(y).get(x);
							c.itemPerCycle.add(new Tuple(dev,item));
							c.capacity_used += infra.sizeTelemetryItems[item];
						}
						//remove satisfied tuple
						this.monApps.get(i).spatialRequirements.remove(y);
						this.monApps.get(i).deviceToBeCollected.remove(y);
					}
					
					//c.printCycleWithCapacity();
					
					
					//create link
					if(c.capacity_used < c.capacity) {
						//find a new node (create a link)
						link = generateLink2(depot, currNode, lastNode, fixedEdgeCost, visited, monApps.get(i).deviceToBeCollected, c);
						//System.out.println("link: " + link);
						
						if(link.second == -99) { //no more edges to visit. So, return to the depot
							flagReturnDepot = true;
							//System.out.println("link.second == -99");
						}else {
							lastNode = link.first;
							currNode = link.second;
							visited[link.first][link.second] = true;
							c.links.add(link);
						}
						
						if(flagReturnDepot) {
							//System.out.println("here");
							break;
						}
						
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
									
								}
							}
						}
						
					}else {
						//System.out.println("no capacity!!!");
						break;
					}
				}
				
				
				if(this.monApps.get(i).spatialRequirements.size() == 0) {
					//System.out.println("spatial size: " + this.monApps.get(i).spatialRequirements);
					monApps.remove(i);
				}else {
					break;
				}
				
				
			}else {
				//System.out.println("Does not have the currNode: " + currNode);
				break;
			}	
		}
	
		//Returning to DEPOT...
		
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
				if(node1 != depot || node2 != depot) {
					c.capacity_used += fixedEdgeCost;	
				}
				
			}
		}
		
		//add cycle to the list of cycles
		c.cycle_id = probeId;
		this.cycles.add(c);
		probeId++;
		this.cycles = addNodesToCycle(this.cycles);
		//c.printCycleWithCapacity();
	}
	
	Collections.sort(this.cycles, Cycle.CycleCapacityUsedDescending);
	/*System.out.println("Second approach, cycles size: " + this.cycles.size());
	for(int i = 0; i < this.cycles.size(); i++) { 
		this.cycles.get(i).printCycleWithCapacity();
	}*/

}



public Pair<Integer,Integer> generateLink2(int depot, int node1, int lastNode, int fixedEdgeCost,
		boolean[][] visited, ArrayList<Integer> deviceToBeCollected, Cycle c) {
	
	Pair<Integer,Integer> edge = Pair.create(node1, -99);
	Random rnd = new Random(this.seed + this.seedChanger);
	this.seedChanger++;
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
			c.capacity_used += fixedEdgeCost;
			//System.out.println("node1: " + node1 + ", edge.second: " + edge.second);
			return edge;
		}
	}
	
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


//input: sorted cycles, output: tries to better utilize probe capacity by reducing the number of needed cycles
public void probeUsageOptimizer() {
	
	for(int i = 0; i < this.cycles.size(); i++) {
		for(int j = this.cycles.size() - 1; j > i; j--) {
			if(i != j) {
				//iterate 'j' nodes and find out if both have it
				ArrayList<Integer> foundDevices = new ArrayList<>();
				for(Integer node : this.cycles.get(j).nodes) {					
					if(this.cycles.get(i).nodes.contains(node) && !foundDevices.contains(node)) {
						foundDevices.add(node);
					}
				}
				
				while(!foundDevices.isEmpty()) {
					int currFoundNode = foundDevices.get(0);
					int weightSum = 0;
					
					//weight the sum of the items from this device. It is an all-or-nothing approach because it is too expensive too check all spatial reqs
					ArrayList<Tuple> devItems = new ArrayList<>();
					for(int j_items = 0; j_items < this.cycles.get(j).itemPerCycle.size(); j_items++) {
						int dev = this.cycles.get(j).itemPerCycle.get(j_items).getFirst();
						int item = this.cycles.get(j).itemPerCycle.get(j_items).getSecond();
						if(dev == currFoundNode) {
							weightSum += infra.sizeTelemetryItems[item];
							devItems.add(new Tuple(dev,item));
						}
					}
					
					
					
					if(this.cycles.get(i).capacity_used + weightSum <= this.cycles.get(i).capacity) {
						while(!devItems.isEmpty()) {
							Tuple currDevItem = devItems.get(0);
							if(!this.cycles.get(i).itemPerCycle.contains(currDevItem)) {
								//add to new probe
								this.cycles.get(i).itemPerCycle.add(currDevItem);
								this.cycles.get(i).capacity_used += infra.sizeTelemetryItems[currDevItem.getSecond()];
								
								//remove it from the previous one
								int indexDevItem = this.cycles.get(j).itemPerCycle.indexOf(currDevItem);
								this.cycles.get(j).itemPerCycle.remove(indexDevItem);
							}else {
								//remove it from the previous one
								int indexDevItem = this.cycles.get(j).itemPerCycle.indexOf(currDevItem);
								this.cycles.get(j).itemPerCycle.remove(indexDevItem);
							}
							
							devItems.remove(0);
						}
					}
					foundDevices.remove(0);
				}
				
				if(this.cycles.get(j).itemPerCycle.isEmpty()) {
					this.cycles.remove(j);
				}
			}
		}
	}
}




public void dynamicProbeGenerator(ArrayList<MonitoringApp> newMonApps, int numMaxSpatialDependencies, int maxSizeSpatialDependency) throws CloneNotSupportedException {
	
	this.monApps = newMonApps;
	
	//int[0]: mon app id, int[1]: dev id (spatial req id), int[2]: item position
	boolean[][][] collected = new boolean[this.monApps.size()][numMaxSpatialDependencies-1][maxSizeSpatialDependency]; 
	
	//Check what spatial requirements are already satisfied by the previous probe scheme
	for(int i = this.cycles.size() - 1; i >= 0; --i){
		for(int j = this.cycles.get(i).itemPerCycle.size() - 1; j >= 0; --j) {			
			for(int a = 0; a < this.monApps.size(); a++) {
				for(int b = 0; b < this.monApps.get(a).spatialRequirements.size(); b++) {
					for(int c = 0; c < this.monApps.get(a).spatialRequirements.get(b).size(); c++) {							
						//System.out.println("a: " + a + ", b: " + b + ", c: " + c);
						int dev = this.monApps.get(a).deviceToBeCollected.get(b);
						int item = this.monApps.get(a).spatialRequirements.get(b).get(c);
						if(this.cycles.get(i).itemPerCycle.contains(new Tuple(dev,item))) {
							//System.out.println("devItem: " + devItem);
							//System.out.println("a: " + a + ", b: " + b + ", c: " + c);
							collected[a][b][c] = true; //if the items are found in the cycle, mark it as already satisfied
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

	
	//print collected
	/*for(int a = 0; a < collected.length; a++) {
		for(int b = 0; b < collected[a].length; b++) {
			for(int c = 0; c < collected[a][b].length; c++) {
				System.out.println("collected["+a+"]["+b+"]["+c+"] = " + collected[a][b][c]);
			}
		}
	}*/
	
	//remove empty cycles - if any
	for(int i = this.cycles.size() - 1; i >= 0; --i) {
		if(this.cycles.get(i).itemPerCycle.isEmpty()) {
			this.cycles.remove(i);
		}
	}
	
	
	/*System.out.println("before check spatial req");
	for(Cycle c : this.cycles) {
		c.printCycleWithCapacity();
	}*/
	
	
	//check spatial constraints - i.e., spatial requirement items must be collected by the same probe
	for(int i = this.cycles.size() - 1; i >= 0; i--) {
		//System.out.println("CYCLE: " + i + ":):):):):):):):):):):):):):):):):):):):):):):):):):):)");
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
						aux[0] = i; //this.cycles.get(i).cycle_id;
						aux[1] = dev;
						aux[2] = item;
						auxCurrSpatialItems.add(aux);
					}else if(!this.cycles.get(i).itemPerCycle.contains(devItem) && collected[a][b][c]) { //AT LEAST another cycle collected the item			
						for(int x = 0; x < this.cycles.size(); x++) {
							if(x != i) {
								if(this.cycles.get(x).itemPerCycle.contains(devItem) && collected[a][b][c]) {
									int[] aux = new int[3];
									aux[0] = x; //this.cycles.get(x).cycle_id;
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
				
				int currProbeId = i; //this.cycles.get(i).cycle_id;
				
				boolean probeDiff = false; //if probe IDs are different, reallocate items
				for(int x = 0; x < auxCurrSpatialItems.size(); x++) {
					if(currProbeId != auxCurrSpatialItems.get(x)[0]) { //if at least one item is not collected, then I can't remove the spatial requirement
						//System.out.println("probeId: " + probeId + ", probeId2: " + auxCurrSpatialItems.get(x)[0]);
						probeDiff = true;
						break;
					}
				}
				
				//if all items are in the same probe (valid probe -- i.e., not '-1'), delete the spatial requirement and move on to the next spatial req
				if(!probeDiff) {
					for(int c = monApps.get(a).spatialRequirements.get(b).size() - 1; c >= 0; --c) {
						collected[a][b][c] = true;
					}
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
					
					
					
					//System.out.println("keyCollectingMost: " + keyCollectingMost);
		
					//if 'keyCollectingMost' is still '-1', all items from curr spatial req are not collected yet
					if(keyCollectingMost == -1) {
						
						//estimate what would cost to insert the whole unsatisfied spatial req into a cycle
						int cost = 0;
						for(int x = 0; x < auxCurrSpatialItems.size(); x++) {
							int item = auxCurrSpatialItems.get(x)[2];
							cost += infra.sizeTelemetryItems[item];
						}
						
						boolean hasAllDevices = true;
						boolean allFit = false;
						
						//find a cycle to fit all of them
						for(int x = 0; x < this.cycles.size(); x++) {
							hasAllDevices = true;
							
							//pre-checking: if the new cycle has all the required devices to fit all the items										
							for(int y = 0; y < auxCurrSpatialItems.size(); y++) {
								int dev = auxCurrSpatialItems.get(y)[1];
								//System.out.println("dev: " + dev);
								
								if(!this.cycles.get(x).nodes.contains(dev)) {
									//System.out.println("dev: " + dev + ", cycle id: " + this.cycles.get(x).cycle_id);
									hasAllDevices = false;
									break;
								}
							}
							
							if(!hasAllDevices) {
								continue;
							}else if(this.cycles.get(x).capacity_used + cost <= this.cycles.get(x).capacity) {
								//System.out.println("cycle nodes: " + this.cycles.get(x).nodes);
								//System.out.println("cap used: " + this.cycles.get(x).capacity_used + " total capacity: " + this.cycles.get(x).capacity);
								//System.out.println("cost: " + cost);
								allFit = true;
								for(int y = 0; y < auxCurrSpatialItems.size(); y++) {
									int dev = auxCurrSpatialItems.get(y)[1];
									int item = auxCurrSpatialItems.get(y)[2];
									Tuple devItem = new Tuple(dev,item);
									if(!this.cycles.get(x).itemPerCycle.contains(devItem)) {
										this.cycles.get(x).itemPerCycle.add(devItem);
										this.cycles.get(x).capacity_used += infra.sizeTelemetryItems[item];	
									}
									
								}
								for(int c = monApps.get(a).spatialRequirements.get(b).size() - 1; c >= 0; --c) {
									collected[a][b][c] = true;
								}
								
							}
							if(allFit) { //sucessfully reallocated all items... go to the next spatial req
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
						
						
						//check if the key collecting most has all devices
						boolean hasAllDevices = true;
						
						//pre-checking: if the new cycle has all the required devices to fit all the items										
						for(int y = 0; y < auxCurrSpatialItems.size(); y++) {
							int dev = auxCurrSpatialItems.get(y)[1];
							//System.out.println("dev: " + dev);
							
							if(!this.cycles.get(keyCollectingMost).nodes.contains(dev)) {
								//System.out.println("dev: " + dev + ", cycle id: " + this.cycles.get(x).cycle_id);
								hasAllDevices = false;
								break;
							}
						}
						
						
						//try to fit the remaining items into the probe collecting most of the items
						if(this.cycles.get(keyCollectingMost).capacity_used + costOtherItems <= this.cycles.get(keyCollectingMost).capacity &&
								hasAllDevices) {
							for(int y = 0; y < auxCurrSpatialItems.size(); y++) {
								int dev = auxCurrSpatialItems.get(y)[1];
								int item = auxCurrSpatialItems.get(y)[2];
								Tuple devItem = new Tuple(dev,item);
								if(!this.cycles.get(keyCollectingMost).itemPerCycle.contains(devItem)) {
									this.cycles.get(keyCollectingMost).itemPerCycle.add(devItem);
									this.cycles.get(keyCollectingMost).capacity_used += infra.sizeTelemetryItems[item];	
								}
								
								if(auxCurrSpatialItems.get(y)[0] != keyCollectingMost && auxCurrSpatialItems.get(y)[0] != -1) {
									this.cycles.get(auxCurrSpatialItems.get(y)[0]).itemPerCycle.remove(devItem);
									this.cycles.get(auxCurrSpatialItems.get(y)[0]).capacity_used -= infra.sizeTelemetryItems[item];
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
								hasAllDevices = true; //tells whether the new cycle contains all devices to collect all items
								
								for(int x = 0; x < this.cycles.size(); x++) {
									if(x != keyCollectingMost) {
										allFit = false; //tells whether the new cycle has available space to collected all items
										hasAllDevices = true; //tells whether the new cycle contains all devices to collect all items
										
										//pre-checking: if the new cycle has all the required devices to fit all the items										
										for(int y = 0; y < auxCurrSpatialItems.size(); y++) {
											Integer dev = auxCurrSpatialItems.get(y)[1];
											if(!this.cycles.get(x).nodes.contains(dev)) {
												hasAllDevices = false;
												break;
											}
										}
										
										if(!hasAllDevices) {
											break; //stop trying for once and go to the next spatial requirement
										}else if(this.cycles.get(x).capacity_used + costAllItems <= this.cycles.get(keyCollectingMost).capacity) {
											allFit = true;
											for(int y = 0; y < auxCurrSpatialItems.size(); y++) {
												int dev = auxCurrSpatialItems.get(y)[1];
												int item = auxCurrSpatialItems.get(y)[2];
												Tuple devItem = new Tuple(dev,item);
												
												if(!this.cycles.get(x).itemPerCycle.contains(devItem)) {
													this.cycles.get(x).itemPerCycle.add(devItem);
													this.cycles.get(x).capacity_used += infra.sizeTelemetryItems[item];	
												}
												
											}
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
						}
					
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
	

	//Collections.sort(this.cycles, Cycle.CycleCapacityUsedDescending);
	
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
	
	
	//collected:           boolean[0]: mon app id, boolean[1]: dev id (spatial req id), boolean[2]: item position
	//whatProbesSatisfied: boolean[0]: probe id,   boolean[1]: mon app,                 boolean[2]: spatial req
	
	//until now... items may be reallocated into new probes... However, the copied items must be removed from older probe cycles
	//for(int c = 0; c < numMaxSpatialDependencies; c++) { //NOTE: actually, generatingMonApps is not respecting this variable correctly
	for(int c = 0; c < numMaxSpatialDependencies-1; c++) {
		for(int b = 0; b < this.monApps.size(); b++) {
			int counterProbes = 0;
			
			for(int a = 0; a < this.cycles.size(); a++) { //iterate probes
				
				if(whatProbesSatisfied[a][b][c]) {
					//System.out.println("whatProbesSatisfied["+a+"]["+b+"]["+c+"] = " + whatProbesSatisfied[a][b][c]);
					counterProbes++;
				}
			}
			
			//get the one where it is not true
			for(int a = 0; a < this.cycles.size(); a++) { //iterate all probes that may be partially satisfying spatial reqs and remove these items from them
				//count the frequency of devices (trying to find another spatial req)
				//countDevs says: there are N items in this cycle associated with this device
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
				//System.out.println("countDevs hashmap: " + countDevs + ", whichDevice: " + whichDevice + " whatProbesSatisfied["+a+"]["+b+"]["+c+"] = " + whatProbesSatisfied[a][b][c]);
				
				
				//check whether another cycle needs to maintain these items (using 'countDevs' info), otherwise delete all of them
				if(!countDevs.isEmpty()) {
					//System.out.println("asdasd");
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
					
					//if the number of items within the same device is smaller than the smallest existing spatial req, 
					//then it is partially satisfying it and must be removed
					if(countItems < minSizeSpatialReq) {
						for(int x = this.cycles.get(a).itemPerCycle.size() - 1; x >= 0 ; --x) {
							int dev = this.cycles.get(a).itemPerCycle.get(x).getFirst();
							
							if(dev == whichDevice) {
								int item = this.cycles.get(a).itemPerCycle.get(x).getSecond();
								//System.out.println("(removing) devItem: (" + dev + "," + item + ")");
								this.cycles.get(a).itemPerCycle.remove(x);
								this.cycles.get(a).capacity_used -= infra.sizeTelemetryItems[item];
							}
							
						}
					}else { //if it is a valid size ( >= minSizeSpatialReq) but there is not a spatial req with size >= this one... it is partially collected 
						
						//iterate all mon apps that could have collected those items
						//e.g., a cycle contains: (6,4), (6,5), minSizeSpatialReq == 2, but there is only a spatial req: (6,4),(6,5),(6,6)...
						//in other words... there is no way the cycle is satisfying a valid spatail req condition
						int minSizeSpatialReqDevice = Integer.MAX_VALUE;
						for(int aa = 0; aa < this.monApps.size(); aa++) {
							for(int bb = 0; bb < this.monApps.get(aa).spatialRequirements.size(); bb++) {
								if(this.monApps.get(aa).deviceToBeCollected.get(bb) == whichDevice) {
									if(this.monApps.get(aa).spatialRequirements.get(bb).size() < minSizeSpatialReqDevice) {
										minSizeSpatialReqDevice = this.monApps.get(aa).spatialRequirements.get(bb).size();
									}
								}
							}
						}
						
						if(countItems < minSizeSpatialReqDevice) {
							for(int x = this.cycles.get(a).itemPerCycle.size() - 1; x >= 0 ; --x) {
								int dev = this.cycles.get(a).itemPerCycle.get(x).getFirst();
								
								if(dev == whichDevice) {
									int item = this.cycles.get(a).itemPerCycle.get(x).getSecond();
									//System.out.println("cycle: " + a + ", devItem: " + new Tuple(dev,item));
									this.cycles.get(a).itemPerCycle.remove(x);
									this.cycles.get(a).capacity_used -= infra.sizeTelemetryItems[item];
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
						//System.out.println("(counterProbes > 1): whatProbesSatisfied["+a+"]["+b+"]["+c+"] = " + whatProbesSatisfied[a][b][c] + "spatial req: " + this.monApps.get(b).spatialRequirements.get(c));
						allProbesIdsCollecting.add(a);
					}
				}
				
				
				int devSpatialReq = this.monApps.get(b).deviceToBeCollected.get(c); //device to look at
				
				//there must be only one cycle collecting the same spatial req
				while(allProbesIdsCollecting.size() > 1) {
					//System.out.println("all probes: " + allProbesIdsCollecting);
					
					int idx0 = allProbesIdsCollecting.get(0); //first probe index
					int idx1 = allProbesIdsCollecting.get(1); //second probe index
					int countItemsIdx0 = 0; //count how many items it satisfies
					int countItemsIdx1 = 0; //count how many items it satisfies
					
					
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
					
					//System.out.println("Idx0..........: " + idx0 + ", Idx1..........: " + idx1);
					//System.out.println("countItemsIdx0: " + countItemsIdx0 + ", countItemsIdx1: " + countItemsIdx1);
					
					//if the first probe (higher capacity used) has all items and/or more items from the same device, delete the second cycle
					if(countItemsIdx0 >= countItemsIdx1) {
						for(int y = this.cycles.get(idx1).itemPerCycle.size() - 1; y >= 0; --y) {
							int dev1 = this.cycles.get(idx1).itemPerCycle.get(y).getFirst();
							
							//System.out.println("dev1: " + dev1 + " devSpatialReq: " + devSpatialReq + " y: " + y + " devItem: " + this.cycles.get(idx1).itemPerCycle.get(y));
							
							if(dev1 == devSpatialReq) {
								//System.out.println("dev1: " + dev1 + " devSpatialReq: " + devSpatialReq + " y: " + y + " devItem: " + this.cycles.get(idx1).itemPerCycle.get(y));
								int item1 = this.cycles.get(idx1).itemPerCycle.get(y).getSecond();
								this.cycles.get(idx1).capacity_used -= infra.sizeTelemetryItems[item1];
								this.cycles.get(idx1).itemPerCycle.remove(y);
							}
						}
						//System.out.println("cycle after");
						//this.cycles.get(idx1).printCycleWithCapacity();
						//System.out.println("allProbesidsCollecting(1): " + allProbesIdsCollecting.get(1));
						whatProbesSatisfied[idx1][b][c] = false;
						allProbesIdsCollecting.remove(1);
					}else { //countItemsIdx0 < countItemsIdx1 //
						//if the second probe (higher capacity used) has all items and more items from the same device, delete the first cycle
						
						for(int x = this.cycles.get(idx0).itemPerCycle.size() - 1; x >= 0; --x) {
							int dev0 = this.cycles.get(idx0).itemPerCycle.get(x).getFirst();
							
							if(dev0 == devSpatialReq) {
								int item0 = this.cycles.get(idx0).itemPerCycle.get(x).getSecond();
								this.cycles.get(idx0).capacity_used -= infra.sizeTelemetryItems[item0];
								this.cycles.get(idx0).itemPerCycle.remove(x);
							}
						}
						whatProbesSatisfied[idx0][b][c] = false;
						allProbesIdsCollecting.remove(0);
					}
					
					counterProbes--;
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
	

	
	whatProbesSatisfied = new boolean[this.cycles.size()][this.monApps.size()][numMaxSpatialDependencies]; //[index 0]: probe id, [index 1]: mon app, [index 2]: spatial req
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
	
	
	
	//update collected
	for(int c = 0; c < numMaxSpatialDependencies-1; c++) {
		for(int b = 0; b < this.monApps.size(); b++) {
		boolean onceAtLeast = false;
		
		for(int a = 0; a < this.cycles.size(); a++) {
			//System.out.println("(before removal) whatProbesSatisfied["+a+"]["+b+"]["+c+"] = " + whatProbesSatisfied[a][b][c] + " / spatial req: " + this.monApps.get(b).spatialRequirements.get(c));
			if(whatProbesSatisfied[a][b][c]) {
				onceAtLeast = true;
				break;
			}
		}
		
		if(!onceAtLeast) {
			for(int d = 0; d < maxSizeSpatialDependency; d++) {
				collected[b][c][d] = false;
			}	
		}else {
			for(int d = 0; d < maxSizeSpatialDependency; d++) {
				collected[b][c][d] = true;
			}
		}
		
			
			
		}
	}
			
	
	//print collected
	/*System.out.println("COLLECTED:");
	for(int a = 0; a < collected.length; a++) {
		for(int b = 0; b < collected[a].length; b++) {
			for(int c = 0; c < collected[a][b].length; c++) {
				System.out.println("collected["+a+"]["+b+"]["+c+"] = " + collected[a][b][c]);
			}
		}
	}*/
	
	
	
	//mark collected items as -1 - in order to keep the correct index access when actually removing them
	for(int i = this.monApps.size() - 1; i >= 0; --i) {
		for(int j = this.monApps.get(i).spatialRequirements.size() - 1; j >= 0; --j) {
			for(int k = this.monApps.get(i).spatialRequirements.get(j).size() - 1; k >= 0; --k) {
				if(collected[i][j][k]) {
					this.monApps.get(i).spatialRequirements.get(j).set(k, -1);
				}
			}
		}
	}
	
	
	//optimization method
	Collections.sort(this.cycles, Cycle.CycleCapacityUsedDescending); //sort probes in descending order by capacity_used
	insertRemainingItemsOptimizer(true, collected); //try to insert the remaining spatial reqs into existing probes
	
	//mark collected items as -1 - in order to keep the correct index access when actually removing them
	for(int i = this.monApps.size() - 1; i >= 0; --i) {
		for(int j = this.monApps.get(i).spatialRequirements.size() - 1; j >= 0; --j) {
			for(int k = this.monApps.get(i).spatialRequirements.get(j).size() - 1; k >= 0; --k) {
				if(collected[i][j][k]) {
					this.monApps.get(i).spatialRequirements.get(j).set(k, -1);
				}
			}
		}
	}
	
	
	//remove everything already collected (items marked as -1)
	for(int i = this.monApps.size() - 1; i >= 0; --i) {
		for(int j = this.monApps.get(i).spatialRequirements.size() - 1; j >= 0; --j) {
			for(int k = this.monApps.get(i).spatialRequirements.get(j).size() - 1; k >= 0; --k) {
				if(this.monApps.get(i).spatialRequirements.get(j).get(k) == -1) {
					//System.out.println("dev-item: " + this.monApps.get(i).deviceToBeCollected.get(j) + "-" + this.monApps.get(i).spatialRequirements.get(j).get(k));
					this.monApps.get(i).spatialRequirements.get(j).remove(k);
				}
			}
			if(this.monApps.get(i).spatialRequirements.get(j).isEmpty()) {
				//System.out.println("i: " + i + " j: " + j);
				this.monApps.get(i).spatialRequirements.remove(j);
				this.monApps.get(i).deviceToBeCollected.remove(j);
			}
		}
		if(this.monApps.get(i).spatialRequirements.isEmpty()) {
			this.monApps.remove(i);
		}
	}

	
	//Create more probes, if needed 	
	firstApproach(this.monApps, this.capacityProbe, false); //this heuristic already sort the cycles at the end
	probeUsageOptimizer();
	
	//System.out.println();
	//System.out.println();
	
	
	//post processing
	/*System.out.println("CYCLES SIZE (AFTER dynMon): " + this.cycles.size());
	for(int i = 0; i < this.cycles.size(); i++) {
		this.cycles.get(i).printCycleWithCapacity();	
	}
	System.out.println("==========");*/
	
	
}




}