package mypack;
import java.util.ArrayList;
import java.util.List;

public class Vehicle implements Comparable<Vehicle> {
	int currentStation;
	int originStation;
	int destinationStation;
	String status;
	//	double accumulatedProportionalShare;
	private double exPostProportionalShare; //the ex ante prop share for this convoy
	private double exAnteProportionalShare; //the ex ante prop share for this convoy
	private double remainingTimeToRotation;//the remaining time to rotation

	private Event rotation;

	double actual_SG_leadTime;
	double actual_RG_leadTime;

	double accumulatedRatio;

	public List convoys=new ArrayList<Convoy>();
	public Convoy currentConvoy;
	public List ratios=new ArrayList<Double>();
	private boolean isLeader;
	private boolean isLeaderRG;

	private double lastLeadStart;//to calculate remaining SG share in case of new arrival.
	private double lastLeadStartRG;//to calculate remaining RG in case of new arrival.

	boolean loopOnce;//for cases where destination == current to prevent immediate leave

	public double participationScore;
	public static int counter;
	private int index;
	public Vehicle() {
		index=counter++;
		currentStation=generateNewStation();
		status="Parked";
	}

	public Vehicle(int originStation,int destinationStation, int ID) {
		//index=counter++;
		this.originStation=originStation;
		this.destinationStation=destinationStation;
		currentStation=originStation;
		status="Parked";
		index=ID;
	}
	public int getIndex() {
		return index;
	}

	public int generateNewStation() {
		loopOnce=false;
		destinationStation=((int)(Test2.rand.nextDouble()*Test2.numStations));
		if(destinationStation==currentStation) {
			loopOnce=true;

		}
		return	destinationStation;
	}
	public void updateParticipationScore() {
		participationScore=Test2.rand.nextDouble();
		if(participationScore<Test2.participationProbability) {
			currentConvoy=new Convoy();
		}
	}
	public void stopAtStation(int destinationStationIndex) {
		//	System.out.println(" Vehicle "+index+" stopped at station "+destinationStationIndex);
		currentStation=destinationStationIndex;
		convoys.add(currentConvoy);
		//	System.out.println(" Vehicle "+index+" now participated in "+convoys.size()+" convoys");

		double addToRatios= currentConvoy.actual/currentConvoy.proportional;
		ratios.add (addToRatios);
		status="Parked";
	}

	public int compareTo(Vehicle other) {
		if (participationScore>=other.participationScore)
			return 1;
		else
			return -1;
	}
	
	public double getremainingTimeToRotation() {
		if(remainingTimeToRotation<0.00001) {
			return 0;
		}
		return remainingTimeToRotation;
	}

	public void setremainingTimeToRotation(double remainingTimeToRotation) {
		this.remainingTimeToRotation = remainingTimeToRotation;
	}

	public boolean isLeader( ) {
		return isLeader;
	}

	
	public void setLeader(boolean setLeader ,double time) {
		this.isLeader = setLeader;

		if(setLeader) {
			lastLeadStart=time;
		}
		else {
			updateActualLeadTimeSG(time);
		}
	}

	public boolean isLeaderRG( ) {
		return isLeaderRG;
	}

	public void setLeaderRG(boolean setLeader ,double time) {
		this.isLeaderRG = setLeader;

		if(setLeader) {
			lastLeadStartRG=time;
		}
		else {
			updateActualLeadTimeRG(time);
		}
	}

	public double getExAnteProportionalShare() {
		return exAnteProportionalShare;
	}

	public void setExAnteProportionalShare(double exAnteProportionalShare) {
		this.exAnteProportionalShare = exAnteProportionalShare;
		this.remainingTimeToRotation=exAnteProportionalShare;
	}

	private void updateActualLeadTimeSG(double time) {
		if(!Test2.dynamicAdjustmentOfTTR) {
		remainingTimeToRotation-=(time-lastLeadStart);
		}
		actual_SG_leadTime+=(time-lastLeadStart);
		if(Test2.debugMode) {	System.out.println("Vehicle "+ getIndex()+" updating SG lead time, to be "+ actual_SG_leadTime+" remainingTimeToRotation="+remainingTimeToRotation);
		}
	}

	public void updateActualLeadTimeRG(double time) {
		actual_RG_leadTime+=(time-lastLeadStartRG);
		if(Test2.debugMode) {System.out.println("Vehicle "+ getIndex()+" updating RG lead time, to be "+ actual_RG_leadTime);
		}
	}


	public double getExPostProportionalShare() {
		return exPostProportionalShare;
	}

	public void setExPostProportionalShare(double exPostProportionalShare) {
		this.exPostProportionalShare = exPostProportionalShare;
	}

	
	
	public Event getRotation() {
		return rotation;
	}

	public void setRotation(Event rotation) {
		this.rotation = rotation;
	}
	public void setRotationTime(double newTime) {
		this.rotation.setTime(newTime);
	}

	public String toString() {
		String s="Vehicle "+index;
		s+=", Origin station="+originStation;
		s+=", Destination station="+destinationStation;
		s+=", Availability period="+(destinationStation-originStation);
		s+=", EPPS="+exPostProportionalShare;
		s+=", EAPS="+exAnteProportionalShare;
		s+=", Actual SG lead time="+actual_SG_leadTime;
		s+=", Actual RG lead time="+actual_RG_leadTime;


		return s;
	}
	public void addToConvoy() {
		status="In Convoy";
	}


}
