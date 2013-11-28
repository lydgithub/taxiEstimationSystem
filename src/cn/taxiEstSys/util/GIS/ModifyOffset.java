package cn.taxiEstSys.util.GIS;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import cn.taxiEstSys.type.PointDbl;

/**
 * 地图坐标修偏
 * 
 * @author wangtong
 */
public class ModifyOffset {

	static double[] X = new double[660 * 450];
	static double[] Y = new double[660 * 450];
	public static boolean INITIALIZED = false;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		init(ModifyOffset.class.getResourceAsStream("axisoffset.dat"));
		// System.out.println(c2s(new PointDouble(118.7715263, 32.0789851)));
		// System.out.println(s2c(new PointDouble(118.7715263, 32.0789851)));
		// System.out.println(c2s(new PointDouble(116.294011, 39.857425)));
		// System.out.println(s2c(new PointDouble(116.3142319, 39.8467484)));
	}

	public static void init(InputStream inputStream) throws IOException {
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(inputStream);

			int i = 0;
			while (in.available() > 0) {
				if ((i & 1) == 1) {
					Y[(i - 1) >> 1] = in.readInt() / 100000.0d;
					;
				} else {
					X[i >> 1] = in.readInt() / 100000.0d;
					;
				}
				i++;
			}
			INITIALIZED = true;
		} catch (IOException e) {
			INITIALIZED = false;
			System.out
					.println("******************Error initializing ModifyOffset data... data reading error");
			e.printStackTrace();
			throw e;
		} finally {
			if (in != null)
				in.close();
		}
	}

	// standard -> china
	public static PointDbl s2c(PointDbl pt) {
		if (!INITIALIZED) {
			try {
				init(ModifyOffset.class.getResourceAsStream("axisoffset.dat"));
			} catch (IOException e) {
				System.err.println("读取坐标转换数据时出错");
				e.printStackTrace();
			}
		}

		int cnt = 10;
		double x = pt.x, y = pt.y;
		while (cnt-- > 0) {
			if (x < 71.9989d || x > 137.8998d || y < 9.9997d || y > 54.8996d)
				return pt;
			int ix = (int) (10.0d * (x - 72.0d));
			int iy = (int) (10.0d * (y - 10.0d));
			double dx = (x - 72.0d - 0.1d * ix) * 10.0d;
			double dy = (y - 10.0d - 0.1d * iy) * 10.0d;
			x = (x + pt.x + (1.0d - dx) * (1.0d - dy) * X[ix + 660 * iy] + dx
					* (1.0d - dy) * X[ix + 660 * iy + 1] + dx * dy
					* X[ix + 660 * iy + 661] + (1.0d - dx) * dy
					* X[ix + 660 * iy + 660] - x) / 2.0d;
			y = (y + pt.y + (1.0d - dx) * (1.0d - dy) * Y[ix + 660 * iy] + dx
					* (1.0d - dy) * Y[ix + 660 * iy + 1] + dx * dy
					* Y[ix + 660 * iy + 661] + (1.0d - dx) * dy
					* Y[ix + 660 * iy + 660] - y) / 2.0d;
		}
		return new PointDbl(x, y);
	}

	// china -> standard
	public static PointDbl c2s(PointDbl pt) {
		if (!INITIALIZED) {
			try {
				init(ModifyOffset.class.getResourceAsStream("axisoffset.dat"));
			} catch (IOException e) {
				System.err.println("读取坐标转换数据时出错");
				e.printStackTrace();
			}
		}

		int cnt = 10;
		double x = pt.x, y = pt.y;
		while (cnt-- > 0) {
			if (x < 71.9989d || x > 137.8998d || y < 9.9997d || y > 54.8996d)
				return pt;
			int ix = (int) (10.0d * (x - 72.0d));
			int iy = (int) (10.0d * (y - 10.0d));
			double dx = (x - 72.0d - 0.1d * ix) * 10.0d;
			double dy = (y - 10.0d - 0.1d * iy) * 10.0d;
			x = (x + pt.x - (1.0d - dx) * (1.0d - dy) * X[ix + 660 * iy] - dx
					* (1.0d - dy) * X[ix + 660 * iy + 1] - dx * dy
					* X[ix + 660 * iy + 661] - (1.0d - dx) * dy
					* X[ix + 660 * iy + 660] + x) / 2.0d;
			y = (y + pt.y - (1.0d - dx) * (1.0d - dy) * Y[ix + 660 * iy] - dx
					* (1.0d - dy) * Y[ix + 660 * iy + 1] - dx * dy
					* Y[ix + 660 * iy + 661] - (1.0d - dx) * dy
					* Y[ix + 660 * iy + 660] + y) / 2.0d;
		}
		return new PointDbl(x, y);
	}

}
