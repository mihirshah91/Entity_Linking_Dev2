package edu.cornell.law.entitylinking;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import edu.cornell.law.entitylinking.preprocessor.*;
import edu.cornell.law.entitylinking.utils.*;

class LinkingThread extends Thread{
	ConcurrentHashMap<String, String> entityToLinkMap = new ConcurrentHashMap<String, String>();
	HashMap<Integer, ArrayList<String>> paragraphEntitiesMap = new HashMap<Integer, ArrayList<String>>();
	HashMap<Integer, String> queryMap;
	HashSet<String> entitiesToBeRemoved;
	String database;
	String threadId;
	Map<String,String> DbpediaMap;
	public ConcurrentHashMap<String, String> getLinkMap(){
		return entityToLinkMap;
	}
	
	public HashMap<Integer, ArrayList<String>> getParaEntitiesMap(){
		return paragraphEntitiesMap;	
	}
	
	public LinkingThread(String threadId,HashMap<Integer, String> queryMap, String database, HashSet<String> entitiesToBeRemoved){
		this.threadId = threadId;
		this.queryMap = queryMap;
		this.database = database;
		this.entitiesToBeRemoved = entitiesToBeRemoved;
		
	}
	
	@Override
	public void run(){
		
		//System.out.println("*************************Running Thread" + threadId + "***************************");
		//Function to tag the paragraph and fetch only the Noun Phrases, If noun Phrases are not present in Ontologies, check for nouns. 
		
		Map<String, String> databaseMap = null;
		for(Map.Entry<Integer, String> entry : queryMap.entrySet()){
			
			Integer paraIndex = entry.getKey();			
			String content = entry.getValue();
		  // Actual entities which have a valid meaning in Ontology, to be linked in the paragraph
      ArrayList<String> actualEntitiesdb = new ArrayList<String>();
			
			List<String> potentialEntities = null;
			if(database.equals("Dbpedia")){
			  /*System.out.println("Database is dbpedia");
			  System.out.print("#####################");
			  System.out.println("Actual: "+content);*/
			  potentialEntities = Utility.findPOSTags(content);
			  //System.out.println("Potential: "+potentialEntities);
			  HashSet<String> sampleContext= DbpediaLinker.extractContentWords(content);
			  //System.out.println("Potential: "+sampleContext);
			  DbpediaMap =  new TreeMap<String,String>();
			  
			  //String[] splitContent = content.split("\\s+");
			  for(String current: potentialEntities){
			    if(current.equals("CFR") || current.equals("FR") )
			        continue;
			    if( current.length() > 2){
			      //System.out.println(current + " :is trying to get linked");
			      String uri = DbpediaLinker.getDbpediaLinkDisambiguate(current,sampleContext);
			      if(uri != null){
			        /*System.out.println("DBpedia disambi fetch: "+current);
			        System.out.println("@@@@@@@@@ Linked Entity: "+ current);                                                        
			        System.out.println("*****###Uri returned is: "+uri);*/
			       
              if (!entityToLinkMap.containsKey(current.toLowerCase())){                
                
                if (!actualEntitiesdb.contains(current))
                  actualEntitiesdb.add(current);
                entityToLinkMap.put(current.toLowerCase(),uri);
              }
              else {
                if (!actualEntitiesdb.contains(current))
                  actualEntitiesdb.add(current);
              }
			      }
			      else{
			        
			        //Check for normal uri
			        uri = DbpediaLinker.getDbpediaLink(current);
			        if(uri != null){
			          /*System.out.println("DBpedia direct fetch: "+current);
	              System.out.println("@@@@@@@@@ Linked Entity: "+ current);
	              System.out.println("*****###Uri returned is: "+uri);*/
	              
	              if (!entityToLinkMap.containsKey(current.toLowerCase())){                
	                
	                if (!actualEntitiesdb.contains(current))
	                  actualEntitiesdb.add(current);
	                entityToLinkMap.put(current.toLowerCase(),uri);
	              }
	              else {
	                if (!actualEntitiesdb.contains(current))
	                  actualEntitiesdb.add(current);
	              }
	              
	            }
	            else{
	              //System.out.println("XXXXXXXX Couldn't Link Entity: "+ current);
	            
	            }
			      }
			    }
			  }
			  paragraphEntitiesMap.put(paraIndex, actualEntitiesdb);
			  /*for(Integer idx : paragraphEntitiesMap.keySet()){
			    System.out.println(idx);
			    System.out.println(paragraphEntitiesMap.get(idx));
			  }*/
			  
			}
			
			else{
			
    			if(database.equals("Drugbank"))
    			{
    				// Better approach to get all noun Phrases for Drugbank, as it will be able to capture complete drug names
    				potentialEntities = Utility.getAllNounPhrases(content);
    				databaseMap = DrugbankPreProcessor.drugsMap;
    			}
    			else if(database.equals("Mesh"))
    			{
    				// Find inner and short noun phrases for other Ontologies
    				potentialEntities = Utility.getInnerNounPhrases(content);
    				databaseMap = MeshPreProcessor.meshMap;
    			}
    			else if(database.equals("Agrovoc"))
    			{
    				// Find inner and short noun phrases for other Ontologies
    				potentialEntities = Utility.getInnerNounPhrases(content);
    				databaseMap = AgrovocPreProcessor.AgrovocMap;
    			}
    
    			// Actual entities which have a valid meaning in Ontology, to be linked in the paragraph
    			ArrayList<String> actualEntities = new ArrayList<String>();
    			
    			// Loop through the list of potential entity words in the paragraph and find a match in Ontology map 
    			for (String s : potentialEntities) {
    				String sLower = s.toLowerCase().trim();
    				boolean crossBoundary = false;
    				
    				for (String entity : entitiesToBeRemoved) {
    					if (entity.contains(sLower) || sLower.contains(entity)) {
    						crossBoundary = true;
    					}
    				}
    				if(crossBoundary)
    					continue;
    				
    				if (databaseMap.containsKey(sLower)) {
    				  
    					String output = null; 
    					if (!entityToLinkMap.containsKey(sLower)) {								
    						output = databaseMap.get(sLower);
    						if (!actualEntities.contains(s))
    							actualEntities.add(s);
    						entityToLinkMap.put(sLower,output);
    					}
    					else {
    						if (!actualEntities.contains(s))
    							actualEntities.add(s);
    					}
    				}
    				else {
    					List<String> singleWordEntities = Utility.findPOSTags(s);
    					for (String w : singleWordEntities) {
    						if (databaseMap.containsKey(w.toLowerCase())) {
    							String output = null; 
    							if (!entityToLinkMap.containsKey(w.toLowerCase())) {								
    								output = databaseMap.get(w.toLowerCase());
    								if (!actualEntities.contains(w))
    									actualEntities.add(w);
    								entityToLinkMap.put(w.toLowerCase(),output);
    							}
    							else {
    								if (!actualEntities.contains(w))
    									actualEntities.add(w);
    							}
    						}
    					}
    					
    				}
    			}
    			paragraphEntitiesMap.put(paraIndex, actualEntities);
    		}		
			 
			
    	}
	}
	
}
