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
import heuristics.DyPro;
import heuristics.OptPathPlan;
import heuristics.Pair;
import heuristics.Tuple;


public class Teste {

	public static void main(String[] args) throws IloException, FileNotFoundException, CloneNotSupportedException{
		//System.loadLibrary("cplex");
		//System.setProperty("java.library.path", "/opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux");
		
		//System.loadLibrary("/opt/ibm/ILOG/CPLEX_Studio129/cplex/bin/x86-64_linux/cplex");
		
		//Parameters
		int networkSize = Integer.parseInt(args[0]); //size of the network (i.e., # of nodes)
		int capacityProbe = Integer.parseInt(args[1]); //available space in a given flow (e.g., # of bytes)	
		int maxProbes = Integer.parseInt(args[2]); //max amount of probes allowed to solve the path generation
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
		int numDevSameReq = Integer.parseInt(args[17]);
		int seedArg = Integer.parseInt(args[18]);
		int addSpatialReqs = Integer.parseInt(args[19]);
		
		
		long seed = seedArg;
		//long seed = System.currentTimeMillis();
		
		float[] normalOpStdItem = {(float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05, (float) 0.05};
		float[] normalOpAvgItem = {(float) 1.1, (float) 3.2, (float) 2.5, (float) 1.7, (float) 0.3, (float) 4.3, (float) 0.452, (float) 0.78, (float) 1.1, (float) 3.2, (float) 2.5, (float) 1.7, (float) 0.3, (float) 4.3, (float) 0.452, (float) 0.78};
		
		NetworkInfrastructure infra = null;
		EdgeRandomization er = null;
		DyPro dyPro = null;
		OptPathPlan pathPlanCycles = null;
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
		//int numberMonitoringApp = 6; //set as parameter
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
		infra.generateRndTopology(seed, 0.7);

		
		//item size verification
		int itemSize = Integer.MIN_VALUE;
		for(int k = 0; k < infra.sizeTelemetryItems.length; k++) {
			if(infra.sizeTelemetryItems[k] > itemSize) {
				itemSize = infra.sizeTelemetryItems[k];
			}
		}
		
		
		if(itemSize > (capacityProbe - 2) /*|| telemetryItemsRouter < maxSizeSpatialDependency*/) { //infeasible
			System.out.println("-0" + ";" + "NaN" + ";" 
					+ infra.size + ";" + infra.telemetryItemsRouter + ";" + infra.maxSizeTelemetryItemsRouter 
					+ ";" + infra.seed);
		}else {
			
			//experiments
			ArrayList<MonitoringApp> monitoringApps = monApps.generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, numDevSameReq, networkSize);
			dyPro = new DyPro(infra, seed, capacityProbe);
			/*int temp[] = new int[2];
			er = new EdgeRandomization(infra, capacityProbe, (long) seed, maxProbes, temp, false);*/
		
			/*monApps.printMonitoringApps(monitoringApps);
			System.out.println("====================================================================");
			//monitoringApps = monApps.halfRandomMonApps(seed, monApps, monitoringApps, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, maxSizeTelemetryItemsRouter, networkSize); 
			monApps.printMonitoringApps(monitoringApps);*/
			
			
			//System.out.println("(FIRST SET) MON APPS BEFORE...");
			//monApps.printMonitoringApps(monitoringApps);
			
			
			//DynMonApp
			boolean firstIter = true;
			double timeDynMonApp = System.nanoTime();
			ArrayList<Cycle> oldCyclesDyPro = new ArrayList<Cycle>();
			oldCyclesDyPro = dyPro.firstApproach(monitoringApps, capacityProbe, firstIter);
			timeDynMonApp = (System.nanoTime() - timeDynMonApp)*0.000000001;
			//System.out.println("---------------------------------End First Approach...");
			//System.exit(0);
			
			//OPP
			double timeOPP = System.nanoTime();
			//ArrayList<Cycle> oldCyclesOPP = new ArrayList<Cycle>();
			pathPlanCycles = new OptPathPlan(infra, capacityProbe, (long) seed, true);
			pathPlanCycles.adaptToLinks();
			/*for(Cycle c : pathPlanCycles.Q) {
				oldCyclesOPP.add(c.clone());
			}*/
			timeOPP = (System.nanoTime() - timeOPP)*0.000000001;
			
			
			//FixOpt
			/*double timeFixOpt = System.nanoTime();
			fixOpt = new FixOptPInt(infra, capacityProbe, maxProbes, numThreads, (long) seed, subProblemTimeLimit, 
					globalTimeLimit, initSizeComb, maxSizeComb, countIterNotImprovMax);
			double fixOptSol = fixOpt.run(pathPlanCycles.Q);
			ArrayList<Cycle> oldCyclesFixOpt = fixOpt.convertToCycleUnordered();
			timeFixOpt = (System.nanoTime() - timeFixOpt)*0.000000001;
			fixOpt.convertToCycleUnordered(); //fetch the links, but they are unordered*/
			
			
			/*System.out.println("System.exit(0)");
			System.exit(0);*/
			
			//totallty random new mon apps (new seed)
			monitoringApps = monApps.generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, numDevSameReq, networkSize);
			ArrayList<MonitoringApp> copyMonApps = monApps.generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, numDevSameReq, networkSize);
			
			//incremental approach to insert new spatial requirements into existing monitoring applications (same seed)
			//monitoringApps = monApps.generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, numDevSameReq, networkSize);
			//ArrayList<MonitoringApp> copyMonApps = monApps.generateMonitoringApps(seed, numberMonitoringApp, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, numDevSameReq, networkSize);
			
			
			
			//System.out.println("(SECOND SET) MON APPS BEFORE...");
			//monApps.printMonitoringApps(monitoringApps);
			monApps.addSpatialReqsToMonApps(seed, addSpatialReqs, monitoringApps, numMaxSpatialDependencies, maxSizeSpatialDependency, maxFrequency, telemetryItemsRouter, networkSize);
			//monApps.printMonitoringApps(monitoringApps);
			
			//System.exit(0);
			
			
			
			//optimizer
			dyPro.dynamicProbeGenerator(monitoringApps, maxSizeSpatialDependency, monApps);
			
			
			//System.out.println("(SECOND SET) MON APPS AFTER...");
			//monApps.printMonitoringApps(monitoringApps);
			
			//Statistics
			Statistics sts = new Statistics();
			
			
			//probe usage
			double[] probeUsageOPP = new double[3]; //(min, max, avg)
			double[] probeUsageDyPro = new double[3]; //(min, max, avg)
			//double[] probeUsageFixOpt = new double[3]; //(min, max, avg)
			
			probeUsageOPP = sts.probeUsage(fixOpt, opt, maxProbes, pathPlanCycles.Q, capacityProbe, 2);
			probeUsageDyPro = sts.probeUsage(fixOpt, opt, maxProbes, dyPro.cycles, capacityProbe, 2);
			//probeUsageFixOpt = sts.probeUsage(fixOpt, opt, maxProbes, pathPlanCycles.Q, capacityProbe, 1);
			
			
			int numSpatialReqs = monApps.countSpatialRequirements(copyMonApps); //total # of existing spatial requirements
			//num of satisfied spatial requirements
			 
			int numSatisfiedOPP = sts.verifySatisfiedSpatialRequirements(copyMonApps, pathPlanCycles.Q);
			int numSatisfiedDyPro = sts.verifySatisfiedSpatialRequirements(copyMonApps, dyPro.cycles);
			//ArrayList<Cycle> fixOptCycles = fixOpt.convertToCycleUnordered();
			//int numSatisfiedFixOpt = sts.verifySatisfiedSpatialRequirements(copyMonApps, fixOptCycles);
			
			
			
			
			//device overhead: # of probes/paths pass through devices (min, max, avg)
			double[] devOverheadOPP = new double[3];
			double[] devOverheadDyPro = new double[3];
			//double[] devOverheadFixOpt = new double[3];
			
			devOverheadOPP = sts.devOverhead(fixOpt, opt, pathPlanCycles.Q, maxProbes, networkSize, 2);
			devOverheadDyPro = sts.devOverhead(fixOpt, opt, dyPro.cycles, maxProbes, networkSize, 2);
			//devOverheadFixOpt = sts.devOverhead(fixOpt, opt, pathPlanCycles.Q, maxProbes, networkSize, 1);
			
			//link overhead: # of probes/paths pass through links (min, max, avg)
			double[] linkOverheadOPP = new double[3];
			double[] linkOverheadDyPro = new double[3];
			//double[] linkOverheadFixOpt = new double[3];
			
			linkOverheadOPP = sts.linkOverhead(infra, fixOpt, opt, maxProbes, pathPlanCycles.Q, networkSize, 2);
			linkOverheadDyPro = sts.linkOverhead(infra, fixOpt, opt, maxProbes, dyPro.cycles, networkSize, 2);
			//linkOverheadFixOpt = sts.linkOverhead(infra, fixOpt, opt, maxProbes, pathPlanCycles.Q, networkSize, 1);

			
			//changes[0]: changes on # of items // changes[1]: changes on # of links // changes[2]: changes on # of cycles
			int[] changesOnMonAppsDyPro = new int[3];
			int[] changesOnMonAppsOPP = new int[3];
			//int[] changesOnMonAppsFixOpt = new int[3];
			changesOnMonAppsDyPro = sts.changesOnMonAppsAttributions(oldCyclesDyPro, dyPro.cycles);
			//changesOnMonAppsOPP = sts.changesOnMonAppsAttributions(oldCyclesOPP, pathPlanCycles.Q);
			//changesOnMonAppsFixOpt = sts.changesOnMonAppsAttributions(oldCyclesFixOpt, fixOptCycles);
			
			
			
			//printing statistics
			//OPP
			System.out.println("OPP" + ";" + pathPlanCycles.Q.size() + ";" + timeOPP + ";" + seed + ";" + pathPlanCycles.infra.size + ";" + 
					pathPlanCycles.infra.telemetryItemsRouter + ";" + pathPlanCycles.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" +
					capacityProbe + ";" + (int)probeUsageOPP[0] + ";" + (int)probeUsageOPP[1] + ";" + probeUsageOPP[2] + ";" +
					numSatisfiedOPP + ";" + numSpatialReqs + ";" + devOverheadOPP[0] + ";" + devOverheadOPP[1] + ";" +
					devOverheadOPP[2] + ";" + linkOverheadOPP[0] + ";" + linkOverheadOPP[1] + ";" + linkOverheadOPP[2] + ";" +
					numDevSameReq + ";" + numberMonitoringApp + ";" + numMaxSpatialDependencies + ";" + maxSizeSpatialDependency +
					";" + changesOnMonAppsOPP[0] + ";" + changesOnMonAppsOPP[1] + ";" + changesOnMonAppsOPP[2] + ";" + addSpatialReqs);
			
			//DynMonApp
			System.out.println("DyPro" + ";" + dyPro.cycles.size() + ";" + timeDynMonApp + ";" + seed + ";" + dyPro.infra.size + ";" + 
					dyPro.infra.telemetryItemsRouter + ";" + dyPro.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" +
					capacityProbe + ";" + (int)probeUsageDyPro[0] + ";" + (int)probeUsageDyPro[1] + ";" + probeUsageDyPro[2] + ";" +
					numSatisfiedDyPro + ";" + numSpatialReqs + ";" + devOverheadDyPro[0] + ";" + devOverheadDyPro[1] + ";" +
					devOverheadDyPro[2] + ";" + linkOverheadDyPro[0] + ";" + linkOverheadDyPro[1] + ";" + linkOverheadDyPro[2] +
					";" + numDevSameReq + ";" + numberMonitoringApp + ";" + numMaxSpatialDependencies + ";" + maxSizeSpatialDependency +
					";" + changesOnMonAppsDyPro[0] + ";" + changesOnMonAppsDyPro[1] + ";" + changesOnMonAppsDyPro[2] + ";" + addSpatialReqs);
			
			//FixOpt
			/*System.out.println("FixOpt" + ";" + (int)fixOptSol + ";" + timeFixOpt + ";" + seed + ";" + fixOpt.infra.size + ";" + fixOpt.infra.telemetryItemsRouter +
					";" + fixOpt.infra.maxSizeTelemetryItemsRouter + ";" + maxProbes + ";" + capacityProbe + ";" + (int)probeUsageFixOpt[0] + ";" + 
					(int)probeUsageFixOpt[1] + ";" + probeUsageFixOpt[2] + ";" + numSatisfiedFixOpt + ";" + numSpatialReqs + ";" + devOverheadFixOpt[0] +
					";" + devOverheadFixOpt[1] + ";" + devOverheadFixOpt[2] + ";" + linkOverheadFixOpt[0] + ";" + linkOverheadFixOpt[1] + ";" +
					linkOverheadFixOpt[2] + ";" + numDevSameReq + ";" + numberMonitoringApp + ";" + numMaxSpatialDependencies + ";" +
					maxSizeSpatialDependency + ";" + changesOnMonAppsFixOpt[0] + ";" + changesOnMonAppsFixOpt[1] + ";" + changesOnMonAppsFixOpt[2]  + ";" + addSpatialReqs);*/
			
			
		}
		
	
	}
}