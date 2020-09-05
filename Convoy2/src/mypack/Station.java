package mypack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Station {
	public static int counter;
	private int index;
	public List<Vehicle> vehicles;
	public Station() {
		index=counter++;
		vehicles=new ArrayList<Vehicle>();
	}

	public List chooseParticipatingVehicleOrder() {
		List exitingVehicles=new ArrayList<Vehicle>();
		for(Vehicle v : vehicles){
			v.updateParticipationScore();
		}
		Collections.sort(vehicles);
		for(Vehicle v : vehicles){
			if(v.participationScore<Test2.participationProbability) {
			v.generateNewStation();
			exitingVehicles.add(v);
			}
	}
		vehicles.removeAll(exitingVehicles);
		return exitingVehicles;

}

	public boolean isEmpty() {
		return vehicles.isEmpty();
	}

	public int getIndex() {
		return index;
	}


public String toString() {
	String s="Station "+index;
	s+=" Has "+vehicles.size()+" vehicles parked";
	return s;
}
}
