package main;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Map.Entry;

import heuristics.Cycle;
import heuristics.Tuple;

public class MonitoringApp implements Cloneable{

	//Need to define monitoring requirements
	public ArrayList<ArrayList<Integer>> spatialRequirements; //{ {0,2,3}, {4,5} }
	public ArrayList<ArrayList<Boolean>> collectedItems;
	public ArrayList<Integer> deviceToBeCollected; // [ 3, 5 ] --> e.g., items '{0,2,3}' must be collected from device '3' and so on...
	
	//<0, 2>
	//<1, 1>
	//Hash works for sets of spatial requirements. The hash key refers to the spataialRequirement's index
	ArrayList<Integer> temporalRequirements;
	ArrayList<Integer> lastTimeCollected;  //it stores 
	
public MonitoringApp() {
	this.spatialRequirements  = new ArrayList<ArrayList<Integer>>(); //[[2,3,4], [4,5]]
	this.temporalRequirements = new ArrayList<Integer>();
	this.lastTimeCollected    = new ArrayList<Integer>();
	this.deviceToBeCollected  = new ArrayList<Integer>(); //it tells me from what device a spatial requirement must be satisfied from 
}

@Override
public MonitoringApp clone() throws CloneNotSupportedException{
	try{
       MonitoringApp clonedMyClass = (MonitoringApp)super.clone();
       // if you have custom object, then you need create a new one in here
	   
       return clonedMyClass;
       
	   } catch (CloneNotSupportedException e) {
	       e.printStackTrace();
	       return new MonitoringApp();
	   }
}

	
ArrayList<MonitoringApp> generateMonitoringApps(long seed, int numMonitoring, int numMaxSpatialDependencies,
			int maxSizeSpatialDependency, int maxFrequency, int telemetryItemsRouter, int numDevSameReq, int networkSize){
	Random rnd = new Random(seed);
	int telemetryItems = numMaxSpatialDependencies;
	int telemetryCandidate;
	boolean hasItem = false;
	int contFail = 0;

	ArrayList<MonitoringApp> monitoringApps = new ArrayList<MonitoringApp>();

	for(int i = 0; i < numMonitoring; i++) {
		
		MonitoringApp mon = new MonitoringApp();
		
		
		
		for(int j = 0; j < telemetryItems; j++) {
			int size;
			
			do {
				size = rnd.nextInt(maxSizeSpatialDependency+1);
			}while(size < 1);
			
			
			ArrayList<Integer> spatialItems = null;
			
			contFail = 0;
			do {
				
				if (contFail > 20) break; //not possible to create subset
				
				
				spatialItems = new ArrayList<Integer>();
				
				while(spatialItems.size() < size) {
					
					do{
						//System.out.println("aqui");
						hasItem = false;
						telemetryCandidate = rnd.nextInt(telemetryItemsRouter);
						for(int l = 0; l < spatialItems.size(); l++) {
							if(spatialItems.get(l) == telemetryCandidate) {
								hasItem = true;
								break;
							}
						}
						
					}while(hasItem);
					
					spatialItems.add(telemetryCandidate);
					
				}
				
				Collections.sort(spatialItems);
				contFail++;
			}while(hasItemList(mon, spatialItems));
			
			if(spatialItems != null) {
				
				mon.spatialRequirements.add(spatialItems);
				mon.deviceToBeCollected.add(rnd.nextInt(networkSize));
				
				int freq;
				
				do {
					freq = rnd.nextInt(maxFrequency+1);  //max frequency
				}while(freq == 0);
				
				mon.temporalRequirements.add(freq);
				
			}
			
		}
		//initialize history
		for(int j = 0; j < mon.spatialRequirements.size(); j++) {
			mon.lastTimeCollected.add(0);
		}
		
		monitoringApps.add(mon);
		
	}
	
	
	if(numDevSameReq > 1) {
		int originalLength = monitoringApps.get(0).deviceToBeCollected.size();
		for(int num = 0; num < numDevSameReq-1; num++) {
			for(int a = 0; a < monitoringApps.size(); a++) {
				for(int c = 0; c < originalLength; c++) {						
					int devFromSpatial = monitoringApps.get(a).deviceToBeCollected.get(c); 
					int newDev = rnd.nextInt(networkSize); 
					
					while(devFromSpatial == newDev) {
						newDev = rnd.nextInt(networkSize);
					}
					
					ArrayList<Integer> spatialItems = new ArrayList<Integer>();
					for(Integer item : monitoringApps.get(a).spatialRequirements.get(c)) {
						spatialItems.add(item);
					}
					
					monitoringApps.get(a).spatialRequirements.add(spatialItems);
					monitoringApps.get(a).deviceToBeCollected.add(newDev);
				}
				
				//initialize history
				/*for(int j = 0; j < monitoringApps.get(a).spatialRequirements.size(); j++) {
					monitoringApps.get(a).lastTimeCollected.add(0);
				}*/
			}
		}
	}

	return monitoringApps;
}

//input: an existing set of mon apps, output: add one or more spatial reqs into every single mon app
public void addSpatialReqsToMonApps(long seed, int numSpatialReqs, ArrayList<MonitoringApp> monitoringApps, int numMaxSpatialDependencies,
		int maxSizeSpatialDependency, int maxFrequency, int telemetryItemsRouter, int networkSize) {
	
	Random rnd = new Random(seed);
	boolean hasItem = false;
	int telemetryCandidate; 
	int contFail = 0;
	
	for(int i = 0; i < numSpatialReqs; i++) {
		for(int a = 0; a < monitoringApps.size(); a++) {
			int size;
			
			do {
				size = rnd.nextInt(maxSizeSpatialDependency+1);
			}while(size < 1);
			
			
			ArrayList<Integer> spatialItems = null;
			
			contFail = 0;
			do {
				
				if (contFail > 20) break; //not possible to create subset
				
				
				spatialItems = new ArrayList<Integer>();
				
				while(spatialItems.size() < size) {
					
					do{
						//System.out.println("aqui");
						hasItem = false;
						telemetryCandidate = rnd.nextInt(telemetryItemsRouter);
						for(int l = 0; l < spatialItems.size(); l++) {
							if(spatialItems.get(l) == telemetryCandidate) {
								hasItem = true;
								break;
							}
						}
						
					}while(hasItem);
					
					spatialItems.add(telemetryCandidate);
					
				}
				
				Collections.sort(spatialItems);
				contFail++;
			}while(hasItemList(monitoringApps.get(a), spatialItems));
			
			if(spatialItems != null) {
				
				monitoringApps.get(a).spatialRequirements.add(spatialItems);
				monitoringApps.get(a).deviceToBeCollected.add(rnd.nextInt(networkSize));
				
				int freq;
				
				do {
					freq = rnd.nextInt(maxFrequency+1);  //max frequency
				}while(freq == 0);
				
				monitoringApps.get(a).temporalRequirements.add(freq);
				
			}
			
		}	
	}
	
	
}



private boolean hasItemList(MonitoringApp monitoring, ArrayList<Integer> spatialItems) {
	boolean hasList = false;
	ArrayList<ArrayList<Integer>> listItems = monitoring.spatialRequirements;
	
	for(int i = 0; i < listItems.size(); i++) {
		if (listItems.get(i).containsAll(spatialItems))
			return true;	
	}
	return hasList;	
}


public void printMonitoringApps(ArrayList<MonitoringApp> monitoringApps) {
	
	for(int i = 0; i < monitoringApps.size(); i++) {
		System.out.println("Monitoring App " + i);
		int numSpatialDependencies = monitoringApps.get(i).spatialRequirements.size();
		
		for(int k = 0; k < numSpatialDependencies; k++) {
			
			System.out.println("   Spatial Req: " + k + " | Device #" + monitoringApps.get(i).deviceToBeCollected.get(k));
			
			for(int l = 0; l < monitoringApps.get(i).spatialRequirements.get(k).size(); l++) {		
				System.out.printf("%d ", monitoringApps.get(i).spatialRequirements.get(k).get(l));	
			}
			System.out.println(" ");		
		}
	}
	
	/*for(int i = 0; i < monitoringApps.size(); i++) {
		System.out.println("Monitoring App " + i);
		int numTemporalDependencies = monitoringApps.get(i).temporalRequirements.size();
		for(int k = 0; k < numTemporalDependencies; k++) {
		
			System.out.println("   Temporal Req: " + k);
			System.out.printf("%d ", monitoringApps.get(i).temporalRequirements.get(k));
			System.out.println(" ");
		}
	}*/
	
	
}


//just counts the input number of spatial requirements to be collected later
public int countSpatialRequirements(ArrayList<MonitoringApp> monitoringApps) {
	int numSpatialReqs = 0;
	
	for(int a = 0; a < monitoringApps.size(); a++) {
		for(int b = 0; b < monitoringApps.get(a).spatialRequirements.size(); b++) {
			numSpatialReqs++;
		}
	}
	
	return numSpatialReqs; 
}



}