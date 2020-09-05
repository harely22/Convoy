package mypack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

//This project measures the fairness and efficiency of a single Sequential Online Chore Division (SOCD) game,
//and how close it is to perfect efficiency (ex-post proportionality)
//It is built on top of project "Convoy" which was made for the repeated game scenario - so some things are redundant
//Test2 is the main file that contains all of the definitions and runs the simulation
// 

public class Test2 {
	/*
	 * public static int numStations=100; 
	 * public static int numVehicles=100;
	 * 
	 * public static int maxNumberOfCycles=100; Determines the maximal number of cycles, but the experiments will run for all the nmbers of cycles for 0 to that number in increments of cycle step
	 * public static int testsPerCycle=10; Determines how many tests to run and average for every cycle step
	 * 
	 * public static int cycleStep=100; Determines the increment of number of cycles in each test iteration up to maxNumberOfCycles
	 */

	public static int numStations=100;
	public static int numVehicles=10;
	public static int NumberOfTrials=100;
	private static boolean uniformDistribution=false;//used to determine start and end stations
	public static boolean dynamicAdjustmentOfTTR=true;//used to dynamically adjust time to rotation of vehicles that have not finished leading, with every new entry

	public static List<Event> eventList;
	public static double[][] AvailabilityPeriod=new double[NumberOfTrials][numVehicles];
	public static double[][] EAPS=new double[NumberOfTrials][numVehicles];
	public static double[][] EPPS=new double[NumberOfTrials][numVehicles];
	public static double[][] SGActualShare=new double[NumberOfTrials][numVehicles];//Single game
	public static double[][] RGActualShare=new double[NumberOfTrials][numVehicles];//Repeated game
	public static double[] SGtoEPPS=new double[NumberOfTrials*numVehicles];//for gini index
	public static double[] RGtoEPPS=new double[NumberOfTrials*numVehicles];//for gini index


	public static boolean debugMode=true;

	//	public static double participationThreshold=0.9;
	public static double participationProbability=0.1;
	public static double TotalAccumulatedProportionalShare;
	public static double TotalAccumulatedActualShare;
	public static Station[] stations;
	public static Vehicle[] vehicles;
	public static Convoy convoy;
	Road r=new Road();
//	public static Random rand=new Random(73);//this instance had the rounding error

	public static Random rand;

	public static void main(String[] args) {
		if(debugMode) {	System.out.println("Starting Convoy");}
		for (int t=0;t<NumberOfTrials;t++) {
			rand=new Random(t);//this is the setup for testing

			if(debugMode) {			System.out.println("Trial number "+t);}
			if(convoy!=null) {
				convoy.vehicles=null;
				convoy.RGVehicles=null;
				convoy.sortedVehicles=null;
				convoy=null;
			}
			stations=null;
			vehicles=null;
			eventList=null;
			eventList=new ArrayList<Event>();

			convoy=new Convoy();
			generateStations();
			generateVehicles();
			Collections.sort(eventList);
			runConvoy(t);
			if(debugMode) {	printStats();}
		}
		printFinalStats1();
		calculateGiniIndex();
	}
	private static void runConvoy(int trialNumber) {
		if(debugMode) {	System.out.println("running convoy, going over events ");}
		Event currentEvent=null;
		Event previousEvent=null; //needed to calculate segment for EPPS
		Event previousEventIncludingRotations=null; //needed to update TTR


		for(int i=0; i<eventList.size();i++) {
			if(debugMode) {		printEventList();
			printStats();
			convoy.printConvoy();
			}

			currentEvent=eventList.get(i);
			if(debugMode) {	System.out.println("currentEvent "+ i+" ="+currentEvent);}
			/////////////////////////////0 ////////////////////////////////////////////////////////////

			Vehicle previousLeader=convoy.getLeader();
			if(dynamicAdjustmentOfTTR) {
				//update TTR for leader - this needs to be done for any event
				double timeFromPreviousEvent=0;

				if(previousEventIncludingRotations!=null&&previousLeader!=null) {
					timeFromPreviousEvent=currentEvent.getTime()-previousEventIncludingRotations.getTime();			
					previousLeader.setremainingTimeToRotation(previousLeader.getremainingTimeToRotation()-timeFromPreviousEvent);
					if(debugMode) {	
						System.out.println("currentEvent.getTime() "+ currentEvent.getTime()+" previousEvent.getTime() ="+previousEventIncludingRotations.getTime());

						System.out.println("Updating TTR for current leader "+ previousLeader.getIndex()+" to be ="+previousLeader.getremainingTimeToRotation());}
				}
			}
			//			if(debugMode) {	convoy.printConvoy();}
			/////////////////////////////1. Arrival////////////////////////////////////////////////////
			if(currentEvent.getType()==1) {//arrive
				//			System.out.println("Event "+i +" arrival event ");
				//0. calculate ex post proportional share for all the participants for this segment before adding new ones
				convoy.calculateExPostProportionalShares(currentEvent, previousEvent);

				//1. Update leaders in Convoy, Current Leader, New Leader
				// Inserting in a sorted order according depart time, so new entrant doesn't neccesarily become the leader
				Vehicle newVehicle=vehicles[currentEvent.getVID()];
				//	Vehicle previousLeader=convoy.getLeader();

				if(debugMode) {	System.out.print("Adding new Vehicle and Updating leaders as needed. ");}
				boolean insertedAsLeader;
				
				
				//If dynamicly adjusting TTR then entering vehicles must enter after the ones that finished leading otherwise there is a situation where a rotated vehicle again finds itself being the leader
				//this happens when new entrants with farther destinations arrive, enter from the back and push the ones that fininshed to the front while at the same time reducing the TTR of the leader
				if(dynamicAdjustmentOfTTR) {
					insertedAsLeader=convoy.insertSortedByDepartureAfterRotatedVehicles(currentEvent);
				}
				else {
					insertedAsLeader=convoy.insertSortedByDeparture(currentEvent);
				}
				if(debugMode) {	System.out.println("insertedAsLeader="+insertedAsLeader);}
				convoy.updateLeaderRG(currentEvent);

				if(debugMode) {System.out.println("Vehicle "+newVehicle.getIndex()+" entered. calculating its prop shares");
				}

				//3. calculate ex-ante proportional share for new leader for entire availability period, and the time to rotation which does not include solitary periods
				//if dynamicAdjustmentOfTTR, this also adjusts the EAPS of existing vehicles
				Event rotationEvent;
				if(dynamicAdjustmentOfTTR) {
					rotationEvent=convoy.calculateEAPSandTTRwithDynamicAdjustment(newVehicle,currentEvent);
				}
				else {
					rotationEvent=convoy.calculateEAPSandTTR(newVehicle,currentEvent);
				}

				//4. Update and adjust remaining EAPS of existing vehicles that have not finished leading their share

				//5. Add rotation event for new leader (if it is not the first)
				if(insertedAsLeader) {
					convoy.addRotationEvent(rotationEvent);
					newVehicle.setRotation(rotationEvent);
					//5. delete rotation for current leader (if there is one and it's not the first)
					if(previousLeader!=null&&rotationEvent.getV()!=previousLeader) {
						deleteRotationEvent(previousLeader,currentEvent);
						//		System.out.println ("deleteRotationEvent for the agent who was the leader="+previousLeader.getIndex());
					}
				}
				else {
					if(previousLeader!=null&&previousLeader.getRotation()!=null) {
						previousLeader.setRotationTime(currentEvent.getTime()+previousLeader.getremainingTimeToRotation());
					}
					eventList.remove(convoy.getLeader().getRotation());
					convoy.insertEventSorted(convoy.getLeader().getRotation());


				}
			}
			/////////////////////////////2. Leave////////////////////////////////////////////////////

			else if(currentEvent.getType()==2) {//leave
				if(debugMode) {	System.out.println("Event "+i +" leave event ");}
				//1. Update leaders in Convoy, Current Leader, New Leader

				Vehicle leavingVehicle=vehicles[currentEvent.getVID()];
				Vehicle leader=convoy.getLeader();
				if(leavingVehicle==leader) {

					leader.setLeader(false,currentEvent.getTime());//this calls updateRemainingEAPS in Vehicle
					if(debugMode) {	System.out.print("leaving agent was the leader="+leader.getIndex());}
					if(convoy.getLength()>1) {
						Vehicle newLeader=convoy.vehicles.get(convoy.getLength()-2);
						newLeader.setLeader(true,currentEvent.getTime());	
						if(debugMode) {	System.out.println(" New leader="+newLeader.getIndex());}
						convoy.setLeader(newLeader);
						//Add rotation event for new leader based on its remaining EAPS
						Event rotationEvent=new Event(3,newLeader.getIndex(),currentEvent.getTime()+newLeader.getremainingTimeToRotation());
						convoy.addRotationEvent(rotationEvent);
						newLeader.setRotation(rotationEvent);


					}
					else {
						if(debugMode) {	System.out.println(" No vehicles left");}
						convoy.setLeader(null);
					}
				}
				else {
					if(debugMode) {System.out.println(" leaving vehicle is not the leader. ");}
				}

				//1. Update leaders in RG Convoy, Current Leader, New Leader

				Vehicle RGleader=convoy.getLeaderRG();
				if(leavingVehicle==RGleader) {

					RGleader.setLeaderRG(false,currentEvent.getTime());//this calls updateActualLeadTimeRG in Vehicle
					if(debugMode) {System.out.print("leaving agent was the RG leader="+RGleader.getIndex());}
					if(convoy.getLength()>1) {
						Vehicle newLeader=convoy.RGVehicles.get(convoy.getLength()-2);
						newLeader.setLeaderRG(true,currentEvent.getTime());	
						if(debugMode) {	System.out.println(" New leader RG="+newLeader.getIndex());}
						convoy.setLeaderRG(newLeader);

					}
					else {
						if(debugMode) {System.out.println(" No vehicles left");}
						convoy.setLeaderRG(null);
					}
				}
				else {
					if(debugMode) {System.out.println(" leaving vehicle is not the RG leader. ");}
				}


				//2. calculate ex post proportional share for all the participants for this segment
				convoy.calculateExPostProportionalShares(currentEvent,previousEvent);

				//3. record all stats for leaving vehicle

				recordStats(trialNumber,leavingVehicle);
				//4. remove leaving vehicle from convoy

				convoy.vehicles.remove(leavingVehicle);
				convoy.RGVehicles.remove(leavingVehicle);
				//5. Remove rotation event for leaving vehicle if exists
				deleteRotationEvent(leavingVehicle, currentEvent);

				if(debugMode) {	System.out.println("removed leaving Vehicle : "+leavingVehicle.getIndex());}


			}
			/////////////////////////////3. Rotate////////////////////////////////////////////////////

			else if(currentEvent.getType()==3) {//rotate
				if(debugMode) {	System.out.println("Event "+i +" rotate event ");
				//		convoy.printConvoy();
				}
				//1. Update leaders in Convoy, Current Leader, New Leader
				if(debugMode) {	System.out.println("Vehicle "+currentEvent.getVID() +" is rotating. convoy size="+convoy.vehicles.size());
				}
				Vehicle rotatingVehicle=vehicles[currentEvent.getVID()];
				rotatingVehicle.setLeader(false, currentEvent.getTime());
				if(convoy.vehicles.size()>1) {
					if(debugMode) {	System.out.println("rotating vehicle index="+convoy.vehicles.indexOf(rotatingVehicle)+" new leader="+convoy.vehicles.get(convoy.vehicles.size()-2).getIndex());
					}
					Vehicle newLeader=convoy.vehicles.get(convoy.vehicles.size()-2);
					newLeader.setLeader(true, currentEvent.getTime());
					convoy.setLeader(newLeader);

					if(debugMode) {	System.out.println("adding rotation of new leader "+newLeader.getIndex()+" at current time plus "+ newLeader.getremainingTimeToRotation());
					}


					//3. Add rotation event for new leader (if it is not the first)
					//			if(convoy.vehicles.size()>2) {
					//				if(currentEvent.getTime()+newLeader.getremainingTimeToRotation()<newLeader.destinationStation) {
					//Add rotation event for new leader based on its remaining EAPS
					Event rotationEvent=new Event(3,newLeader.getIndex(),currentEvent.getTime()+newLeader.getremainingTimeToRotation());
					convoy.addRotationEvent(rotationEvent);
					newLeader.setRotation(rotationEvent);
					//				}
					//			}
					//4. move rotating vehicle to back of convoy
					convoy.vehicles.remove(rotatingVehicle);
					convoy.vehicles.add(0,rotatingVehicle);

					if(debugMode) {		System.out.println("rotating Vehicle="+rotatingVehicle.getIndex()+" , new leader="+convoy.getLeader().getIndex());
					}

					//5. delete rotation event from event list 
		//			deleteRotationEvent(rotatingVehicle,currentEvent);

				}
				else {
					if(debugMode) {	System.out.println("rotation error - single vehicle in convoy");}
				}
			}
			if(currentEvent.getType()!=3) { //for EPPS the rotations don't count
				previousEvent=currentEvent;
			}
			previousEventIncludingRotations =currentEvent;
		}
	}
	private static void recordStats(int trialNumber, Vehicle leavingVehicle) {
		AvailabilityPeriod[trialNumber][leavingVehicle.getIndex()]=leavingVehicle.destinationStation-leavingVehicle.originStation;

		EPPS[trialNumber][leavingVehicle.getIndex()]=leavingVehicle.getExPostProportionalShare();
		EAPS[trialNumber][leavingVehicle.getIndex()]=leavingVehicle.getExAnteProportionalShare();
		SGActualShare[trialNumber][leavingVehicle.getIndex()]=leavingVehicle.actual_SG_leadTime;//Single game
		RGActualShare[trialNumber][leavingVehicle.getIndex()]=leavingVehicle.actual_RG_leadTime;//Repeated game		
	}
	private static void generateStations() {
		if(debugMode) {System.out.println("Generating "+numStations+" Stations");}
		Station.counter=0;
		stations=new Station[numStations];
		for (int i=0;i<numStations;i++) {
			stations[i]=new Station();
		}
	}
	private static void generateVehicles() {
		if(debugMode) {System.out.println("Generating "+numVehicles+" Vehicles and adding them to stations");}
		vehicles=new Vehicle[numVehicles];
		Vehicle.counter=0;
		for (int i=0;i<numVehicles;i++) {
			//since the convoy traveles at a constant speed, time and distance are synonimous
			int start=0;
			int end=0;
			if(uniformDistribution) {
				start=(int)(rand.nextDouble()*numStations);
				end=start +1+(int)(rand.nextDouble()*numStations);
			}

			else{//bi polar distribution
				double percent=10;
				start=(int)(rand.nextDouble()*(double)(numStations)*percent/100);
				end=(int)((double)(numStations)*(1-percent/100)+(int)(rand.nextDouble()*(double)(numStations)*percent/100));
			}

			Event startTime=new Event(1,i,start);
			Event endTime=new Event(2,i,end);

			eventList.add(startTime);
			eventList.add(endTime);
			vehicles[i]=new Vehicle(start,end,i);
			if(debugMode) {	System.out.println(vehicles[i]);}
			startTime.setV(vehicles[i]);
			endTime.setV(vehicles[i]);
			stations[vehicles[i].currentStation].vehicles.add(vehicles[i]);
		}


	}


	public static void deleteRotationEvent(Vehicle vehicleToRemove, Event afterThis) {
		Event vehicleToRemoveRotation;
		for(int i=0; i<eventList.size();i++) {
			vehicleToRemoveRotation=eventList.get(i);
			if(vehicleToRemoveRotation.getType()==3&&vehicleToRemoveRotation.getVID()==vehicleToRemove.getIndex()&&vehicleToRemoveRotation.getTime()>=afterThis.getTime()) {//rotate
				if(debugMode) {		System.out.println("removing rotation Event for previous leader ="+vehicleToRemove.getIndex());
				}
				eventList.remove(i);
				if(debugMode) {System.out.println("Deleting Event "+vehicleToRemoveRotation);	}
				break;
			}
		}
	}

	private static void printStats() {
		double averageNumberOfConvoys=0;
		double averageNumberOfVehicleWithMoreThanTenPercentRatio=0;
		System.out.println("==============================================");
		System.out.println("===============Printing Stats For a Single Game =================");
		System.out.println("==============================================");
		for(Vehicle v: vehicles) {
			System.out.println(v);
		}


	}

	private static void printFinalStats1() {

		System.out.println("==============================================");
		System.out.println("===============Printing Final Stats1=================");
		System.out.println("==============================================");
		System.out.println("NumberOfTrials="+NumberOfTrials);
		System.out.println("numVehicles="+numVehicles);
		System.out.println("numStations="+numStations);

		for (int trialNumber=0;trialNumber<NumberOfTrials;trialNumber++) {
			System.out.println("Trial number="+trialNumber);
			System.out.println("VID \t AvailabilityPeriod \t EPPS \t EAPS \t SGActualShare \t RGActualShare \t SGtoEPPS \t RGtoEPPS" );
			double totalLeadtimeSG=0;
			double totalLeadtimeRG=0;
			double combinedAvailabilityPeriod=eventList.get(eventList.size()-1).getTime()-eventList.get(0).getTime();

			for (int v=0;v<numVehicles;v++) {
				System.out.print(" "+v+"  ");
				System.out.print("\t"+AvailabilityPeriod[trialNumber][v]);
				System.out.print("\t"+EPPS[trialNumber][v]);
				System.out.print("\t"+EAPS[trialNumber][v]);		
				System.out.print("\t"+SGActualShare[trialNumber][v]);
				System.out.print("\t"+RGActualShare[trialNumber][v]);
				SGtoEPPS[trialNumber*numVehicles+v]=SGActualShare[trialNumber][v]/EPPS[trialNumber][v];
				RGtoEPPS[trialNumber*numVehicles+v]=RGActualShare[trialNumber][v]/EPPS[trialNumber][v];

				System.out.print("\t"+SGActualShare[trialNumber][v]/EPPS[trialNumber][v]); 
				System.out.print("\t"+RGActualShare[trialNumber][v]/EPPS[trialNumber][v]);//if(SGActualShare[trialNumber][v]>EAPS[trialNumber][v]) {System.out.print(" Error! SG>EAPS");}
				System.out.println();
				totalLeadtimeSG+=SGActualShare[trialNumber][v];
				totalLeadtimeRG+=RGActualShare[trialNumber][v];

			}	
			//		if(combinedAvailabilityPeriod!=totalLeadtimeSG) { System.out.println("Error total SG lead time doesn't match combinedAvailabilityPeriod "+totalLeadtimeSG+" vs. " + combinedAvailabilityPeriod);}
			//		if(combinedAvailabilityPeriod!=totalLeadtimeRG) { System.out.println("Error total RG lead time doesn't match combinedAvailabilityPeriod "+totalLeadtimeRG+" vs. " + combinedAvailabilityPeriod);}

		}

	}

	private static void calculateGiniIndex() {
		System.out.println("==============================================");
		System.out.println("===============Calculating Gini index=================");
		System.out.println("==============================================");
		System.out.println("NumberOfTrials="+NumberOfTrials);
		System.out.println("numVehicles="+numVehicles);
		System.out.println("numStations="+numStations);
		
		System.out.println("dynamicAdjustmentOfTTR="+Test2.dynamicAdjustmentOfTTR);
		System.out.println("uniformDistribution="+Test2.uniformDistribution);
		double[][] SGtoEPPSdifferences= new double[NumberOfTrials*numVehicles][NumberOfTrials*numVehicles];
		double[][] RGtoEPPSdifferences= new double[NumberOfTrials*numVehicles][NumberOfTrials*numVehicles];

		double SGsumOfDifferences=0;
		double RGsumOfDifferences=0;

		double sumSGtoEPPSRatio=0;
		double sumRGtoEPPSRatio=0;
		double averageSGtoEPPSRatio=0;
		double averageRGtoEPPSRatio=0;

		double SGdiffIJ=0;
		double RGdiffIJ=0;

		for (int i=0;i<NumberOfTrials*numVehicles;i++) {
			for (int j=0;j<NumberOfTrials*numVehicles;j++) {
				SGdiffIJ=SGtoEPPS[i]-SGtoEPPS[j];
				RGdiffIJ=RGtoEPPS[i]-RGtoEPPS[j];
				//		System.out.println("("+i+","+j+") SGdiffIJ="+SGdiffIJ+" SGtoEPPS[i]="+SGtoEPPS[i]+" SGtoEPPS[j]"+SGtoEPPS[j]);
				//		System.out.println("("+i+","+j+") RGdiffIJ="+RGdiffIJ+" RGtoEPPS[i]="+RGtoEPPS[i]+" RGtoEPPS[j]"+RGtoEPPS[j]);
				SGtoEPPSdifferences[i][j]=SGdiffIJ;
				RGtoEPPSdifferences[i][j]=RGdiffIJ;

				SGsumOfDifferences+=Math.abs(SGdiffIJ);
				RGsumOfDifferences+=Math.abs(RGdiffIJ);
				//		System.out.println("SGsumOfDifferences="+SGsumOfDifferences);
				//		System.out.println("RGsumOfDifferences="+RGsumOfDifferences);


				//		System.out.println("("+i+","+j+") SGtoEPPS[i]="+SGtoEPPS[i]+" SGtoEPPS[j]="+SGtoEPPS[j]);
				//		System.out.println("("+i+","+j+") RGtoEPPS[i]="+RGtoEPPS[i]+" RGtoEPPS[j]="+RGtoEPPS[j]);
			}	
			//		System.out.print(SGtoEPPS[i]+" ,");
//			System.out.print(RGtoEPPS[i]+" ,");


			sumSGtoEPPSRatio+=SGtoEPPS[i];
			sumRGtoEPPSRatio+=RGtoEPPS[i];


		}
		System.out.println();

		averageSGtoEPPSRatio=sumSGtoEPPSRatio/(NumberOfTrials*numVehicles);
		averageRGtoEPPSRatio=sumRGtoEPPSRatio/(NumberOfTrials*numVehicles);

		double giniSG=SGsumOfDifferences/(2*NumberOfTrials*numVehicles*sumSGtoEPPSRatio);
		double giniRG=RGsumOfDifferences/(2*NumberOfTrials*numVehicles*sumRGtoEPPSRatio);

		System.out.println("averageSGtoEPPSRatio="+averageSGtoEPPSRatio);
		System.out.println("averageRGtoEPPSRatio="+averageRGtoEPPSRatio);
		System.out.println("SGsumOfDifferences="+SGsumOfDifferences);
		System.out.println("RGsumOfDifferences="+RGsumOfDifferences);
		System.out.println("Gini Index for SG="+giniSG);
		System.out.println("Gini Index for RG="+giniRG);
		System.out.println();

		System.out.println("The values used to compute the index are the ratio of SG actual lead time to EPPS");
		System.out.println("raw data for single game =");

		for (int i=0;i<NumberOfTrials*numVehicles;i++) {
			System.out.print(SGtoEPPS[i]+" ,");
		}
		System.out.println();

		System.out.println("The values used to compute the index are the ratio of RG actual lead time to EPPS");
		System.out.println("raw data for repeated game =");

		for (int i=0;i<NumberOfTrials*numVehicles;i++) {
			System.out.print(RGtoEPPS[i]+" ,");
		}
		System.out.println();

	}



	public static void printEventList() {

		System.out.println("==============================================");
		System.out.println("===============Printing Event List=================");
		System.out.println("==============================================");
		System.out.println("number of events="+eventList.size());
		//		System.out.println("current step=");


		for (int i=0;i<eventList.size();i++) {
			System.out.println(i+ " " +eventList.get(i));
		}

	}


}