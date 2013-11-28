package cn.taxiEstSys.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Properties;

import org.apache.log4j.Logger;

public class PARAMS {
	public static boolean INITIALIZED = false;

	public static boolean DB_AVAILABLE = false;

	// 建模用参数
	public static String MODEL_FILE_PATH = null;
	public static String TP_SEGID_FILE_PATH = null;
	public static String TP_SEGID_BAKFILE_PATH = null;
	public static String OP_FILE_PATH = null;
	public static String RESULT_FILE_PATH = null;

	public static String HOST = null;// localhost
	public static int PORT = -1;
	public static String DB_NAME = null;
	public static String COLLECTION_NAME = null;
	public static String WORK_SPACE_DIR = null;
	public static String SEG_PROP_FILE_PATH = null;
	public static String ROAD_FILE_PATH = null;
	public static String TIME_IDX_FILE_PATH = null;

	// 模型参数
	public static short NUM_OF_SHOTS＿IN_A_DAY = -1;// 一天拍几张快照
	public static int SHOT_DURATION = -1;// 默认曝光8分钟

	// 观察策略
	public static int MAX_WAITING_TIME = -1;
	public static int WAITING_TIME = -1;
	public static int GPS_SAMPLE_INTERVAL = -1;

	// 输出格式参数
	public static DecimalFormat FORMAT_CHANCE = null;
	public static DecimalFormat FORMAT_WAITING_TIME = null;
	public static DecimalFormat FORMAT_RUN_TIME = null;

	// SDE
	public static String SERVER = null;
	public static String INSTANCE = null;
	public static String DATABASE = null;
	public static String USER = null;
	public static String PASSWORD = null;
	public static String VERSION = null;

	public static String MAP_SOURCE = null;

	public static double MINX = -1;
	public static double MINY = -1;
	public static double MAXX = -1;
	public static double MAXY = -1;

	public static double BUFFER_SIZE = -1;

	private static Logger logger = Logger.getLogger(PARAMS.class);

	public static void init() {

		if (INITIALIZED) {
			return;
		}

		try {

			WORK_SPACE_DIR = System.getProperty("user.dir");
			FileInputStream fis = new FileInputStream(WORK_SPACE_DIR
					+ "/etc/params.Properties");

			Properties prop = new Properties();
			prop.load(fis);

			DB_AVAILABLE = Boolean.parseBoolean(prop
					.getProperty("DB_AVAILABLE"));
			MODEL_FILE_PATH = prop.getProperty("MODEL_FILE_PATH");
			TP_SEGID_FILE_PATH = prop.getProperty("TP_SEGID_FILE_PATH");
			TP_SEGID_BAKFILE_PATH = prop.getProperty("TP_SEGID_BAKFILE_PATH");
			OP_FILE_PATH = prop.getProperty("OP_FILE_PATH");
			RESULT_FILE_PATH = prop.getProperty("RESULT_FILE_PATH");

			HOST = prop.getProperty("HOST");
			PORT = Integer.parseInt(prop.getProperty("PORT"));
			DB_NAME = prop.getProperty("DB_NAME");
			COLLECTION_NAME = prop.getProperty("COLLECTION_NAME");
			SEG_PROP_FILE_PATH = prop.getProperty("SEG_PROP_FILE_PATH");
			ROAD_FILE_PATH = prop.getProperty("ROAD_FILE_PATH");
			TIME_IDX_FILE_PATH = prop.getProperty("TIME_IDX_FILE_PATH");

			SHOT_DURATION = 60 * Integer.parseInt(prop
					.getProperty("SHOT_DURATION"));

			MAX_WAITING_TIME = 60 * Integer.parseInt(prop
					.getProperty("MAX_WAITING_TIME"));
			WAITING_TIME = 60 * Integer.parseInt(prop
					.getProperty("WAITING_TIME"));
			GPS_SAMPLE_INTERVAL = 60 * Integer.parseInt(prop
					.getProperty("GPS_SAMPLE_INTERVAL"));

			FORMAT_CHANCE = new DecimalFormat(prop.getProperty("FORMAT_CHANCE"));
			FORMAT_WAITING_TIME = new DecimalFormat(
					prop.getProperty("FORMAT_WAITING_TIME"));
			FORMAT_RUN_TIME = new DecimalFormat(
					prop.getProperty("FORMAT_RUN_TIME"));

			MAP_SOURCE = prop.getProperty("MAP_SOURCE");

			// SDE
			SERVER = prop.getProperty("SERVER");
			INSTANCE = prop.getProperty("INSTANCE");
			DATABASE = prop.getProperty("DATABASE");
			USER = prop.getProperty("USER");
			PASSWORD = prop.getProperty("PASSWORD");
			VERSION = prop.getProperty("VERSION");

			// Partial Extent
			MINX = Double.parseDouble(prop.getProperty("MINX"));
			MINY = Double.parseDouble(prop.getProperty("MINY"));
			MAXX = Double.parseDouble(prop.getProperty("MAXX"));
			MAXY = Double.parseDouble(prop.getProperty("MAXY"));

			BUFFER_SIZE = Double.parseDouble(prop.getProperty("BUFFER_SIZE"));

			INITIALIZED = true;
		} catch (FileNotFoundException e) {
			logger.error(WORK_SPACE_DIR + "/etc/params.Properties"
					+ "could NOT found");
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("damaged params.Properties file");
			e.printStackTrace();
		}
	}
}
