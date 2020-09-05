package mypack;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Collections;
import java.util.List;
public class Convoy {
	public List<Vehicle> vehicles=new ArrayList<Vehicle>(); //unsorted, represents actual location in convoy. used to determine next leader in rotations
	public List<Vehicle> sortedVehicles=new ArrayList<Vehicle>(); // sorted by leave, used to calculate ex ante prop share
	public List<Vehicle> RGVehicles=new ArrayList<Vehicle>(); // used to maintain the vehicle order which results form applying the repeated game mechanism on the same set of events 

	public Vehicle leader=null; // Single Game leader
	public Vehicle RGLeader=null;// Repeated Game leader

	public double actual;
	public double proportional;

	public String toString() {
		String s="Convoy has "+vehicles.size()+" participants";
		return s;
	}

	public boolean addVehicle(Vehicle v) {
		RGVehicles.add(v);
		return vehicles.add(v);

	}
	public boolean removeVehicle(Vehicle v) {
		RGVehicles.remove(v);
		return vehicles.remove(v);
	}
	public int getLength() {
		return vehicles.size();
	}
	public Vehicle getLeader() {
		return leader;
	}
	public void setLeader(Vehicle v) {
		leader=v;
	}
	public Vehicle getLeaderRG() {
		return RGLeader;
	}
	public void setLeaderRG(Vehicle v) {
		RGLeader=v;
	}

	public void calculateExPostProportionalShares(Event currentEvent,Event previousEvent) {
		//calculates the EPPS for all participants for a single segment
		if(previousEvent==null || previousEvent.getTime()==currentEvent.getTime()) {//This means that the currentEvent is the first so there is no segment, or seg_size ==0
			if(Test2.debugMode) {		System.out.println("seg_size = 0 so not calculating EPPS");}//ex ante proportional share
		}
		else {
			sortVehiclesByDepartue();
			int n_seg=sortedVehicles.size();//number of vehicles in each segment.

			if(n_seg==0) {
				if(Test2.debugMode) {	System.out.println("No vehicles in this segment, so not calculating EPPS");}//ex ante proportional share
				return;
			}
			double seg_size = currentEvent.getTime()-previousEvent.getTime();
			double propShareForSegnment=seg_size/n_seg;		

			if(Test2.debugMode) {System.out.println("EPPS="+ propShareForSegnment+" for segment<"+previousEvent.getTime()+ ","+currentEvent.getTime()+">  n_seg="+n_seg);//ex ante proportional share
			}
			for(Vehicle v :sortedVehicles) {
				if(currentEvent.getTime()!=v.originStation) {//if arrival event then reduce n_seg by 1 (new arrival which did not participate in the segment) this should never happen since I call calc EPPS before inserting
					v.setExPostProportionalShare(v.getExPostProportionalShare()+propShareForSegnment);
				}
			}
		}

	}

	public Event calculateExAnteProportionalShares(Vehicle newVehicle, Event currentEvent) {//The reason this function returns a rotation event is that in some cases the rotation is not at the end of the proportional share. For example when a vehicle has extra time on its own, and we count that in the EAPS.

		double timeToRotation=0;
		sortVehiclesByDepartue();
		int n_seg=sortedVehicles.size();//number of vehicles in each segment.
		double seg_start=newVehicle.originStation;
		if(Test2.debugMode) {		System.out.println("calculating EAPS for Vehicle "+newVehicle.getIndex()+ " n_seg="+n_seg);//ex ante proportional share
		}
		for(Vehicle existingVehicle :sortedVehicles) {
			if(existingVehicle.getIndex()==newVehicle.getIndex()) {
				//			if(Test2.debugMode) {	System.out.println("No other vehicles, EAPS is the rest of the availability period ");}
			}
			if(existingVehicle.destinationStation > newVehicle.destinationStation) {
				if(Test2.debugMode) {System.out.println("Break, next vehicles leave after the new arrival leaves");}
				break;
			}
			double seg_end=existingVehicle.destinationStation;

			double seg_size = (seg_end-seg_start);
			seg_start=seg_end;
			double propShareForSegnment=seg_size/n_seg;		

			if(Test2.debugMode) {	System.out.println("seg_size= "+seg_size+ " n_seg="+n_seg+" EAPS for this segnment="+propShareForSegnment);
			}
			if(n_seg>1) {
				timeToRotation+=propShareForSegnment;
			}
			if(Test2.debugMode) {System.out.println("adding " +propShareForSegnment+" to EAPS share of "+newVehicle.getIndex()+" current EAPS share= "+newVehicle.getExAnteProportionalShare());
			}
			newVehicle.setExAnteProportionalShare(newVehicle.getExAnteProportionalShare()+propShareForSegnment);

			//			}
			//this code is required for updating the first arrival's EAPS not to be its entire availability period!!!!!!!
			if(vehicles.size()==2) {
				System.out.println("reducing " +propShareForSegnment+" from EAPS of "+vehicles.get(0).getIndex());
				vehicles.get(0).setExAnteProportionalShare(vehicles.get(0).getExAnteProportionalShare()-propShareForSegnment);

				///This code is needed to prevent the SG>EAPS error by forcing the agent with the earlier leave time to lead first
				if(vehicles.get(0).destinationStation<newVehicle.destinationStation) {
					System.out.println("Setting "+vehicles.get(0).getIndex()+" to be first leader since it leaves first");
					Vehicle firstLeader=vehicles.get(0);
					Vehicle secondLeader=newVehicle;
					vehicles.remove(secondLeader);
					vehicles.add(0,secondLeader);
					firstLeader.setLeader(true,currentEvent.getTime());
					secondLeader.setLeader(false,currentEvent.getTime());
					setLeader(firstLeader);
					Event rotation=new Event(3,firstLeader.getIndex(),currentEvent.getTime()+timeToRotation);
					return rotation;

				}

			}
			n_seg--;

		}
		if(Test2.debugMode) {System.out.println("Vehicle "+newVehicle.getIndex() +" EAPS share= "+newVehicle.getExAnteProportionalShare());
		}
		Event rotation=new Event(3,newVehicle.getIndex(),currentEvent.getTime()+timeToRotation);
		return rotation;

	}




	public Event calculateEAPSandTTR(Vehicle newVehicle, Event currentEvent) {//The reason this function returns a rotation event is that in some cases the rotation is not at the end of the proportional share. For example when a vehicle has extra time on its own, and we count that in the EAPS.
		//calculates EAPS
		//assumes that vehicles are sorted by departure
		//TTR = Time To Rotation , which is not the same as EAPS in case the agent has solitary time		
		//if the agents leave time is the earliest, it enters from the front and becomes the leader, in this case the rotation event is used
		//if the agent is the last to leave, it enters from the back, and has extra solitary time at the end of the convoy, in this case, the EAPS is not the same as the TTR 
		double timeToRotation=0;
		//sortVehiclesByDepartue();
		int n_seg=vehicles.size();//number of vehicles in each segment.
		double seg_start=newVehicle.originStation;
		double seg_end;
		double seg_size;
		if(Test2.debugMode) {		
			System.out.println("calculating EAPS for Vehicle "+newVehicle.getIndex()+ " n_seg="+n_seg);//ex ante proportional share
		}
		//	for(Vehicle existingVehicle : vehicles) {  //the order is reversed
		Vehicle existingVehicle;
		for( int i= vehicles.size()-1;i>=vehicles.indexOf(newVehicle);i--) { //descending
			existingVehicle=vehicles.get(i);
			seg_end=existingVehicle.destinationStation;
			seg_size = (seg_end-seg_start);
			double propShareForSegnment=seg_size/n_seg;		

			if(Test2.debugMode) {	System.out.println("seg <"+seg_start+","+ seg_end+"> size= "+seg_size+ " n_seg="+n_seg+" EAPS for this segnment="+propShareForSegnment);
			}
			seg_start=seg_end;

			timeToRotation+=propShareForSegnment;

			if(Test2.debugMode) {System.out.println("adding " +propShareForSegnment+" to EAPS share of "+newVehicle.getIndex()+" current EAPS share= "+newVehicle.getExAnteProportionalShare());
			}
			newVehicle.setExAnteProportionalShare(newVehicle.getExAnteProportionalShare()+propShareForSegnment);


			n_seg--;

		}
		if(Test2.debugMode) {System.out.println("Vehicle "+newVehicle.getIndex() +" EAPS share= "+newVehicle.getExAnteProportionalShare());
		}
		Event rotation=new Event(3,newVehicle.getIndex(),currentEvent.getTime()+timeToRotation);
		return rotation;

	}


	public Event calculateEAPSandTTRwithDynamicAdjustment(Vehicle newVehicle, Event currentEvent) {
		//This method  calculates EAPS of new entrant
		//This method  calculates TTR of new entrant - this is the EAPS of the non solitary segments
		//This method adjusts the remainingTimeToRotation of existing vehicles whenever a new one enters
		//assumes that vehicles are sorted by departure
		//reduces the  remainingTimeToRotation for existing agents (which have not finished leading) by (new agent's contribution)/(number of existing vehicles with remaining lead time)
		// For each segment, The new remainingTimeToRotation will be = min{ remainingTimeToRotation- 1/n / (1/(n-1-finished)), 0}

		double timeToRotation=0; //for new agent
		int n_seg=vehicles.size();//number of vehicles in each segment.
		double seg_start=newVehicle.originStation;
		double seg_end;
		double seg_size;
		sortVehiclesByDepartue();


		if(Test2.debugMode) {		
			System.out.println("Dynamically adjusting TTR following Vehicle "+newVehicle.getIndex()+ " entrance, n_seg="+n_seg);
		}
		//	for(Vehicle existingVehicle : vehicles) {  //the order is reversed
		Vehicle existingVehicle;

		//go over every segment in newVehicle's availability period  starting from [newVehicle's arrival, first leave] until newVehicle's leave
		for( int i= 0;i<=sortedVehicles.indexOf(newVehicle);i++) { //ascending
			existingVehicle=sortedVehicles.get(i);
			seg_end=existingVehicle.destinationStation;
			seg_size = (seg_end-seg_start);
			double propShareForSegnment=seg_size/n_seg;		

			if(Test2.debugMode) {	
				System.out.println("existingVehicle="+existingVehicle.getIndex()+", located at position "+i+" its destination is at "+existingVehicle.destinationStation);

				System.out.println("seg <"+seg_start+","+ seg_end+"> size= "+seg_size+ " n_seg="+n_seg+" EAPS for this segnment="+propShareForSegnment);
			}
			seg_start=seg_end;

			timeToRotation+=propShareForSegnment;

			if(Test2.debugMode) {System.out.println("adding " +propShareForSegnment+" to EAPS share of "+newVehicle.getIndex()+" current EAPS share= "+newVehicle.getExAnteProportionalShare());
			}
			newVehicle.setExAnteProportionalShare(newVehicle.getExAnteProportionalShare()+propShareForSegnment);

			//in order to dynamically adjust the remainingTTD of the vehicles in the segment which have yet to finish their share, we need to find how many are there and adjust their ttr accordingly
			int numberOfVehiclesThatFinishedLeading= getNumVehiclesWithZeroRemainingTTR(existingVehicle);

			//this is the adjustment value that each splitter (vehicles that still need to lead) will get
			int numberOfSplitters=n_seg-1-numberOfVehiclesThatFinishedLeading;
			if(numberOfSplitters>0) {
				double adjustment=propShareForSegnment/(numberOfSplitters);
				if(Test2.debugMode) {System.out.println("adjustmnet=" +adjustment+" will be reduced from the TTR of "+numberOfSplitters+" vehicles that didn't finish leading");}

				//going over vehicles from first until exsistingVehicle and adjusting
	//			for( int j=0 ;j<=sortedVehicles.indexOf(existingVehicle);j++) { //
	//				for( int j=sortedVehicles.size()-1;j>= sortedVehicles.indexOf(existingVehicle) ;j--) { 
						for( int j=sortedVehicles.indexOf(existingVehicle);j<=sortedVehicles.size()-1;j++) { 

					Vehicle toAdjust=sortedVehicles.get(j);
					
					if(toAdjust.getremainingTimeToRotation()>0 && toAdjust.getIndex()!=newVehicle.getIndex()) {
						if(Test2.debugMode) {		
							System.out.println("Vehicle "+ toAdjust.getIndex()+" TTR is"+toAdjust.getremainingTimeToRotation()+ " adjustment= "+adjustment);//
						}
						toAdjust.setremainingTimeToRotation(Math.max(toAdjust.getremainingTimeToRotation()-adjustment,0));
						if(Test2.debugMode) {		
							System.out.println("Dynamically adjusting TTR of vehicle"+toAdjust.getIndex()+" to be "+ toAdjust.getremainingTimeToRotation()+ " following Vehicle "+newVehicle.getIndex()+ " entrance, n_seg="+n_seg+" numberOfVehiclesThatFinishedLeading="+numberOfVehiclesThatFinishedLeading);//ex ante proportional share
						}
						if(Test2.debugMode) {		
							System.out.println("The rotation event of vehicle"+toAdjust.getIndex()+" is "+ toAdjust.getRotation());
						}
						
						//adjust the rotation time of the leader
//						if(toAdjust.isLeader()) {
//							double newRotationTime=toAdjust.getRotation().getTime()-adjustment;
//							if(newRotationTime<currentEvent.getTime()) {
//								newRotationTime=currentEvent.getTime();
//							}
//							toAdjust.getRotation().setTime(newRotationTime);
//							
//							if(Test2.debugMode) {		
//								System.out.println("Dynamically adjusting rotation event time of vehicle"+toAdjust.getIndex()+" to be "+ toAdjust.getRotation().getTime());//ex ante proportional share
//							}
//						}
						
					}
				}
			}
			else {
				if(Test2.debugMode) {System.out.println("No splitters for this segment");}
			}
			//reduce number of vehicles in segment before continuing to next segment
			n_seg--;
		}
		if(Test2.debugMode) {System.out.println("Vehicle "+newVehicle.getIndex() +" EAPS share= "+newVehicle.getExAnteProportionalShare());
		}

		//Create rotation event for newVehicle in case it is the leader 
		Event rotation=new Event(3,newVehicle.getIndex(),currentEvent.getTime()+timeToRotation);
		return rotation;

	}


	private int getNumVehiclesWithZeroRemainingTTR(Vehicle existingVehicle) { // to calculate adjustment to TTR 
		//returns the number of vehicles that have not finished leading until existingVehicle's departure
		int counter=0;
		for( Vehicle v : vehicles) { 
			if(v.getremainingTimeToRotation()==0) {
				counter++;
			}
			if(existingVehicle==v) {
				return counter;

			}
		}
		return counter;
	}


	public void addRotationEvent(Event rotationEvent) {

	//	if(vehicles.size()>1) {
			insertEventSorted(rotationEvent);
			if(Test2.debugMode) {	System.out.println("added new rotation event at time " + rotationEvent.getTime() +" for Vehicle "+rotationEvent.getVID() );
	//		}
		}
	}
	public void insertEventSorted(Event e){
		for(int i=0; i<Test2.eventList.size();i++) {
			if(e.getVID()==Test2.eventList.get(i).getVID()&&e.getType()==3&&Test2.eventList.get(i).getType()==3&&(Test2.eventList.get(i).getTime()==e.getTime())) {
				System.out.print("Error- Adding duplicate rotation event= "+e);
				System.out.print("Do you want to proceed?");
				Scanner a = new Scanner(System.in);  // Create a Scanner object
				String s = a.nextLine();  // Read user input
			}

			if (e.getTime()<Test2.eventList.get(i).getTime()) {//making sure the rotation is inserted after events with similar time 
				Test2.eventList.add(i,e);
				break;
			}
		}
	}
	public void sortVehiclesByDepartue() { //ascending
		sortedVehicles.clear();
		for(Vehicle v: vehicles) {
			sortedVehicles.add(v);
		}
		boolean sorted = false;
		Vehicle temp;
		while(!sorted) {
			sorted = true;
			for (int i = 0; i < sortedVehicles.size() - 1; i++) {
				if (sortedVehicles.get(i).destinationStation > sortedVehicles.get(i+1).destinationStation) {
					temp = sortedVehicles.get(i+1);
					sortedVehicles.remove(i+1);
					sortedVehicles.add(i,temp);
					sorted = false;
				}
			}
		}
		//	System.out.println("Vehicles sorted by destination ");
	}

	//	public void createConvoyRG() {
	//		RGVehicles.clear();
	//		for(Vehicle v: vehicles) {
	//			RGVehicles.add(v);
	//		}
	//	}

	public void updateLeader(Event currentEvent) {
		Vehicle previousLeader=getLeader();
		if(previousLeader!=null) {
			previousLeader.setLeader(false,currentEvent.getTime());// this also updates remaining SG proportional share for previous leader, 
			if(Test2.debugMode) {	System.out.print(" previous leader="+previousLeader.getIndex());}
		}
		else {
			if(Test2.debugMode) {System.out.print(" no previous leader. ");}
		}
		Vehicle newVehicle=currentEvent.getV();
		vehicles.add(newVehicle);
		newVehicle.setLeader(true,currentEvent.getTime());	
		setLeader(newVehicle);
		if(Test2.debugMode) {System.out.println(" new leader="+currentEvent.getVID());}

	}
	public boolean insertSortedByDeparture(Event currentEvent) {
		//this sorts the vehicles in decending order with the last to leave at position 0 and the first to leave at position size()-1
		Vehicle newVehicle=currentEvent.getV();
		boolean inserted=false;
		boolean firstToLeave=false;// indicates that this new vehicle will be the leader
		if(Test2.debugMode) {	System.out.println(" Inserting newVehicle="+newVehicle.getIndex()+" leave_time="+newVehicle.destinationStation);}

		for (int i = 0; i < vehicles.size(); i++) {
			if (newVehicle.destinationStation >= vehicles.get(i).destinationStation ) { //tie breaking - the vehicle that was longer in the convoy will lead first
				if(Test2.debugMode) {	System.out.println(" Inserting newVehicle="+newVehicle.getIndex()+" at position="+i);}
				vehicles.add(i,newVehicle);		
				inserted=true;
				return firstToLeave;	//false - not leader
			}
		}
		if(!inserted) {
			updateLeader(currentEvent);
			firstToLeave=true;	
		}
		return firstToLeave;	

	}
	
	
	public boolean insertSortedByDepartureAfterRotatedVehicles(Event currentEvent) {
		//this sorts the vehicles in decending order with the last to leave at position 0 and the first to leave at position size()-1
		Vehicle newVehicle=currentEvent.getV();
		boolean inserted=false;
		boolean firstToLeave=false;// indicates that this new vehicle will be the leader
		if(Test2.debugMode) {	System.out.println(" Inserting newVehicle="+newVehicle.getIndex()+" leave_time="+newVehicle.destinationStation);}

		for (int i = 0; i < vehicles.size(); i++) {
			if(vehicles.get(i).getremainingTimeToRotation()==0) {
				continue;
			}

			if (newVehicle.destinationStation >= vehicles.get(i).destinationStation ) { //tie breaking - the vehicle that was longer in the convoy will lead first
				if(Test2.debugMode) {	System.out.println(" Inserting newVehicle="+newVehicle.getIndex()+" at position="+i);}
				vehicles.add(i,newVehicle);		
				inserted=true;
				return firstToLeave;	//false - not leader
			}
		}
		if(!inserted) {
			updateLeader(currentEvent);
			firstToLeave=true;	
		}
		return firstToLeave;	

	}
	
	public void updateLeaderRG(Event currentEvent) {
		Vehicle previousLeader=getLeaderRG();
		if(previousLeader!=null) {
			previousLeader.setLeaderRG(false,currentEvent.getTime());// this also updates remaining RG proportional share for previous leader, 
			if(Test2.debugMode) {	System.out.print(" previous RG leader="+previousLeader.getIndex());}
		}
		else {
			if(Test2.debugMode) {	System.out.print(" no previous RG leader. ");}
		}
		Vehicle newVehicle=currentEvent.getV();
		RGVehicles.add(newVehicle);
		newVehicle.setLeaderRG(true,currentEvent.getTime());	
		setLeaderRG(newVehicle);
		if(Test2.debugMode) {System.out.println(" new RG leader="+currentEvent.getVID());		}
	}
	public void printConvoy() {
		System.out.println("======printing convoy =======");
		for(int i=0;i<vehicles.size();i++) {
			System.out.print(" ("+i+"), vid="+vehicles.get(i).getIndex()+ " TTR="+vehicles.get(i).getremainingTimeToRotation() );
			if(vehicles.get(i).isLeader()) { System.out.print(" isLeader=true ");
			if(this.leader.getIndex()!=vehicles.get(i).getIndex()) { System.out.println("!!! leader mismatch !!! convoy.leader="+leader.getIndex());}
			}
		}
		System.out.println();		
	}

}