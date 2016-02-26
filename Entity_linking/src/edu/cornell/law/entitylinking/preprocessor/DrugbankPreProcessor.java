package edu.cornell.law.entitylinking.preprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DrugbankPreProcessor {
	public static Map<String,String> drugsMap =  new HashMap<String,String>();
	
	public static void generateDrugsMap(String path) throws IOException {
		BufferedReader br = null;
		
				
		try {
			String currentLine = "";
			//BufferedWriter writer = new BufferedWriter(new FileWriter("./preprocessedMaps/drugs_Map.out"));
			br = new BufferedReader(new FileReader(path));  
			while ((currentLine = br.readLine()) != null)
			{
				if (currentLine.contains("rdf-schema#label")) {
					
					/* 
					 * To search for in the file: 
					 * <http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugs/DB00852> <http://www.w3.org/2000/01/rdf-schema#label> "Pseudoephedrine" .
					*/
					String[] currentLineArray = currentLine.split(" [<\"]");
					
					if (!drugsMap.containsKey(currentLineArray[2]) && currentLineArray[0].contains("/drugbank/resource/drugs/"))
						drugsMap.put(currentLineArray[2].replace("\" .","").toLowerCase(), currentLineArray[0].substring(currentLineArray[0].indexOf("DB"),currentLineArray[0].indexOf("DB")+7));				
				}
			}			
			/*System.out.println("Size of drugsMap: "+drugsMap.size());
			System.out.println("***************DRUGS LIST**************");
			for (String k : drugsMap.keySet()) {
				writer.write(k + "\t" + drugsMap.get(k)+"\n");
			}
			writer.close();*/
	}
	catch (Exception e) {
			System.out.println(e.getMessage());
		}
	finally {
		br.close();		
	}
	}
}
	
