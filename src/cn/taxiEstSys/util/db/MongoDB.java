package cn.taxiEstSys.util.db;

import java.net.UnknownHostException;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;

public class MongoDB {
	private static Mongo mongo_conn = null;
	private static DBCollection dbcoll = null;
	private static DBCursor cursor = null;

	public static DBCursor getCursor(String hostname, int port, String dbName,
			String collectionName) {
		try {
			mongo_conn = new Mongo(new ServerAddress(hostname, port));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		dbcoll = mongo_conn.getDB(dbName).getCollection(collectionName);
		cursor = dbcoll.find();
		return cursor;
	}

	public static void closeDB() {
		cursor.close();
		cursor = null;

		mongo_conn.close();
		mongo_conn = null;
	}
}
