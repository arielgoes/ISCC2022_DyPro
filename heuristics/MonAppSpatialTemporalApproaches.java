package heuristics;

import java.util.ArrayList;
import main.MonitoringApp;

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

public class MonAppSpatialTemporalApproaches {
	public ArrayList<Cycle> cycles;
	
	public MonAppSpatialTemporalApproaches() {
		this.cycles = new ArrayList<Cycle>();
	}
	
	
	
	public ArrayList<Cycle> firstApproach(ArrayList<MonitoringApp> monitoringApps) {
		ArrayList<Cycle> cycles_sol = new ArrayList<Cycle>();
		
		int item = -1;
		
		for(int i = 0; i < monitoringApps.size(); i++) { //iterate over all sets of spatial requirements
			int numSpatialDependencies = monitoringApps.get(i).spatialRequirements.size();
			for(int k = 0; k < numSpatialDependencies; k++) { //iterate over mon apps' spatial requirements (items)
				for(int l = 0; l < monitoringApps.get(i).spatialRequirements.get(k).size(); l++) {
					item = monitoringApps.get(i).spatialRequirements.get(k).get(l);
					//ArrayList<Integer> req_array
					
				}
			}
		}
		
		return cycles_sol; 
	}
	
}
