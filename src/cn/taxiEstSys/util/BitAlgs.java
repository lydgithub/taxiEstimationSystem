package cn.taxiEstSys.util;

public class BitAlgs {
	public static long getModelKeyByConcating(long segID, byte sameDirection,
			short shotTimeIdx) {
		return segID << 16 | sameDirection << 15 | shotTimeIdx;// 拼接出模型的键
	}
}
