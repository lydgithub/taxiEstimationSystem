package cn.taxiEstSys.type;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//比赛用的那10个观察点的类
public class ObservPoint {
	public PointDbl pos;
	public TimeString observTime;
	public int direction;
	public String rawInput;
	public double runTime;

	public double Q;
	public double chanceOfSuc;
	public double waitingTime;// 等车时间，单位:秒

	public ObservPoint(double x, double y, String rawTime, int direction) {
		this.pos = new PointDbl(x, y);
		this.observTime = new TimeString(rawTime);
		this.direction = direction;
	}

	public ObservPoint(String line) throws ParseException {
		this.rawInput = line;

		String[] strs = line.split("\t");
		String rawPosition = strs[0].substring(1, strs[0].length() - 2);
		String rawObservTime = strs[2];// 注意下标顺序
		String rawDirection = strs[1];

		double x = Double.parseDouble(rawPosition.split(", ")[0]);
		double y = Double.parseDouble(rawPosition.split(", ")[1]);
		this.pos = new PointDbl(x, y);

		DateFormat dateFormat1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		DateFormat dateFormat2 = new SimpleDateFormat("yyyyMMddHHmmss");
		Date myDate = dateFormat1.parse(rawObservTime);
		this.observTime = new TimeString(dateFormat2.format(myDate));

		if (rawDirection.equals("自南向北")) {
			this.direction = 0;
		} else if (rawDirection.equals("自西向东")) {
			this.direction = 90;
		} else if (rawDirection.equals("自北向南")) {
			this.direction = 180;
		} else if (rawDirection.equals("自东向西")) {
			this.direction = 270;
		}
	}
}
