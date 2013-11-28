package cn.taxiEstSys.util.math;

import cn.taxiEstSys.util.PARAMS;

/**
 * 根据Q，求解打车概率和等车时间。阿黄的算法，概率论公式推导出来，用在这里
 * 
 * @author koyo
 * 
 */
public class ChanceAndWaitingTimeCalculation {

	// 注意，求的是3分钟内成功打到车的概率
	public static double getChanceOfSucByQ(double Q) {
		// 曝光8分钟，等车3分钟；按比例缩放，得到3分钟内经过的车数
		Q = Q * PARAMS.WAITING_TIME / PARAMS.SHOT_DURATION;
		if (Q <= 1) {
			return 0.75 * Q;
		} else {
			return 3 * Q / (3 * Q + 1);
		}
	}

	// 注意，都是国际单位，秒；直接返回“要等多少秒”，在规定时限内等不到车由函数外处理
	public static double getWatingTimeByQ(double Q) {
		if (Q == 0 || PARAMS.SHOT_DURATION / Q > PARAMS.MAX_WAITING_TIME * 100) {// 如果超过了最长等待时限的100倍，就认为是无穷大
			return Double.MAX_VALUE;
		} else {
			return PARAMS.SHOT_DURATION / Q;
		}
	}
}
