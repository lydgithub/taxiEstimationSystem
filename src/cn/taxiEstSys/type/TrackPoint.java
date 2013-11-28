package cn.taxiEstSys.type;

import com.mongodb.DBObject;

//对应MongoDB中的一条记录，一个轨迹点
public class TrackPoint {
	public int taxiID;
	public TimeString time;
	public PointDbl p;
	public int direction;
	public double speed;

	public TrackPoint(DBObject row) {
		taxiID = (Integer) row.get("taxiId");
		time = new TimeString((String) row.get("date"));
		p = new PointDbl((Double) row.get("longitude"),
				(Double) row.get("latitude"));
		direction = (Integer) row.get("direction");
		speed = (Integer) row.get("speed");
	}

	public TrackPoint(int taxiID, TimeString time, PointDbl p, int direction,
			double speed) {
		this.taxiID = taxiID;
		this.time = time;
		this.p = p;
		this.direction = direction;
		this.speed = speed;
	}

	public TrackPoint(int taxiID, String rawTime, double x, double y,
			int direction, double speed) {
		this.taxiID = taxiID;
		this.time = new TimeString(rawTime);
		this.p = new PointDbl(x, y);
		this.direction = direction;
		this.speed = speed;
	}
}
