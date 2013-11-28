package cn.taxiEstSys.util.GIS;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;

import cn.taxiEstSys.type.PointDbl;
import cn.taxiEstSys.util.PARAMS;

import com.esri.arcgis.datasourcesGDB.FileGDBWorkspaceFactory;
import com.esri.arcgis.datasourcesGDB.InMemoryWorkspaceFactory;
import com.esri.arcgis.datasourcesGDB.SdeWorkspaceFactory;
import com.esri.arcgis.geodatabase.FeatureClass;
import com.esri.arcgis.geodatabase.Field;
import com.esri.arcgis.geodatabase.Fields;
import com.esri.arcgis.geodatabase.GeometryDef;
import com.esri.arcgis.geodatabase.IEnumIndex;
import com.esri.arcgis.geodatabase.IFeature;
import com.esri.arcgis.geodatabase.IFeatureClass;
import com.esri.arcgis.geodatabase.IFeatureCursor;
import com.esri.arcgis.geodatabase.IFeatureWorkspace;
import com.esri.arcgis.geodatabase.IFeatureWorkspaceProxy;
import com.esri.arcgis.geodatabase.IField;
import com.esri.arcgis.geodatabase.IFieldEdit;
import com.esri.arcgis.geodatabase.IFields;
import com.esri.arcgis.geodatabase.IFieldsEdit;
import com.esri.arcgis.geodatabase.IGeometryDef;
import com.esri.arcgis.geodatabase.IGeometryDefEdit;
import com.esri.arcgis.geodatabase.IIndex;
import com.esri.arcgis.geodatabase.IIndexEdit;
import com.esri.arcgis.geodatabase.IIndexes;
import com.esri.arcgis.geodatabase.IWorkspace;
import com.esri.arcgis.geodatabase.IWorkspaceFactory;
import com.esri.arcgis.geodatabase.IWorkspaceName;
import com.esri.arcgis.geodatabase.IWorkspaceProxy;
import com.esri.arcgis.geodatabase.Index;
import com.esri.arcgis.geodatabase.SpatialFilter;
import com.esri.arcgis.geodatabase.Workspace;
import com.esri.arcgis.geodatabase.esriFeatureType;
import com.esri.arcgis.geodatabase.esriFieldType;
import com.esri.arcgis.geodatabase.esriSpatialRelEnum;
import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.ISpatialReference;
import com.esri.arcgis.geometry.Point;
import com.esri.arcgis.geometry.Polyline;
import com.esri.arcgis.geometry.esriGeometryType;
import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.system.Cleaner;
import com.esri.arcgis.system.EngineInitializer;
import com.esri.arcgis.system.IClone;
import com.esri.arcgis.system.IName;
import com.esri.arcgis.system.INameProxy;
import com.esri.arcgis.system.PropertySet;

/**
 * 空间相关的算法 就是老钱　&　歌神的两个算法
 * 
 * @author koyo
 */
public class SpatialAlgs {

	private static boolean GDMS_INITIALIZED = false;
	private static DataSourceFactory dsf = null;
	private static DataSource ds = null;

	// private static boolean ESRI_INITIALIZED = false;

	private static IFeatureClass myFC = null;
	private static SpatialFilter spatialFilter = null;
	private static ISpatialReference spatialReference = null;
	private static int mySegID_idx = -1;

	private static Logger logger = Logger.getLogger(SpatialAlgs.class);

	public static void rebuildSpatialIndex(IFeatureClass featureClass,
			Double gridOneSize, Double gridTwoSize, Double gridThreeSize)
			throws AutomationException, IOException {
		// Get an enumerator for indexes based on the shape field.
		IIndexes indexes = featureClass.getIndexes();
		String shapeFieldName = featureClass.getShapeFieldName();
		IEnumIndex enumIndex = indexes.findIndexesByFieldName(shapeFieldName);
		enumIndex.reset();

		// Get the index based on the shape field (should only be one) and
		// delete it.
		IIndex index = enumIndex.next();
		if (index != null) {
			featureClass.deleteIndex(index);
		}

		// Clone the shape field from the feature class.

		int shapeFieldIndex = featureClass.findField(shapeFieldName);
		IFields fields = featureClass.getFields();
		IField sourceField = fields.getField(shapeFieldIndex);

		IClone sourceFieldClone = (IClone) sourceField;
		IClone targetFieldClone = sourceFieldClone.esri_clone();
		IField targetField = (IField) targetFieldClone;

		// Open the geometry definition from the cloned field and modify it.
		IGeometryDef geometryDef = targetField.getGeometryDef();
		IGeometryDefEdit geometryDefEdit = (IGeometryDefEdit) geometryDef;

		geometryDefEdit.setGridCount(2);
		geometryDefEdit.setGridSize(0, 0.004);
		geometryDefEdit.setGridSize(1, 0.015);
		// geometryDefEdit.setGridSize(0, gridThreeSize);

		// geometryDefEdit.setGridCount(3);
		// geometryDefEdit.setGridSize(0, gridOneSize);
		// geometryDefEdit.setGridSize(1, gridTwoSize);
		// geometryDefEdit.setGridSize(2, gridThreeSize);

		// Create a spatial index and set the required attributes.
		IIndex newIndex = new Index();
		IIndexEdit newIndexEdit = (IIndexEdit) newIndex;
		newIndexEdit.setName(shapeFieldName + "_Index");
		newIndexEdit.setIsAscending(true);
		newIndexEdit.setIsUnique(false);

		// Create a fields collection and assign it to the new index.
		IFields newIndexFields = new Fields();
		IFieldsEdit newIndexFieldsEdit = (IFieldsEdit) newIndexFields;
		newIndexFieldsEdit.addField(targetField);
		newIndexEdit.setFieldsByRef(newIndexFields);

		// Add the spatial index back into the feature class.
		featureClass.addIndex(newIndex);
	}

	// 在内存中创建点图层
	private static IFeatureClass getInMemoryFeatureClass(
			ISpatialReference pSpatialReference) {
		try {
			// 在内存中创建工作空间
			IWorkspaceFactory workspaceFactory = new InMemoryWorkspaceFactory();
			IWorkspaceName workspaceName = workspaceFactory.create("",
					"MyWorkspace", null, 0);
			IName name = new INameProxy(workspaceName);
			IWorkspace inmemWor = new IWorkspaceProxy(name.open());
			IFeatureWorkspace pFW = new IFeatureWorkspaceProxy(inmemWor);

			String strShapeFieldName = "shape";
			// InMemoryWorkspaceFactory pMWF = new InMemoryWorkspaceFactory();
			// IWorkspaceName pWName = pMWF.create("", "MyWorkspace", null, 0);
			// IWorkspace pWorkSpace = (IWorkspace) ((IName) pWName).open();
			// IFeatureWorkspace pFW = (IFeatureWorkspace) pWorkSpace;

			// 设置字段集
			Fields pFieldsEdit = new Fields();

			// 设置字段
			IField pField = new Field();
			IFieldEdit pFieldEdit = (IFieldEdit) pField;

			// 创建类型为几何类型的shape字段(必须)
			pFieldEdit.setName("shape");
			pFieldEdit.setType(esriFieldType.esriFieldTypeGeometry);

			// 为esriFieldTypeGeometry类型的字段创建几何定义，包括类型和空间参照
			IGeometryDef pGeoDef = new GeometryDef();
			IGeometryDefEdit pGeoDefEdit = (IGeometryDefEdit) pGeoDef;
			pGeoDefEdit.setGeometryType(esriGeometryType.esriGeometryPolyline);// 注意是缓冲后
			pGeoDefEdit.setSpatialReferenceByRef(pSpatialReference);

			pFieldEdit.setGeometryDefByRef(pGeoDef);
			pFieldsEdit.addField(pField);

			// 添加segID字段
			pField = new Field();
			pFieldEdit = (IFieldEdit) pField;
			pFieldEdit.setName("segID");
			pFieldEdit.setType(esriFieldType.esriFieldTypeString);// 没有提供长整型
			pFieldsEdit.addField(pField);

			IFeatureClass pFC = pFW.createFeatureClass("Temp", pFieldsEdit,
					null, null, esriFeatureType.esriFTSimple,
					strShapeFieldName, "");
			return pFC;
		} catch (AutomationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void getESRIAuth() {
		try {
			// 初始化ArcEngine授权
			EngineInitializer.initializeEngine();
			com.esri.arcgis.system.AoInitialize ao = new com.esri.arcgis.system.AoInitialize();
			if (ao.isProductCodeAvailable(com.esri.arcgis.system.esriLicenseProductCode.esriLicenseProductCodeEngine) == com.esri.arcgis.system.esriLicenseStatus.esriLicenseAvailable)
				ao.initialize(com.esri.arcgis.system.esriLicenseProductCode.esriLicenseProductCodeEngine);
		} catch (Exception e) {
			logger.error("Error getting ESRI Auth");
			e.printStackTrace();
		}

	}

	public static boolean initESRI() {
		getESRIAuth();

		try {
			spatialFilter = new SpatialFilter();

			if (PARAMS.MAP_SOURCE.equals("SDE")) {
				SdeWorkspaceFactory sdeFact = new SdeWorkspaceFactory();
				PropertySet propSet = new PropertySet();
				propSet.setProperty("SERVER", PARAMS.SERVER);
				propSet.setProperty("INSTANCE", PARAMS.INSTANCE);
				propSet.setProperty("DATABASE", PARAMS.DATABASE);
				propSet.setProperty("USER", PARAMS.USER);
				propSet.setProperty("PASSWORD", PARAMS.PASSWORD);
				propSet.setProperty("VERSION", PARAMS.VERSION);

				Workspace ws = new Workspace(sdeFact.open(propSet, 0));

				myFC = ws.openFeatureClass("Segment");
				mySegID_idx = myFC.findField("ID");
				spatialFilter.setSubFields("ID,shape");// shape不能少
			} else if (PARAMS.MAP_SOURCE.equals("FGDB")) {
				// 读取FGDB，最终不提供
				FileGDBWorkspaceFactory GDBwsfac = new FileGDBWorkspaceFactory();
				Workspace ws = new Workspace(GDBwsfac.openFromFile(
						PARAMS.WORK_SPACE_DIR + PARAMS.ROAD_FILE_PATH, 0));
				FeatureClass GDBfc = new FeatureClass(
						ws.openFeatureClass("Road"));

				myFC = GDBfc;
				mySegID_idx = myFC.findField("ID");
				spatialFilter.setSubFields("ID,shape");// shape不能少
			} else if (PARAMS.MAP_SOURCE.equals("IN_MEMORY")) {
				// 使用InMemory Feature Class，最终不提供
				FileGDBWorkspaceFactory GDBwsfac = new FileGDBWorkspaceFactory();
				Workspace ws = new Workspace(GDBwsfac.openFromFile(
						PARAMS.WORK_SPACE_DIR + PARAMS.ROAD_FILE_PATH, 0));
				FeatureClass GDBfc = new FeatureClass(
						ws.openFeatureClass("Road"));

				int GDB_ID_idx = GDBfc.findField("ID");
				// int GDB_width_idx = GDBfc.findField("width");

				// 建立好InMemoryFeatureClass
				spatialReference = GDBfc.getSpatialReference();
				myFC = getInMemoryFeatureClass(spatialReference);
				mySegID_idx = myFC.findField("segID");
				spatialFilter.setSubFields("segID,shape");// shape不能少

				// 遍历GDB，保存缓冲后的Polyline到内存
				IFeatureCursor cur = GDBfc.search(null, true);
				IFeature seg = cur.nextFeature();
				int i = 0;
				while (seg != null) {
					// double width = Double.parseDouble(seg.getValue(
					// GDB_width_idx).toString());
					String segID = seg.getValue(GDB_ID_idx).toString();
					Polyline segPL = (Polyline) seg.getShapeCopy();
					// myFeat.setShapeByRef(segPL.buffer(width*
					// 0.000005));//按路宽缓冲
					IFeature myFeat = myFC.createFeature();
					myFeat.setShapeByRef(segPL);
					myFeat.setValue(mySegID_idx, segID);
					myFeat.store();// 保存到InMemoryFeatureClass // int ttt =
					myFeat.getShape().getGeometryType();

					seg = cur.nextFeature();
					System.out.println("*********" + (++i)
							+ "/226237 segments loaded");
				}
			} else {
				logger.error("Unknown MAP_SOURCE");
				return false;
			}
			// rebuildSpatialIndex(myFC, 0.002, 0.006, 0.02);// 空间索引很重要
			spatialFilter.setGeometryField(myFC.getShapeFieldName());
			spatialFilter
					.setSpatialRel(esriSpatialRelEnum.esriSpatialRelCrosses);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		logger.info("ESRI initialized");
		return true;
	}

	public static long getSegID_by_Position_ESRI(PointDbl p) {
		try {
			// 已经是中国坐标
			Point p2 = new Point();
			p2.setSpatialReferenceByRef(spatialReference);
			p2.setX(p.x);
			p2.setY(p.y);
			IGeometry buf = p2.buffer(PARAMS.BUFFER_SIZE);

			spatialFilter.setGeometryByRef(buf);

			IFeatureCursor featureCursor = myFC.search(spatialFilter, false);
			IFeature feature = featureCursor.nextFeature();
			if (feature == null) {
				return -1;
			}

			long segID = Long.parseLong(feature.getValue(mySegID_idx)
					.toString());
			double distance = p2.returnDistance(feature.getShape());

			// int cnt = 1;

			while ((feature = featureCursor.nextFeature()) != null) {
				if (logger.getLevel() == Level.DEBUG) {
					// cnt++;
				}
				if (p2.returnDistance(feature.getShape()) < distance) {
					distance = p2.returnDistance(feature.getShape());
					segID = Long.parseLong(feature.getValue(mySegID_idx)
							.toString());
				}
			}

			// logger.debug("************" + cnt + " hits");

			Cleaner.release(featureCursor);
			return segID;
		} catch (Exception e) {
			logger.error("Error getting SegID fo Point " + p.toString());
			e.printStackTrace();
			return -1;
		}
	}

	public static long getSegID_by_Position_GDMS(PointDbl p) {
		if (!GDMS_INITIALIZED) {
			initGDMS();
		}

		PointDbl pChina = ModifyOffset.s2c(p);

		try {
			ds = dsf.getDataSourceFromSQL("SELECT ID FROM RoadNet WHERE ST_Crosses(the_geom,ST_Buffer(ST_GeomFromText('POINT("
					+ pChina.x
					+ " "
					+ pChina.y
					+ ")'), 0.0001,'Square')) ORDER BY ST_Distance(the_geom,ST_GeomFromText('POINT("
					+ pChina.x + " " + pChina.y + ")'));");
			// 0.0001大约为半径10米的矩形缓冲区，考虑GPS误差也足够了
			ds.open();
			long cnt = ds.getRowCount();
			// System.out.println("total: " + cnt);
			if (cnt == 0) {
				ds.close();
				return 0;// None of the segments cross the point
			}
			String segID = ds.getString(0, 0);
			// System.out.println("ID of the first one: " + segID);
			ds.close();

			return Long.parseLong(segID);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	// 老钱GDMS算法中用到的初始化过程，不用关心细节
	private static void initGDMS() {
		if (!GDMS_INITIALIZED) {
			File file = new File(PARAMS.WORK_SPACE_DIR + PARAMS.ROAD_FILE_PATH);
			dsf = new DataSourceFactory();
			dsf.getSourceManager().register("RoadNet", file);
		}
		GDMS_INITIALIZED = true;
	}

	// @SuppressWarnings("null")
	// public static void preCalcSegID() {
	// PARAMS.init();
	// initESRI();
	// BuildModel.prepareRoadNetworkProps();// 建立路网属性表
	//
	// DBCursor cursor = cn.taxiEstSys.util.db.MongoDB.getCursor(PARAMS.HOST,
	// PARAMS.PORT, PARAMS.DB_NAME, PARAMS.COLLECTION_NAME);
	// DBObject row = null;
	// TrackPoint tp = null;
	// short shotTimeIdx = -1;
	// long segID = -1;
	// int weekNum = -1;
	// // int DBsize = cursor.count();
	//
	// FileWriter fw = null;
	// try {
	// // fw = new FileWriter(new File("D:/TP_segID.txt"));
	//
	// System.out.println("----------------start "
	// + DateTime.getCurrentDate());
	// long start = System.currentTimeMillis();
	//
	// int idx = 0;
	//
	// int pos_Friday = 16000000;
	// idx += pos_Friday;
	// cursor.skip(pos_Friday);
	//
	// while (cursor.hasNext()) {
	// ++idx;
	// row = cursor.next();
	// tp = new TrackPoint(row);
	//
	// weekNum = tp.time.weekNum;
	// if (weekNum == 6 || (weekNum > 0 && weekNum < 4)) {
	// continue;// 跳过周二、三、四、日
	// }
	//
	// if (!pointWithInPartialExtent(tp.p)) {
	// continue;
	// }
	//
	// shotTimeIdx = Snapshot.timeString2timeIdx(tp.time);//
	// 根据轨迹点的时刻，求出是一周中的第几张快照
	// if (shotTimeIdx == -1)// 落在快照曝光时间外，过滤
	// continue;
	//
	// segID = SpatialAlgs.getSegID_by_Position_ESRI(ModifyOffset
	// .s2c(tp.p));// 老钱的算法，根据位置点求路段ID
	//
	// fw.write(idx + "_" + segID + "\n");
	// // fw.write(row.get("_id").toString() + "_" + segID + "\n");
	// }
	//
	// System.out.println("---------------------end "
	// + DateTime.getCurrentDate());
	// System.out.println("--------------"
	// + (System.currentTimeMillis() - start) + " ms cost");
	//
	// fw.close();
	// cursor.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// return;
	// }
	// }

	public static boolean pointWithInPartialExtent(PointDbl p) {
		// if (p.x < PARAMS.MINX || p.y < PARAMS.MINY || p.x > PARAMS.MAXX
		// || p.y > PARAMS.MAXY) {
		// return false;
		// } else {
		// return true;
		// }

		double bufSize = 0.01;
		// boolean outofInner = (p.x < PARAMS.MINX || p.y < PARAMS.MINY
		// || p.x > PARAMS.MAXX || p.y > PARAMS.MAXY);
		boolean outofOuter = (p.x < PARAMS.MINX - bufSize
				|| p.y < PARAMS.MINY - bufSize || p.x > PARAMS.MAXX + bufSize || p.y > PARAMS.MAXY
				+ bufSize);

		// return (outofInner) && !outofOuter;
		return !outofOuter;
	}

	public static void main(String[] args) throws Exception {
		// preCalcSegID();

		// 测试运行速度的，废弃
		// PARAMS.init();
		// Random r = new Random();
		// int idx = 0;
		//
		// initESRI(); // 初始化时间不计入平均时间
		// long start = System.currentTimeMillis();
		//
		// for (int i = 1; i <= 1000; ++i) {
		// long SegID = getSegID_by_Position_ESRI(new PointDbl(
		// 116.36914 + r.nextDouble() / 1000,
		// 39.915003 + r.nextDouble() / 1000));
		// System.out.println((++idx) + "************" + SegID);
		// // ModifyOffset.c2s(new PointDbl(116.294011, 39.857425));
		// // 117.3679083307631, 40.47967805263435
		// }
		// long end = System.currentTimeMillis();
		// System.out.println("Total time cost: " + (end - start) + "ms");
		// return;
	}

	/**
	 * 歌神的算法
	 * 
	 * 把垂直的情况也归结为了false
	 * 
	 * @param objDirection
	 * @param segDirection
	 * @return
	 */

	public static byte judgeSameDirection(int objDirection, int segDirection,
			int tolerance) {
		int deltaDegree = Math.abs(objDirection - segDirection);
		if (deltaDegree < tolerance// 绝对值必然大于0，不用写
				|| (deltaDegree > 270 && deltaDegree <= 360)) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * 
	 * 
	 * @param S
	 * @param E
	 * @return 400为两点重合
	 */
	public static int getDegreeBySE(PointDbl S, PointDbl E) {

		double roadx = E.x - S.x;
		double roady = E.y - S.y;

		double cosvalue = 0.0d;
		double radian = 0.0d;
		double angle = 0.0d;

		if (roadx == 0) {// y轴
			if (roady == 0) {
				System.out.println("error,两点重合");
				return 400;
			} else if (roady > 0) {
				return 0;
			} else {
				return 180;
			}
		} else {
			cosvalue = roady / Math.sqrt(roadx * roadx + roady * roady);
			radian = Math.acos(cosvalue);
			angle = Math.toDegrees(radian);
			// 精度截取，四舍五入
			BigDecimal bd = new BigDecimal(angle);
			angle = bd.setScale(0, BigDecimal.ROUND_HALF_UP).doubleValue();
			if (roadx > 0) {// y轴右侧
				if (roady == 0) {
					return 90;
				} else {// 第一、四象限
					// System.out.println("angle:" + angle);
					return (int) angle;
				}
			} else {// y轴左侧
				if (roady == 0) {
					return 270;
				} else {// 第二、三象限
					angle = 360 - angle;
					// System.out.println("angle:" + angle);
					return (int) angle;
				}
			}
		}

	}

	public static boolean personMoved(int personDirection, int segDirection) {
		if (judgeSameDirection(personDirection, segDirection, 30) == 1
				|| judgeSameDirection((personDirection + 180) % 360,
						segDirection, 30) == 1) {
			return false;
		} else {
			return true;
		}
	}
}
