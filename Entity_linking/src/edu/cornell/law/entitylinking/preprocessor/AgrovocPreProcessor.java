package edu.cornell.law.entitylinking.preprocessor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.TreeMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

public class AgrovocPreProcessor {
	public static Map<String,String> AgrovocMap =  new TreeMap<String,String>();
	
	public static void generateAgrovocMap(String path) {
		Model m = ModelFactory.createDefaultModel();
		m.read(path);
		StmtIterator iterator;
		Statement stmt;

		iterator = m.listStatements();
		while (iterator.hasNext()) {
		    stmt = iterator.next();
		    if (stmt.getPredicate().getLocalName().trim().contains("altLabel") || stmt.getPredicate().getLocalName().contains("prefLabel")) {
		    	if (stmt.getObject().toString().toLowerCase().replace("@en", "").equals("www"))
		    		continue;
		    	AgrovocMap.put(stmt.getObject().toString().toLowerCase().replace("@en", ""), stmt.getSubject().getURI());
		    }	
		}
		
		//Write the Map into a file 
		/*BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter("./preprocessedMaps/Agrovoc_Map.out"));
			System.out.println("Size of AgrovocMap: "+AgrovocMap.size());
			System.out.println("***************Agrovoc Map LIST**************");
			for (String k : AgrovocMap.keySet()) {
				writer.write(k + "\t" + AgrovocMap.get(k)+"\n");
			}
			writer.close();
		}
		catch (Exception e) {
				System.out.println(e.getMessage());
			}	*/
	}	
}
