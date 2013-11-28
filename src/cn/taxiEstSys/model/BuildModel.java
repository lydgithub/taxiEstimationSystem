package cn.taxiEstSys.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import cn.taxiEstSys.type.SegProp;
import cn.taxiEstSys.type.Snapshot;
import cn.taxiEstSys.type.TrackPoint;
import cn.taxiEstSys.util.BitAlgs;
import cn.taxiEstSys.util.PARAMS;
import cn.taxiEstSys.util.GIS.ModifyOffset;
import cn.taxiEstSys.util.GIS.SpatialAlgs;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * 这个类的功能就是建模并把建好的模型写到文本文件中去，执行main即可
 * 
 * @author koyo
 * 
 */
public class BuildModel {
	public static HashMap<Long, SegProp> roadNetworkProp = null;// 路网属性表
	// 模型是个哈希表(SegID_side_timeIdx)=>(N,V,Q...)
	private static HashMap<Long, Snapshot> model = null;

	private static Logger logger = Logger.getLogger(BuildModel.class);

	public static void main(String[] args) throws IOException {
		PropertyConfigurator.configure("etc/log4j.Properties");

		PARAMS.init();
		prepareRoadNetworkProps();// 建立路网属性表

		// SpatialAlgs.initESRI();
		// scanDB(PARAMS.TP_SEGID_FILE_PATH, PARAMS.TP_SEGID_BAKFILE_PATH);//
		// 建模，并把老钱的函数结果写的文本文件

		buildModel(PARAMS.TP_SEGID_FILE_PATH);
		writeModel2txt(PARAMS.MODEL_FILE_PATH);// 把模型写到文本文件
	}

	private static void buildModel(String TP_SegID_File_Path)
			throws IOException {
		model = new HashMap<Long, Snapshot>();
		Snapshot ss = null;
		short shotTimeIdx = -1;
		long segID = -1;
		byte sameDirection = -1;
		long idx = 0;
		int TP_SEGID_FILE_LINES = 5432555;// cursor.count();
		double speed = -1;
		int taxiID = -1;

		BufferedReader r = new BufferedReader(new InputStreamReader(
				new FileInputStream(TP_SegID_File_Path)));

		String line = r.readLine();
		while (line != null) {
			++idx;
			if (idx % 10000 == 0) {
				logger.info(idx + "/" + TP_SEGID_FILE_LINES
						+ " lines processed");
			}
			String[] strs = line.split("_");
			segID = Long.parseLong(strs[1]);
			sameDirection = Byte.parseByte(strs[2]);
			shotTimeIdx = Short.parseShort(strs[3]);
			speed = Double.parseDouble(strs[4]);
			taxiID = Integer.parseInt(strs[5]);

			long modelKey = BitAlgs.getModelKeyByConcating(segID,
					sameDirection, shotTimeIdx);// 拼接出模型的键
			if (!model.containsKey(modelKey)) {
				model.put(modelKey, new Snapshot(segID, sameDirection,
						shotTimeIdx));
			}

			ss = model.get(modelKey);
			ss.multiN++;// 出现的总车数加1
			ss.V += speed;// 求平均速度用到的累加器
			ss.allTaxiIDs.add(taxiID);// 这张快照见过的所有TaxiID

			line = r.readLine();
		}

		r.close();

		// 确认遍历完成之后，收尾工作，把每张快照的平均速度和Q求出来
		logger.info("Starting post-calc model");
		double V = -1;
		int deltaT = -1;
		double L = -1;
		int N = -1;
		for (Snapshot ss2 : model.values()) {
			V = (ss2.V / ss2.multiN) * 0.277777778;// 千米/小时 => 米/秒
			deltaT = PARAMS.SHOT_DURATION;
			L = roadNetworkProp.get(ss2.segID).length * 1000;// 千米　=>　米
			N = ss2.allTaxiIDs.size();
			if (L < V * PARAMS.GPS_SAMPLE_INTERVAL) {// 如果路段短于1分钟开过的长度，按比例还原真实的N
				N *= V * PARAMS.GPS_SAMPLE_INTERVAL / L;
			}
			ss2.Q = (N * V * deltaT) / (L + V * deltaT);//
			// 前面用的都是国际单位，所以这里求出来的Q的单位是“辆”
		}

		logger.info("Model built");
	}

	public static void prepareRoadNetworkProps() {
		try {
			// 读PartialSegmentProps.csv，建立roadNetworkProp
			roadNetworkProp = new HashMap<Long, SegProp>();

			BufferedReader r = new BufferedReader(new InputStreamReader(
					new FileInputStream(PARAMS.WORK_SPACE_DIR
							+ PARAMS.SEG_PROP_FILE_PATH)));

			String line = r.readLine();
			while (line != null) {
				String[] str = line.split(",");
				long SegID = Long.parseLong(str[0]);
				double len = Double.parseDouble(str[1]);
				double Sx = Double.parseDouble(str[2]);
				double Sy = Double.parseDouble(str[3]);
				double Ex = Double.parseDouble(str[4]);
				double Ey = Double.parseDouble(str[5]);

				roadNetworkProp.put(SegID, new SegProp(SegID, len, Sx, Sy, Ex,
						Ey));
				line = r.readLine();
			}

			r.close();

			logger.info("RoadNetwork Loaded");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private static void scanDB(String TP_SegID_File_Path,
			String oldTP_SegID_File_Path) throws IOException {
		DBCursor cursor = null;

		// ...从DB中流式地读取轨迹点到trackPts，变量抽出来提高效率
		logger.info("Starting scanDB");
		DBObject row = null;
		TrackPoint tp = null;
		short shotTimeIdx = -1;
		long segID = -1;
		byte sameDirection = -1;
		long idx = 0;
		// 注意是追加
		FileWriter fwTP_SegID = new FileWriter(TP_SegID_File_Path, true);

		cursor = cn.taxiEstSys.util.db.MongoDB.getCursor(PARAMS.HOST,
				PARAMS.PORT, PARAMS.DB_NAME, PARAMS.COLLECTION_NAME);
		int DBsize = cursor.count();

		idx += 3980172;
		cursor.skip(3980172);

		while (cursor.hasNext()) {
			idx++;
			row = cursor.next();

			if (idx % 100 == 0) {
				logger.debug(idx + "/" + DBsize + " records processed");
			}

			tp = new TrackPoint(row);

			// // 如果轨迹点不在Partial范围内，过滤
			// if (!SpatialAlgs.pointWithInPartialExtent(tp.p)) {
			// continue;
			// }
			//
			shotTimeIdx = Snapshot.timeString2timeIdx(tp.time);//
			// 根据轨迹点的时刻，求出是一周中的第几张快照
			// if (shotTimeIdx == -1)// 落在快照曝光时间外，过滤
			// continue;

			segID = SpatialAlgs.getSegID_by_Position_ESRI(ModifyOffset
					.s2c(tp.p));// 老钱的算法，根据位置点求路段ID

			if (segID <= 0) {// 不在马路上者，过滤
				continue;
			}

			sameDirection = SpatialAlgs.judgeSameDirection(tp.direction,
					roadNetworkProp.get(segID).direction, 90);// 歌神的算法，求车与路段是否同向

			StringBuilder sb = new StringBuilder();
			sb.append(idx).append("_").append(segID).append("_")
					.append(sameDirection).append("_").append(shotTimeIdx)
					.append("_").append(tp.speed).append("_").append(tp.taxiID)
					.append("\n");
			fwTP_SegID.write(sb.toString());
		}

		cursor.close();

		logger.info("scanDB finished");

		fwTP_SegID.close();
	}

	private static void writeModel2txt(String modelFilePath) throws IOException {
		logger.info("Starting write model to file ---- " + modelFilePath);
		FileWriter fwModel = new FileWriter(new File(modelFilePath));

		for (Snapshot ss : model.values()) {
			fwModel.write(ss.toString());
		}

		fwModel.close();
		logger.info("Model written to file: " + modelFilePath);
	}

}
