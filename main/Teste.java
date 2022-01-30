package main;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import heuristics.EdgeRandomization;
import heuristics.FixOptPInt;
import heuristics.HeuristicAugmentMerge;
import heuristics.KCombinations;
import heuristics.MonAppSpatialTemporalApproaches;
import heuristics.OptPathPlan;

public class Teste {

	public static void main(String[] args) throws IloException, FileNotFoundException, CloneNotSupportedException{
		
		//Parameters
		int networkSize = Integer.parseInt(args[0]); //size of the network (i.e., # of nodes)
		int capacityProbe = Integer.parseInt(args[1]); //available space in a given flow (e.g., # of bytes)	
		int maxProbes = Integer.parseInt(args[2]); //max ammount of probes allowed to solve the path generation
		int telemetryItemsRouter = Integer.parseInt(args[3]); //number of telemetry items per router 
		int maxSizeTelemetryItemsRouter = Integer.parseInt(args[4]); //max size of a given telemetry item (in bytes)
		int initSizeComb = Integer.parseInt(args[5]); // initial size of the combinations
		int maxSizeComb = Integer.parseInt(args[6]); // max size of the combinations
		int numThreads = Integer.parseInt(args[7]); //max number of threads allowed
		int subProblemTimeLimit = Integer.parseInt(args[8]); //maximum time to solve a subproblem
		int globalTimeLimit = Integer.parseInt(args[9]); //global time to cover the whole network
		int contIterNotImprovMax = Integer.parseInt(args[10]); //# of iterations without any improvement (i.e., no path reduction)
		int combSize = Integer.parseInt(args[11]); //combinations of size 'k', used on performed statistics
		int n_internal = Integer.parseInt(args[12]); //internal number of iterations
		
		long seed = 123;
		//long seed = System.currentTimeMillis();
		
		float[] normalOpStdItem = {(float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05};
		float[] normalOpAvgItem = {(float) 1.1, (float) 3.2, (float) 2.5, (float) 1.7, (float) 0.3, (float) 4.3, (float) 0.452, (float) 0.78, (float) 1.1, (float) 3.2, (float) 2.5, (float) 1.7, (float) 0.3, (float) 4.3, (float) 0.452, (float) 0.78};
		
		NetworkInfrastructure infra = null;
		EdgeRandomization modelER = null;
		//OptPathPlan pathPlanCycles = null;
		FixOptPInt fixOpt = null;
		AlgorithmOpt opt = null; //due to statistics parameters
		ArrayList<int[]> collectors = new ArrayList<int[]>();
		KCombinations kComb = new KCombinations();
		MonitoringApp monApps = new MonitoringApp();
		
		int[] array = new int[networkSize];
		for(int i = 0; i < networkSize; i++) {
			array[i] = i;
		}

		//Monitoring Apps' parameters
		int numberMonitoringApp = 6;
		int numMaxSpatialDependencies = 4;
		int maxSizeSpatialDependency = 4;
		int maxFrequency = 5;
		
		float percentDevicesAnomaly = (float) 0.5;
 		int lastingTimeAnomaly = 5;
 		int intervalTimeUnitAnomaly = 5;
 		int window = 10;
 		//IloCplex cplex = new IloCplex();
		
		String pathInstance = ""; //used in case one desires to parse a data file as input
		
		//creating infrastructure and generating a random topology
		infra = new NetworkInfrastructure(networkSize, pathInstance, telemetryItemsRouter, maxSizeTelemetryItemsRouter, (long) seed);
		infra.filePath = pathInstance;
		infra.generateRndTopology(0.7, seed);

		
		//item size verification
		int itemSize = Integer.MIN_VALUE;
		for(int k = 0; k < infra.sizeTelemetryItems.length; k++) {
			if(infra.sizeTelemetryItems[k] > itemSize) {
				itemSize = infra.sizeTelemetryItems[k];
			}
		}
		
		if(itemSize > (capacityProbe - 2)) { //infeasible
			System.out.println("-0" + ";" + "NaN" + ";" 
					+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
					+ ";" + infra.seed);
		}else {
			System.out.println("-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.- OLD MONITORING APPS -.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-");
			ArrayList<MonitoringApp> monitoringApps = monApps.generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, networkSize);
			monApps.printMonitoringApps(monitoringApps);
			
			//solver
			MonAppSpatialTemporalApproaches dynamicMonApp = new MonAppSpatialTemporalApproaches(infra, seed, capacityProbe);
			dynamicMonApp.cycles = dynamicMonApp.firstApproach(monitoringApps, capacityProbe);
			//dynamicMonApp.cycles = dynamicMonApp.secondApproach(monitoringApps, capacityProbe, monApps);
			monApps.printMonitoringApps(dynamicMonApp.clonedMonApps);
			System.out.println("TU TA NA MAIN MANO: ");
			System.out.println("(main) size cloned: " + dynamicMonApp.clonedMonApps.size());
			System.out.println("(main) size origin: " + monitoringApps.size());
			
			System.out.println("-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.- NEW MONITORING APPS -.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-");
			//solver
			monitoringApps = monApps.generateMonitoringApps(seed+1, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, networkSize);
			monApps.printMonitoringApps(dynamicMonApp.clonedMonApps);
			
			//optimizer
			System.out.println("-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.- NEW MONITORING APPS (AFTER OPTIMIZE)-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-.-");
			dynamicMonApp.dynamicMonAppProbeGenerator(monitoringApps);
			monApps.printMonitoringApps(dynamicMonApp.clonedMonApps); //should print it empty
			
			System.out.println("TU TA NA MAIN MANO DE NOVO: ");
			System.out.println("(main) size cloned: " + dynamicMonApp.clonedMonApps.size());
			System.out.println("(main) size origin: " + monitoringApps.size());
			
		}
		
	
		
		
	
	}
}
