package cn.taxiEstSys.util;

import cn.taxiEstSys.type.PointDbl;
import cn.taxiEstSys.type.Snapshot;
import cn.taxiEstSys.type.TimeString;
import cn.taxiEstSys.util.GIS.ModifyOffset;

public class Tester {

	/**
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		PARAMS.init();
//		SpatialAlgs.initESRI();
		
		PointDbl p1=ModifyOffset.s2c(new PointDbl(116.3981186, 39.85986098));
		PointDbl p2=ModifyOffset.s2c(new PointDbl(116.4792503, 39.91469681));
		
		

		short preShotTimeIdx = Snapshot.timeString2preTimeIdx(new TimeString(
				"20130915101200"));

		TimeString time1 = new TimeString("20121105000000");
		TimeString time2 = new TimeString("20121105000500");
		TimeString time3 = new TimeString("20121105063100");
		TimeString time4 = new TimeString("20121105223900");
		TimeString time5 = new TimeString("20121104223500");

		int shotTimeIdx1 = Snapshot.timeString2timeIdx(time1);
		int shotTimeIdx2 = Snapshot.timeString2timeIdx(time2);
		int shotTimeIdx3 = Snapshot.timeString2timeIdx(time3);
		int shotTimeIdx4 = Snapshot.timeString2timeIdx(time4);
		int shotTimeIdx5 = Snapshot.timeString2timeIdx(time5);

		{
			int i = 0;
		}
	}
}
