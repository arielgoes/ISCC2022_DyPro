package main;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Collections;
import java.util.Random;

import heuristics.Cycle;

public class MonitoringApp implements Cloneable{

	//Need to define monitoring requirements
	public ArrayList<ArrayList<Integer>> spatialRequirements; //{ {0,2,3}, {4,5} }
	public ArrayList<Integer> deviceToBeCollected; // [ 3, 5 ] --> e.g., items '{0,2,3}' must be collected from device '3' and so on...
	
	//<0, 2>
	//<1, 1>
	//Hash works for sets of spatial requirements. The hash key refers to the spataialRequirement's index
	ArrayList<Integer> temporalRequirements;
	ArrayList<Integer> lastTimeCollected;  //it stores 
	
public MonitoringApp() {
	this.spatialRequirements  = new ArrayList<ArrayList<Integer>>();
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
			int maxSizeSpatialDependency, int maxFrequency, int telemetryItemsRouter, int networkSize){

		Random rnd = new Random(seed);
		int telemetryItems;
		int maxTelemetryItems = telemetryItemsRouter; //max telemetry items in a router.
		int telemetryCandidate;
		boolean hasItem = false;
		
		int contFail = 0;

		ArrayList<MonitoringApp> monitoringApps = new ArrayList<MonitoringApp>();

		for(int i = 0; i < numMonitoring; i++) {

			if(numMaxSpatialDependencies < 3) {
				telemetryItems = 3;
			}else {
				do {

					telemetryItems = rnd.nextInt(numMaxSpatialDependencies);
					
				}while(telemetryItems < 3);
			}
			
			
			
			MonitoringApp mon = new MonitoringApp();
			
			for(int j = 0; j < telemetryItems; j++) {
				
				int size;
				
				do {
					size = rnd.nextInt(maxSizeSpatialDependency+1);

				}while(size <= 1);
				
				
				ArrayList<Integer> spatialItems = null;
				
				contFail = 0;
				do {
					
					if (contFail > 20) break; //not possible to create subset
					
					spatialItems = new ArrayList<Integer>();
					
					while(spatialItems.size() < size) {
						
						do{
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

		return monitoringApps;
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
		//int numDevToBeCollected = monitoringApps.get(i).deviceToBeCollected.size();
		//System.out.println("NUM SPATIAL: " + numSpatialDependencies + " NUM COLLECTED: " + numDevToBeCollected);
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

//checks whether all restriction (spatial) items are covered by one or more probes
public ArrayList<Integer> MonRestrictionVerifier(ArrayList<Cycle> cycles, ArrayList<MonitoringApp> monitoringApps) {
	//verify spatial constraints across probes
	int restrictionItem = -1;
	ArrayList<Integer> unsatisfiedMonItems = new ArrayList<Integer>(); //adds unsatisfied items after iterating all cycles
	for(int i = 0; i < monitoringApps.size(); i++) {
		int numSpatialDependencies = monitoringApps.get(i).spatialRequirements.size();
		for(int k = 0; k < numSpatialDependencies; k++) {
			for(int l = 0; l < monitoringApps.get(i).spatialRequirements.get(k).size(); l++) {		
				restrictionItem = monitoringApps.get(i).spatialRequirements.get(k).get(l);
				int numCycles = cycles.size();
				boolean hasItem = false;
				for(int z = 0; z < numCycles; z++) {
					if(cycles.get(z).hasItem(restrictionItem)) {
						hasItem = true;
						break;
					}
				}
				if(!hasItem) {
					unsatisfiedMonItems.add(restrictionItem);	
				}
				
			}
		}
	}
	
	return unsatisfiedMonItems;
}

//checks whether all restriction (spatial) items are covered by a single probe cycle (UNDER DEVELOPMENT)
/*public ArrayList<Integer> MonRestrictionVerifier2(ArrayList<Cycle> cycles, ArrayList<MonitoringApp> monitoringApps) {
	//verify spatial constraints across probes
	int restrictionItem = -1;
	ArrayList<Integer> unsatisfiedMonItems = new ArrayList<Integer>(); //adds unsatisfied items after iterating all cycles
	int numCycles = cycles.size();
	
	for(int z = 0; z < numCycles; z++) {
		for(int i = 0; i < monitoringApps.size(); i++) {
			int numSpatialDependencies = monitoringApps.get(i).spatialRequirements.size();
			for(int k = 0; k < numSpatialDependencies; k++) {
				for(int l = 0; l < monitoringApps.get(i).spatialRequirements.get(k).size(); l++) {		
					restrictionItem = monitoringApps.get(i).spatialRequirements.get(k).get(l);
					if(!cycles.get(z).hasItem(restrictionItem)) {
						unsatisfiedMonItems.add(restrictionItem);
					}
				}
			}
		}
	}
	
	
	return unsatisfiedMonItems;
}*/











}