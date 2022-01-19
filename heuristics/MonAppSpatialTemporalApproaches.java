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
	public int[][] costShortestPath;
	public NetworkInfrastructure infra;
	long seed;
	public boolean flagCreatePath = false; //either I let it like this, or return a vector [edge.first, edge.second, flagCreatePath]
	public boolean flagEqualDepot = false;
	
	public MonAppSpatialTemporalApproaches(NetworkInfrastructure infra, long seed) {
		this.cycles = new ArrayList<Cycle>();
		this.infra = infra;
		this.costShortestPath = new int[infra.size][infra.size];
		this.seed = seed;
		
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
	}
	
	return clonedMonApps;
}

public ArrayList<Cycle> firstApproach(ArrayList<MonitoringApp> monitoringApps, int capacityProbe, MonitoringApp monApps) throws CloneNotSupportedException {
	ArrayList<Cycle> cycles_sol = new ArrayList<Cycle>();
	//ArrayList<ArrayList<Integer>> auxSpatialRequirements = ArrayList<ArrayList<Integer>> 
	
	ArrayList<MonitoringApp> clonedMonApps = new ArrayList<MonitoringApp>();
	clonedMonApps = cloneMonitoringApps(monitoringApps);
	
	
	
	boolean[][] collectedItems = new boolean[this.infra.size][this.infra.telemetryItemsRouter]; //device, item
	
	int item = -1;
	int fixedEdgeCost = 1;
	int currProbeCapacity = capacityProbe;
	int oldProbeCapacity = capacityProbe;
	int probeId = 0;
	boolean flag = false;
	
	// # of remaining items to be collected
	int remainingItems = 0;
	for(int i = 0; i < clonedMonApps.size(); i++) {
		for(int j = 0; j < clonedMonApps.get(i).spatialRequirements.size(); j++) {
			for(int k = 0; k < clonedMonApps.get(i).spatialRequirements.get(j).size(); k++) {
				remainingItems++;
			}
		}
	}
	
	
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
			System.out.println("DEPOT... " + depot);
		}else {
			break;
		}
		
		Cycle c = new Cycle();
		c.capacity = capacityProbe;
		
		currNode = depot; // starting point]
		
		do {
		
			for(int i = 0; i < clonedMonApps.size(); i++) { 
				
				System.out.println("curr tuples........................: " + clonedMonApps.get(i).spatialRequirements);
				//iterate over all sets of spatial requirements
				for(int k = 0; k < clonedMonApps.get(i).spatialRequirements.size(); k++) { //iterate over mon apps' spatial requirements (item tuples)
					System.out.println("curr dev.................: " + clonedMonApps.get(i).deviceToBeCollected.get(k));
					//System.out.println("probe capacity: " + currProbeCapacity);
					//System.out.println("remainingItems: " + remainingItems);
					//System.out.println("cloned size: " + clonedMonApps.size());
					
					
					int numItemsCurrSpatialDep = clonedMonApps.get(i).spatialRequirements.get(k).size();
					ArrayList<Integer> auxRemoveCurrTuple = new ArrayList<Integer>();
					//System.out.println("numItemsSpatialDep: " + numItemsCurrSpatialDep);
					
					//current tuple 
					for(int l = 0; l < clonedMonApps.get(i).spatialRequirements.get(k).size(); l++) {
						item = clonedMonApps.get(i).spatialRequirements.get(k).get(l);
						//System.out.println("item (out): " + item);
						//add items to probe - if it has enough available space
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
							//clonedMonApps.get(i).spatialRequirements.get(k).remove(l);
						}
					}
					
					//remove collected items
					for(int x = 0; x < auxRemoveCurrTuple.size(); x++) {
						if(clonedMonApps.get(i).spatialRequirements.get(k).contains(auxRemoveCurrTuple.get(x))) {
							clonedMonApps.get(i).spatialRequirements.get(k).remove(auxRemoveCurrTuple.get(x));
						}
					}
					
					//System.out.println("numitemscurrspatialdep: " + numItemsCurrSpatialDep);
					//System.out.println("cloned spatial req (before): " + clonedMonApps.get(i).spatialRequirements);
					//System.out.println("cloned dev req     (before): " + clonedMonApps.get(i).deviceToBeCollected);
					if(numItemsCurrSpatialDep == 0) { // e.g., before: Mon 0: < <2,3>, <4,5> > || after: Mon 0: < <4,5> >
						
						clonedMonApps.get(i).spatialRequirements.remove(k);
						clonedMonApps.get(i).deviceToBeCollected.remove(k);						
					}
					//System.out.println("cloned spatial req (after): " + clonedMonApps.get(i).spatialRequirements);
					//System.out.println("cloned dev req     (after): " + clonedMonApps.get(i).deviceToBeCollected);

					
					//Choose the next node - i.e., create a cycle link 
					link = generateLink(depot, currNode, lastNode, currProbeCapacity, fixedEdgeCost, visited, clonedMonApps.get(i).deviceToBeCollected); 
					
					if(link.second == -99) { //no more edges to visit. So, return to the depot
						flag = true;
					}else {
						lastNode = link.first;
						currNode = link.second;
						c.links.add(link);
					}
					
					
				}
				
				//if all items from the current tuple are satisfied, remove the spatial requirement request (i.e., it is now satisfied)
				/*if(savedIndex != -1) { // OBS.: NAO ESTA REMOVENDO CORRETAMENTE PQ ELE SO REMOVE O ULTIMO AO SAIR DO LOOP
					System.out.println("cloned spatial req (before): " + clonedMonApps.get(i).spatialRequirements);
					clonedMonApps.get(i).spatialRequirements.remove(savedIndex);
					clonedMonApps.get(i).deviceToBeCollected.remove(savedIndex);
					System.out.println("cloned spatial req (after): " + clonedMonApps.get(i).spatialRequirements);
					System.exit(0);
					
				}*/
				
				//System.out.println("cloned spatial req (outer): " + clonedMonApps.get(i).spatialRequirements);
				if(clonedMonApps.get(i).spatialRequirements.size() <= 0) {
					flag = true;
					clonedMonApps.remove(i);
				}
				
				if(flag) {
					break;
				}
				
			}
		
		 System.out.println("clonedMonApps: " + clonedMonApps);
		}while(clonedMonApps.size() > 0 && currProbeCapacity > capacityProbe/2 && !flag); //upforward steps
		//do-while - backwards steps - i.e., connecting the cycle towards the 'depot'
		
		//reset flag
		flag = false;
		
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
			
			for(int k = 0; k < shortPath.size() - 1; k++) {
				int node1 = shortPath.get(k);
				int node2 = shortPath.get(k+1);
				Pair<Integer,Integer> p = Pair.create(node1, node2);
				c.links.add(p); //add to current circuit
				
				//if the edge contains the 'depot' it's already 
				if(node1 != depot && node2 != depot) {
					currProbeCapacity -= fixedEdgeCost;	
				}
				
				
				//TO BE CODED: TRY TO COLLECT ITEMS WHILE RETURNING TO DEPOT
				//...
			}
		}
		
		
		
		
		//add cycle to the list of cycles
		c.cycle_id = probeId;
		cycles_sol.add(c);
		probeId++;
		c.printCycleWithCapacity();
		currProbeCapacity = oldProbeCapacity; //reset probe capacity
	
	}while(clonedMonApps.size() > 0 && remainingItems > 0);
	
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
			break;
		}
	}
	
	//System.out.println("neighbours: " + neighboursList);
	
	//if there is at least one feasible neighbour
	if(!neighboursList.isEmpty() && edge.second != -99) {
		//System.out.println("edge.first (if): " + edge.first);
		//System.out.println("edge.second (if): " + edge.second);
		visited[node1][edge.second] = true;
		currProbeCapacity -= fixedEdgeCost;
		return edge;
	}else if(!neighboursList.isEmpty()) {
		edge.second = neighboursList.get(rnd.nextInt(neighboursList.size()));
		//System.out.println("edge.first (else if): " + edge.first);
		//System.out.println("edge.second (else if): " + edge.second);
		visited[node1][edge.second] = true;
		return edge;
	}
	
	// if pair is (node, -2) at the end, it means it completed a circuit and there is no remaining edge to the current node,...
	// ... so, one have to choose how to treat this issue.
	return edge;
}



public ArrayList<Cycle> secondApproach(ArrayList<MonitoringApp> monitoringApps, int capacityProbe, MonitoringApp monApps) throws CloneNotSupportedException {
	ArrayList<Cycle> cycles_sol = new ArrayList<Cycle>();
	//ArrayList<ArrayList<Integer>> auxSpatialRequirements = ArrayList<ArrayList<Integer>> 
	
	ArrayList<MonitoringApp> clonedMonApps = new ArrayList<MonitoringApp>();
	clonedMonApps = cloneMonitoringApps(monitoringApps);
	
	
	
	boolean[][] collectedItems = new boolean[this.infra.size][this.infra.telemetryItemsRouter]; //device, item
	
	int item = -1;
	int fixedEdgeCost = 1;
	int currProbeCapacity = capacityProbe;
	int oldProbeCapacity = capacityProbe;
	int probeId = 0;
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
	
	
	//System.out.println("<TEMP> items' size");
	/*for(int i = 0; i < infra.telemetryItemsRouter; i++) {
		System.out.println("item " + i + ", weight: " + infra.sizeTelemetryItems[i]);
	}*/
	
	do {
		
		
		//System.out.println("---------------------------------NEW CYCLE-----------------------------------------------------------------");
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
		
			for(int i = 0; i < clonedMonApps.size(); i++) { 
				
				//System.out.println("curr tuples........................: " + clonedMonApps.get(i).spatialRequirements);
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
					//System.out.println("num item curr spatial dep comeccinhgooo: " + numItemsCurrSpatialDep);
					ArrayList<Integer> auxRemoveCurrTuple = new ArrayList<Integer>();
					//System.out.println("numItemsSpatialDep: " + numItemsCurrSpatialDep);
					
					/*for(int p = 0; p < collectedItems.length; p++) {
						for(int q = 0; q < collectedItems[p].length; q++) {
							System.out.println("collectedItems["+p+"]["+q+"] = " + collectedItems[p][q]);	
						}
					}*/
					
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
					for(int x = 0; x < auxRemoveCurrTuple.size(); x++) {
						if(clonedMonApps.get(i).spatialRequirements.get(0).contains(auxRemoveCurrTuple.get(x))) {
							clonedMonApps.get(i).spatialRequirements.get(0).remove(auxRemoveCurrTuple.get(x));
						}
					}
					
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
				if(clonedMonApps.get(i).spatialRequirements.size() <= 0) {
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
			}
		}
		
		
		//add cycle to the list of cycles
		c.cycle_id = probeId;
		cycles_sol.add(c);
		probeId++;
		c.printCycleWithCapacity();
		currProbeCapacity = oldProbeCapacity; //reset probe capacity
	
	}while(clonedMonApps.size() > 0 && remainingItems > 0);
	
	
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



/*public ArrayList<Integer> addNodesToCycle(ArrayList<Cycle> cycles_sol) {
	
	for(int i = 0; i < cycles_sol.size(); i++) {	
		ArrayList<Integer> path = new ArrayList<Integer>();
		for(int j = 0; j < cycles_sol.get(i).links.size(); j++) {
			if(j != cycles_sol.get(i).links.size()) {
				cycles_sol.get(i).nodes.add()	
			}
			
		}
	}

	return ;
}*/

	
}
