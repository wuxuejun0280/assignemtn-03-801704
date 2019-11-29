package clientstreamingestapp.mongo;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.apache.log4j.Logger;
import org.bson.Document;

public class Mongo {

	MongoClient mongoClient;
	MongoDatabase database;
	final static Logger logger = Logger.getLogger(Mongo.class);

	public Mongo(String databaseName) {
		// address of mongo may have to change based on the situation
		mongoClient = new MongoClient();
		database = mongoClient.getDatabase(databaseName);
	}

	public int importData(List<Document> documents, String collection, String thread) {
		MongoCollection<Document> coll = database.getCollection(collection);
		coll.insertMany(documents);
		Date end = new Date();
		return documents.size();
	}
}
