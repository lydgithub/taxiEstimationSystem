package cn.taxiEstSys.type;

/**
 * 自定义的时间类，要高效率地支持提取星期、分、秒等操作
 * 
 * @author koyo
 * 
 */
public class TimeString {
	public String rawTime;
	public int weekNum;
	public int hhmmss;

	// rawTime是　yyyyMMddhhmmss格式的
	public TimeString(String rawTime) {
		this.rawTime = rawTime;
		hhmmss = Integer.parseInt(rawTime.substring(rawTime.length() - 6));

		int day = Integer.parseInt(rawTime.substring(6, 8));
		if (rawTime.startsWith("2013")) {// 注意时间
			weekNum = (day + 5) % 7;
		} else {
			weekNum = (day + 2) % 7;
		}
	}

	public int minusHHMMSS(int thhmmss) {
		int ss = this.hhmmss % 100;
		int mm = (this.hhmmss % 10000 - ss) / 100;
		int hh = (this.hhmmss - mm * 100 - ss) / 10000;

		int tss = thhmmss % 100;
		int tmm = (thhmmss % 10000 - tss) / 100;
		int thh = (thhmmss - tmm * 100 - tss) / 10000;

		return (hh * 3600 + mm * 60 + ss) - (thh * 3600 + tmm * 60 + tss);
	}

	@Override
	public String toString() {
		return rawTime;
	}

}
