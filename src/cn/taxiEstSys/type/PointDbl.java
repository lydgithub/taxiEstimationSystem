package cn.taxiEstSys.type;

//自定义的点类型
public class PointDbl {
	public double x;
	public double y;

	public PointDbl(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public String toString() {
		return y + " " + x;
	}

	public double distance2(PointDbl p) {
		return (p.x - this.x) * (p.x - this.x) + (p.y - this.y) * (p.y - this.y);
	}
}
