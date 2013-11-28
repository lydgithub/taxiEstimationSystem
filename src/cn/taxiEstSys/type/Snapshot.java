package cn.taxiEstSys.type;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;

import cn.taxiEstSys.util.BitAlgs;
import cn.taxiEstSys.util.PARAMS;

//一张快照的定义
public class Snapshot {
	public long segID;
	public byte side;// 正向为1
	public short shotTimeIdx;// 一星期内的第几张快照

	public int multiN;// 曝光时间内出现的总车次数，不去重
	public int N;// 去重后的N
	public double V;// 求所有车的平均速度，用的累加器；最后也用作保存平均速度结果的容器。
	public HashSet<Integer> allTaxiIDs;// 所有路过的车的ID集合

	public double Q;// 最后模型中的3分钟内车流量

	private static ArrayList<Integer> shotTimeIdxTable = null;

	private static void initShotTimeTable() {
		try {
			shotTimeIdxTable = new ArrayList<Integer>();
			BufferedReader r = new BufferedReader(new InputStreamReader(
					new FileInputStream(PARAMS.WORK_SPACE_DIR
							+ PARAMS.TIME_IDX_FILE_PATH)));
			String line = r.readLine();
			while (line != null) {
				shotTimeIdxTable.add(Integer.parseInt(line));
				line = r.readLine();
			}
			PARAMS.NUM_OF_SHOTS＿IN_A_DAY = (short) shotTimeIdxTable.size();
			shotTimeIdxTable.add(240000);// 尾上加一个哨兵，简化溢出代码；注意shotTimeIdxTable.size不能再用了
		} catch (FileNotFoundException e) {
			System.err.println("ShootTimeTable does not exist!");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("ShootTimeTable damaged!");
			e.printStackTrace();
		}
	}

	// 把一个时刻，映射到索引（即一周中的第几张快照）
	public static short timeString2timeIdx(TimeString ts) {
		if (shotTimeIdxTable == null) {
			initShotTimeTable();
		}

		int time = ts.hhmmss;
		short idx = 0;
		for (idx = 0; idx <= PARAMS.NUM_OF_SHOTS＿IN_A_DAY - 1; ++idx) {
			if (shotTimeIdxTable.get(idx) <= time
					&& (shotTimeIdxTable.get(idx) + PARAMS.SHOT_DURATION / 60
							* 100 >= time)) {
				return (short) (idx + ts.weekNum * PARAMS.NUM_OF_SHOTS＿IN_A_DAY);
			}
		}
		return -1;
	}

	// 类似前面的函数，求不晚于这个时刻的最近的一个快照索引
	public static short timeString2preTimeIdx(TimeString ts) {
		if (shotTimeIdxTable == null) {
			initShotTimeTable();
		}

		int time = ts.hhmmss;
		short idx = 0;
		for (idx = 0; idx <= PARAMS.NUM_OF_SHOTS＿IN_A_DAY - 1; ++idx) {
			int S_ss = shotTimeIdxTable.get(idx) % 100;
			int S_mm = (shotTimeIdxTable.get(idx) % 10000 - S_ss) / 100;
			int S_hh = (shotTimeIdxTable.get(idx) - S_mm * 100 - S_ss) / 10000;
			int S = S_hh * 3600 + S_mm * 60 + S_ss;

			int E_ss = shotTimeIdxTable.get(idx + 1) % 100;
			int E_mm = (shotTimeIdxTable.get(idx + 1) % 10000 - E_ss) / 100;
			int E_hh = (shotTimeIdxTable.get(idx + 1) - E_mm * 100 - E_ss) / 10000;
			int E = E_hh * 3600 + E_mm * 60 + E_ss;

			int T_ss = time % 100;
			int T_mm = (time % 10000 - T_ss) / 100;
			int T_hh = (time - T_mm * 100 - T_ss) / 10000;
			int T = T_hh * 3600 + T_mm * 60 + T_ss;

			if (S + PARAMS.SHOT_DURATION / 2 <= T + PARAMS.WAITING_TIME / 2
					&& E + PARAMS.SHOT_DURATION / 2 >= T + PARAMS.WAITING_TIME
							/ 2) {// 最后有个24:00:00尾哨兵
				return (short) (idx + ts.weekNum * PARAMS.NUM_OF_SHOTS＿IN_A_DAY);
			}
		}

		// should not reach here
		return -1;
	}

	// 索引到时刻的逆函数，返回hhmmss
	public static int timeIdx2hhmmss(int timeIdx) {
		if (shotTimeIdxTable == null) {
			initShotTimeTable();
		}

		return shotTimeIdxTable.get(timeIdx % PARAMS.NUM_OF_SHOTS＿IN_A_DAY);
	}

	public Snapshot(long segID, byte side, short timeIdx) {
		this.segID = segID;
		this.side = side;
		this.shotTimeIdx = timeIdx;

		multiN = 0;
		N = 0;
		V = 0;
		allTaxiIDs = new HashSet<Integer>();
	}

	/**
	 * 覆盖默认的toString()，这个函数用来帮助把模型写到文本文件
	 */
	@Override
	public String toString() {
		long modelKey = BitAlgs
				.getModelKeyByConcating(segID, side, shotTimeIdx);// 拼接出模型的键

		return modelKey + "," + Q + "\n";
	}

	// public static void main(String[] args) {
	// timeString2timeIdx(new TimeString("20121101233200"));
	// timeString2preTimeIdx(new TimeString("20121101233500"));
	// }
}
