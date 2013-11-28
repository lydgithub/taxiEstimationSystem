package cn.taxiEstSys.util;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class TestLog4j {

	private static Logger logger = Logger.getLogger(TestLog4j.class);

	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.Properties");

		logger.debug("Here is DEBUG messgae");
		logger.info("Here is INFO message");
		logger.warn("Here is WARN message");
		logger.error("Here is ERROR message");
		logger.fatal("Here is FATAL message");

	}

}
