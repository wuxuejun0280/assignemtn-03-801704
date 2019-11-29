package clientstreamingestapp.application;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

public class csvUtil {
	public static ArrayList<HashMap<String,String>> getTaxiData(String fileName) {
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String,String>>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = br.readLine();
			String[] header = line.split(",");
			line = br.readLine();
			while (line != null) {
				String[] attributes = line.split(",");
				HashMap<String, String> document = new HashMap<String,String>();
				
				for (int i=0;i<header.length;i++) {
					document.put(header[i], attributes[i]);
				}
				list.add(document);
				line = br.readLine();
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
			
		}
		return list;
	}
}
