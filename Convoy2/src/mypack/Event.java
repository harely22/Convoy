package mypack;

public class Event implements Comparable<Event> {
	private int type; //1=arrive,2=leave, 3=rotate,
	private int VID; // vehicle ID
	private Vehicle v; // vehicle 
	private double time;
	public Event(int type, int VID, double time) {
		if(time-Math.ceil(time)<0.0001) {
			time=Math.ceil(time);
		}
		
		
		this.type=type;
		this.VID=VID;
		this.time=time;
	}
	public Event(int type, Vehicle v, double time) {
		this.type=type;
		this.v=v;
		this.time=time;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int getVID() {
		return VID;
	}

	public double getTime() {
		return time;
	}
	public void setTime(double time) {
		if(time-Math.ceil(time)<0.0001) { //round to prevent repeated rotation error 13.999999999999
			time=Math.ceil(time);
		}
		this.time = time;
	}
	@Override
	public int compareTo(Event o) {
		if(this.time>o.getTime())
			return 1;
		else
			return -1;
	}
	@Override
	public String toString() {
		String typeName="arrive";
		if(type==2) {typeName="leave";}
		else if(type==3){typeName="rotate";}
		return "Event [type=" + type + " " + typeName+", VID=" + VID + ", time=" + time + "]";
	}
	public Vehicle getV() {
		return v;
	}
	public void setV(Vehicle v) {
		this.v = v;
	}

}
