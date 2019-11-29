package clientstreamingestapp.application;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bson.Document;

import clientstreamingestapp.mongo.Mongo;
import clientstreamingestapp.rabbitmq.RabbitmqClient;

public class Runner {

	final static Logger logger = Logger.getLogger(Runner.class);
	static String USER;
	static String APPID = "2";
	static String PATH;
	static double faultrate = 0.1;

	public static void main(String[] args) {
//		RabbitmqClient rabbitmqClient = new RabbitmqClient("master");
		String path = "/home/youzi/courses/bigdataplatform/assignment-03-801704/data/yellow_demo.csv";
		PATH = path;
		new Thread(new insertThread("1", PATH, "")).start();

	}

	static class insertThread implements Runnable {
		String processId;
		String path;
		String logPath;
		RabbitmqClient rabbitmqClient;

		public insertThread(String processId, String path, String logPath) {
			this.processId = processId;
			this.path = path;
			this.logPath = logPath;
			rabbitmqClient = new RabbitmqClient("inputQueue");

		}

		public void run() {
//			int documentNum = 0;
			ArrayList<HashMap<String, String>> documents = csvUtil.getTaxiData(path);
			String result = "fail";
			if (documents.size() != 0) {
				Iterator<HashMap<String, String>> documentSet = documents.iterator();
				while (documentSet.hasNext()) {
					HashMap<String,String> document = documentSet.next();
					String message = "";
					String data = "";
					Iterator<String> keySet = document.keySet().iterator();
					while(keySet.hasNext()) {
						String key = keySet.next();
						data+=key+":"+document.get(key)+" ";
					}
					message+=data+","+new Date().getTime();
					
					if (Math.random()<faultrate) {
						message = "asdfasdfasdfasdf";
					}
					rabbitmqClient.publish(message);
					try {
						Thread.sleep(0);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
//			try {
//				if (!new File(logPath + "/info.log").exists()) {
//					new File(logPath + "/info.log").createNewFile();
//				}
//				Files.write(Paths.get(logPath + "/info.log"), log.getBytes(), StandardOpenOption.APPEND);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			return;
		}
	}

}
