package cn.taxiEstSys.type;

import cn.taxiEstSys.util.GIS.SpatialAlgs;

//路段属性表项
public class SegProp {
	public long SegID;
	public PointDbl S;
	public PointDbl E;
	public double length;
	// public double width;
	public int direction;

	public SegProp(long segID, double length, double Sx, double Sy, double Ex,
			double Ey) {
		SegID = segID;
		this.length = length;
		S = new PointDbl(Sx, Sy);
		E = new PointDbl(Ex, Ey);
		direction = SpatialAlgs.getDegreeBySE(S, E);
	}
}
