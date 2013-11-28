package cn.taxiEstSys.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import cn.taxiEstSys.type.ObservPoint;
import cn.taxiEstSys.type.Snapshot;
import cn.taxiEstSys.util.BitAlgs;
import cn.taxiEstSys.util.PARAMS;
import cn.taxiEstSys.util.GIS.ModifyOffset;
import cn.taxiEstSys.util.GIS.SpatialAlgs;
import cn.taxiEstSys.util.math.ChanceAndWaitingTimeCalculation;

/**
 * 从文本文件中读取信息，重建模型；并用模型来解答问题
 * 
 * @author koyo
 * 
 */
public class UseModel_ESRI {
	public static ArrayList<ObservPoint> ops = null;// 最终评测时用的10个观察点
	// 复原得到的模型，跟建模时的稍有不同；去除了一些不需要的中间过程字段
	private static ArrayList<HashMap<Long, Double>> models = new ArrayList<HashMap<Long, Double>>();
	private static HashMap<Long, Double> model = null;
	private static final int MODEL_COUNT = 1;

	private static Logger logger = Logger.getLogger(UseModel_ESRI.class);

	public static void main(String[] args) throws IOException, ParseException {

		PropertyConfigurator.configure("etc/log4j.Properties");

		PARAMS.init();
		// 因为后面要求人的打车方向与路向的顺逆，分两侧讨论； 所以，也会用到路网中路段的角度属性。
		// 这里就直接重用了BuildModel类中的public static方法。 我对设计模式不太熟，这个复用的设计确实很恶心；以后有空再改。
		BuildModel.prepareRoadNetworkProps();// 建立路网属性表
		SpatialAlgs.initESRI();

		// for (int i = 0; i <= MODEL_COUNT - 1; ++i) {
		// readModelFromTxt(PARAMS.MODEL_FILE_PATH, i);// 从文本文件中读取模型
		// }

		for (int i = 3; i <= 3; ++i) {
			readModelFromTxt(PARAMS.MODEL_FILE_PATH, i);// 从文本文件中读取模型
		}

		readObservPoints(PARAMS.OP_FILE_PATH);// ...从大赛方的API读取观察点
		calcAnswer();// 计算答案；写到观察点的Q、chanceOfSuc和watingTime属性中
		outputAnswer(PARAMS.RESULT_FILE_PATH);// 把答案输出到屏幕上
	}

	private static void readObservPoints(String OPsFilePath)
			throws IOException, ParseException {
		ops = new ArrayList<ObservPoint>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(OPsFilePath)));

		reader.readLine();// 第一行是表头，去掉
		String line = reader.readLine();
		while (line != null) {
			ops.add(new ObservPoint(line));
			line = reader.readLine();
		}
		reader.close();
	}

	private static void readModelFromTxt(String modelFilePath, int modelIdx)
			throws IOException {
		if (modelIdx == 0) {
			modelFilePath = modelFilePath.replace("X", "1-7");
		} else if (modelIdx == 1) {
			modelFilePath = modelFilePath.replace("X", "8-14");
		} else if (modelIdx == 2) {
			modelFilePath = modelFilePath.replace("X", "15-21");
		} else if (modelIdx == 3) {
			modelFilePath = modelFilePath.replace("X", "22-28");
		} else {
			logger.error("Wrong model index, starting from 0");
			return;
		}

		logger.info("Starting restore model from model file ---- "
				+ modelFilePath);

		model = new HashMap<Long, Double>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(modelFilePath)));

		String line = reader.readLine();
		while (line != null) {
			/**
			 * 拆开成键、值；复习一下建模时的Snapshoot.toString()函数源码就明白了
			 * (SegID_side_timeIdx)=>(Q)
			 */
			String[] strs = line.split(",");

			Long modelKey = Long.parseLong(strs[0]);
			Double Q = Double.parseDouble(strs[1]);
			model.put(modelKey, Q);

			line = reader.readLine();
		}
		models.add(model);
		reader.close();

		logger.info("Model restored from file: " + modelFilePath);
	}

	private static short[] getMultiIdxes(short shotTimeIdx) {
		int weekNum = shotTimeIdx / PARAMS.NUM_OF_SHOTS＿IN_A_DAY;
		short monIdx = (short) (shotTimeIdx % PARAMS.NUM_OF_SHOTS＿IN_A_DAY);
		if (weekNum <= 4) {
			return new short[] { monIdx,
					(short) (monIdx + PARAMS.NUM_OF_SHOTS＿IN_A_DAY),
					(short) (monIdx + 2 * PARAMS.NUM_OF_SHOTS＿IN_A_DAY),
					(short) (monIdx + 3 * PARAMS.NUM_OF_SHOTS＿IN_A_DAY),
					(short) (monIdx + 4 * PARAMS.NUM_OF_SHOTS＿IN_A_DAY) };
		} else {
			return new short[] {
					(short) (monIdx + 5 * PARAMS.NUM_OF_SHOTS＿IN_A_DAY),
					(short) (monIdx + 6 * PARAMS.NUM_OF_SHOTS＿IN_A_DAY) };
		}
	}

	private static void calcAnswer() {
		logger.info("Starting calc answers");

		for (ObservPoint op : ops) {
			long start = System.currentTimeMillis();

			op.pos = ModifyOffset.s2c(op.pos);
			long segID = SpatialAlgs.getSegID_by_Position_ESRI(op.pos);// 再次调用老钱的算法，求路段ID

			byte side = SpatialAlgs.judgeSameDirection(op.direction,// 再次调用歌神的算法，求顺逆向
					BuildModel.roadNetworkProp.get(segID).direction, 90);

			// 记观察点的时间为t 求小于或等于t的最近的快照时间索引
			short rawPreShotTimeIdx = Snapshot
					.timeString2preTimeIdx(op.observTime);
			double sumQ_over_weeks = 0;
			for (int modelIdx = 0; modelIdx <= MODEL_COUNT - 1; ++modelIdx) {
				model = models.get(modelIdx);

				ArrayList<Double> Qs = new ArrayList<Double>();
				for (short preShotTimeIdx : getMultiIdxes(rawPreShotTimeIdx)) {

					long modelKey0 = BitAlgs.getModelKeyByConcating(segID,
							side,
							(short) (preShotTimeIdx > 0 ? preShotTimeIdx - 1
									: preShotTimeIdx));

					long modelKey1 = BitAlgs.getModelKeyByConcating(segID,
							side, preShotTimeIdx);
					// 简单地加1是不好的，可能溢出到下一周的边界
					long modelKey2 = BitAlgs
							.getModelKeyByConcating(
									segID,
									side,
									(short) ((preShotTimeIdx + 1 > PARAMS.NUM_OF_SHOTS＿IN_A_DAY * 7 - 1) ? (PARAMS.NUM_OF_SHOTS＿IN_A_DAY * 7 - 1)
											: preShotTimeIdx + 1));

					long modelKey3 = BitAlgs
							.getModelKeyByConcating(
									segID,
									side,
									(short) ((preShotTimeIdx + 2 > PARAMS.NUM_OF_SHOTS＿IN_A_DAY * 7 - 1) ? (PARAMS.NUM_OF_SHOTS＿IN_A_DAY * 7 - 1)
											: (preShotTimeIdx + 2)));

					int deltaT = op.observTime.minusHHMMSS(Snapshot
							.timeIdx2hhmmss(preShotTimeIdx))
							+ PARAMS.WAITING_TIME
							/ 2
							- PARAMS.SHOT_DURATION
							/ 2;
					// 注意，只需要分钟的后一位，且取的时间是中点时刻
					double Q1 = model.containsKey(modelKey1) ? model
							.get(modelKey1) : 0;
					double Q2 = model.containsKey(modelKey2) ? model
							.get(modelKey2) : 0;
					double Q_inner = deltaT * (Q2 - Q1) / 600 + Q1;

					deltaT += 600;// t0->tx
					double Q0 = model.containsKey(modelKey0) ? model
							.get(modelKey0) : 0;
					double Q3 = model.containsKey(modelKey3) ? model
							.get(modelKey3) : 0;
					double Q_outer = deltaT * (Q3 - Q0) / 1800 + Q0;

					if ((preShotTimeIdx - 1) % PARAMS.NUM_OF_SHOTS＿IN_A_DAY == 0) {
						Qs.add(Q_inner);
					} else {
						Qs.add(0.75 * Q_inner + 0.25 * Q_outer);
					}
				}

				double sum = 0;
				for (double Q : Qs) {
					sum += Q;
				}

				if (sum < 0) {
					logger.error("sum<0");
					return;
				}
				sumQ_over_weeks += sum / Qs.size();
			}

			op.Q = sumQ_over_weeks / MODEL_COUNT;
			// 调用阿黄的算法求打车概率和预计等待时间
			op.chanceOfSuc = ChanceAndWaitingTimeCalculation
					.getChanceOfSucByQ(op.Q);
			op.waitingTime = ChanceAndWaitingTimeCalculation
					.getWatingTimeByQ(op.Q);// 等车时间单位是秒（最后再处理成分钟），可取小数

			op.runTime = (System.currentTimeMillis() - start) / 1000;
		}

		logger.info("Calculation finished");
	}

	private static void outputAnswer(String resultFilePath) throws IOException {
		FileWriter fwResult = new FileWriter(new File(resultFilePath));
		fwResult.write("用户位置\t车流方向\t当前时间\t打到车的概率\t平均等待时间(分钟)\t运行时间(秒)\r\n");

		for (ObservPoint op : ops) {
			StringBuilder sb = new StringBuilder();
			sb.append(op.rawInput).append("\t")
					.append(PARAMS.FORMAT_CHANCE.format(op.chanceOfSuc))
					.append("\t");

			if (op.waitingTime > PARAMS.MAX_WAITING_TIME) {
				sb.append(">" + PARAMS.MAX_WAITING_TIME / 60).append("\t");
			} else {
				sb.append(
						PARAMS.FORMAT_WAITING_TIME.format(op.waitingTime / 60))
						.append("\t");
			}

			sb.append(PARAMS.FORMAT_CHANCE.format(op.Q)).append("\t");

			sb.append(PARAMS.FORMAT_RUN_TIME.format(op.runTime)).append("\r\n");

			fwResult.write(sb.toString());
		}

		fwResult.close();

		logger.info("Results outputing finished");
	}
}
