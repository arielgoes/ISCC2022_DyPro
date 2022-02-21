package main;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import heuristics.Cycle;
import heuristics.EdgeRandomization;
import heuristics.FixOptPInt;
import heuristics.HeuristicAugmentMerge;
import heuristics.KCombinations;
import heuristics.MonAppSpatialTemporalApproaches;
import heuristics.OptPathPlan;


public class Teste {

	public static void main(String[] args) throws IloException, FileNotFoundException, CloneNotSupportedException{
		//System.loadLibrary("cplex");
		//System.setProperty("java.library.path", "/opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux");
		
		//System.loadLibrary("/opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux/cplex");
		
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
		int countIterNotImprovMax = Integer.parseInt(args[10]); //# of iterations without any improvement (i.e., no path reduction)
		int combSize = Integer.parseInt(args[11]); //combinations of size 'k', used on performed statistics
		int n_internal = Integer.parseInt(args[12]); //internal number of iterations
		int numberMonitoringApp = Integer.parseInt(args[13]);
		int numMaxSpatialDependencies = Integer.parseInt(args[14]);
		int maxSizeSpatialDependency = Integer.parseInt(args[15]);
		int maxFrequency = Integer.parseInt(args[16]);
		int seedArg = Integer.parseInt(args[17]);
		
		
		long seed = seedArg;
		//long seed = System.currentTimeMillis();
		
		float[] normalOpStdItem = {(float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05};
		float[] normalOpAvgItem = {(float) 1.1, (float) 3.2, (float) 2.5, (float) 1.7, (float) 0.3, (float) 4.3, (float) 0.452, (float) 0.78, (float) 1.1, (float) 3.2, (float) 2.5, (float) 1.7, (float) 0.3, (float) 4.3, (float) 0.452, (float) 0.78};
		
		NetworkInfrastructure infra = null;
		EdgeRandomization er = null;
		MonAppSpatialTemporalApproaches dynMonApp = null;
		//OptPathPlan pathPlanCycles = null;
		FixOptPInt fixOpt = null;
		AlgorithmOpt opt = null; //due to statistics parameters
		//ArrayList<int[]> collectors = new ArrayList<int[]>();
		//KCombinations kComb = new KCombinations();
		MonitoringApp monApps = new MonitoringApp();
		
		int[] array = new int[networkSize];
		for(int i = 0; i < networkSize; i++) {
			array[i] = i;
		}

		//Monitoring Apps' parameters
		//int numberMonitoringApp = 10; //set as parameter
		//int numMaxSpatialDependencies = 4; 
		//int maxSizeSpatialDependency = 4; //set as telemetryItemsRouter (number of telemetry items per router)
		//int maxFrequency = 5;
		
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
			
			//experiments
			ArrayList<MonitoringApp> monitoringApps = monApps.generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, networkSize);
			dynMonApp = new MonAppSpatialTemporalApproaches(infra, seed, capacityProbe);
			er = new EdgeRandomization(infra, capacityProbe, (long) seed, maxProbes);
			boolean firstIter = true;
		
			/*monApps.printMonitoringApps(monitoringApps);
			System.out.println("====================================================================");
			//monitoringApps = monApps.halfRandomMonApps(seed, monApps, monitoringApps, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, maxSizeTelemetryItemsRouter, networkSize); 
			monApps.printMonitoringApps(monitoringApps);*/
			
			
			//System.exit(0);
			
			//DynMonApp
			double timeDynMonApp = System.nanoTime();
			dynMonApp.firstApproach(monitoringApps, capacityProbe, firstIter);
			timeDynMonApp = (System.nanoTime() - timeDynMonApp)*0.000000001;
			//dynMonApp.secondApproach(monitoringApps, capacityProbe, firstIter); //the path is repeating nodes/edges (fix it)
			
			//ER
			double timeER = System.nanoTime();
			er.runER();
			timeER = (System.nanoTime() - timeER)*0.000000001;
			
			//FixOpt
			double timeFixOpt = System.nanoTime();
			fixOpt = new FixOptPInt(infra, capacityProbe, maxProbes, numThreads, (long) seed, subProblemTimeLimit, 
					globalTimeLimit, initSizeComb, maxSizeComb, countIterNotImprovMax);
			double fixOptSol = fixOpt.run(er.cycles);
			timeFixOpt = (System.nanoTime() - timeFixOpt)*0.000000001;
			fixOpt.convertToCycleUnordered(); //fetch the links, but they are unordered
			
			//totallty random new mon apps
			monitoringApps = monApps.generateMonitoringApps(seed+1, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, networkSize);
			ArrayList<MonitoringApp> copyMonApps = monApps.generateMonitoringApps(seed+1, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, networkSize);
			
			//half stays the same, half is random
			//monitoringApps = monApps.generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, networkSize);
			//monitoringApps = monApps.halfRandomMonApps(seed, monApps, monitoringApps, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, maxSizeTelemetryItemsRouter, networkSize);
			//ArrayList<MonitoringApp> copyMonApps = monApps.generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, networkSize); 
			//monApps.halfRandomMonApps(seed, monApps, copyMonApps, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, maxSizeTelemetryItemsRouter, networkSize);
			
			//optimizer
			dynMonApp.dynamicProbeGenerator(monitoringApps,numMaxSpatialDependencies,maxSizeSpatialDependency);
			
			//Statistics
			Statistics sts = new Statistics();
			
			//probe usage
			double[] probeUsageER = new double[3]; //(min, max, avg)
			double[] probeUsageDynMonApp = new double[3]; //(min, max, avg)
			double[] probeUsageFixOpt = new double[3]; //(min, max, avg)
			
			probeUsageER = sts.probeUsage(fixOpt, opt, maxProbes, er.cycles, capacityProbe, 2);
			probeUsageDynMonApp = sts.probeUsage(fixOpt, opt, maxProbes, dynMonApp.cycles, capacityProbe, 2);
			probeUsageFixOpt = sts.probeUsage(fixOpt, opt, maxProbes, er.cycles, capacityProbe, 1);

			//probeUsageFixOpt = sts.probeUsage(fixOpt, opt, maxProbes, er.cycles, capacityProbe, 1);
			
			
			int numSpatialReqs = monApps.countSpatialRequirements(copyMonApps); //total # of existing spatial requirements
			//num of satisfied spatial requirements
			int numSatisfiedDynMonApp = sts.verifySatisfiedSpatialRequirements(copyMonApps, dynMonApp.cycles); 
			int numSatisfiedER = sts.verifySatisfiedSpatialRequirements(copyMonApps, er.cycles);
			ArrayList<Cycle> fixOptCycles = fixOpt.convertToCycleUnordered();
			int numSatisfiedFixOpt = sts.verifySatisfiedSpatialRequirements(copyMonApps, fixOptCycles);
			
			
			//device overhead: # of probes/paths pass through devices (min, max, avg)
			double[] devOverheadER = new double[3];
			double[] devOverheadDynMonApp = new double[3];
			double[] devOverheadFixOpt = new double[3];
			devOverheadER = sts.devOverhead(fixOpt, opt, er.cycles, maxProbes, networkSize, 2);
			devOverheadDynMonApp = sts.devOverhead(fixOpt, opt, dynMonApp.cycles, maxProbes, networkSize, 2);
			devOverheadFixOpt = sts.devOverhead(fixOpt, opt, er.cycles, maxProbes, networkSize, 1);
			
			//link overhead: # of probes/paths pass through links (min, max, avg)
			double[] linkOverheadER = new double[3];
			double[] linkOverheadDynMonApp = new double[3];
			double[] linkOverheadFixOpt = new double[3];
			linkOverheadER = sts.linkOverhead(infra, fixOpt, opt, maxProbes, er.cycles, networkSize, 2);
			linkOverheadDynMonApp = sts.linkOverhead(infra, fixOpt, opt, maxProbes, dynMonApp.cycles, networkSize, 2);
			linkOverheadFixOpt = sts.linkOverhead(infra, fixOpt, opt, maxProbes, er.cycles, networkSize, 1);

			
			
			//printing statistics
			//ER
			System.out.println("ER" + ";" + er.cycles.size() + ";" + timeER + ";" + seed + ";" + er.infra.size + ";" + 
					er.infra.telemetryItemsRouter + ";" + er.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" +
					capacityProbe + ";" + (int)probeUsageER[0] + ";" + (int)probeUsageER[1] + ";" + probeUsageER[2] + ";" +
					numSatisfiedER + ";" + numSpatialReqs + ";" + devOverheadER[0] + ";" + devOverheadER[1] + ";" +
					devOverheadER[2] + ";" + linkOverheadER[0] + ";" + linkOverheadER[1] + ";" + linkOverheadER[2]);
			
			//DynMonApp
			System.out.println("DynMonApp" + ";" + dynMonApp.cycles.size() + ";" + timeDynMonApp + ";" + seed + ";" + dynMonApp.infra.size + ";" + 
					dynMonApp.infra.telemetryItemsRouter + ";" + dynMonApp.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" +
					capacityProbe + ";" + (int)probeUsageDynMonApp[0] + ";" + (int)probeUsageDynMonApp[1] + ";" + probeUsageDynMonApp[2] + ";" +
					numSatisfiedDynMonApp + ";" + numSpatialReqs + ";" + devOverheadDynMonApp[0] + ";" + devOverheadDynMonApp[1] + ";" +
					devOverheadDynMonApp[2] + ";" + linkOverheadDynMonApp[0] + ";" + linkOverheadDynMonApp[1] + ";" + linkOverheadDynMonApp[2]);
			
			//FixOpt
			System.out.println("FixOpt" + ";" + fixOptSol + ";" + timeFixOpt + ";" + seed + ";" + fixOpt.infra.size + ";" + fixOpt.infra.telemetryItemsRouter +
					";" + fixOpt.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe + ";" + (int)probeUsageFixOpt[0] + ";" + 
					(int)probeUsageFixOpt[1] + ";" + probeUsageFixOpt[2] + ";" + numSatisfiedFixOpt + ";" + numSpatialReqs + ";" + devOverheadFixOpt[0] +
					";" + devOverheadFixOpt[1] + ";" + devOverheadFixOpt[2] + ";" + linkOverheadFixOpt[0] + ";" + linkOverheadFixOpt[1] + ";" +
					linkOverheadFixOpt[2]);
			
			
		}
		
	
	}
}
