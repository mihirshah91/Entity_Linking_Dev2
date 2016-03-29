package edu.cornell.law.entitylinking;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import edu.cornell.law.entitylinking.preprocessor.AgrovocPreProcessor;
import edu.cornell.law.entitylinking.preprocessor.DbpediaLinker;
import edu.cornell.law.entitylinking.preprocessor.DrugbankPreProcessor;
import edu.cornell.law.entitylinking.preprocessor.MeshPreProcessor;
import edu.cornell.law.entitylinking.utils.LoadParameters;
import edu.cornell.law.entitylinking.utils.Utility;

class LinkingThreadNew extends Thread{
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
	
	public LinkingThreadNew(String threadId,HashMap<Integer, String> queryMap, String database, HashSet<String> entitiesToBeRemoved){
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
    				//for printing map
    			    try {
					//	printMap(databaseMap,threadId);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						System.out.println(e);
						
					}

    				
    				
    			}
    			else if(database.equals("Agrovoc"))
    			{
    				// Find inner and short noun phrases for other Ontologies
    				potentialEntities = Utility.getInnerNounPhrases(content);
    				databaseMap = AgrovocPreProcessor.AgrovocMap;
    				  try {
    						printMap(databaseMap,threadId);
    						} catch (Exception e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    							System.out.println(e);
    							
    						}

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
    				
    				
    			}
    			paragraphEntitiesMap.put(paraIndex, actualEntities);
    		}		
			 
			
    	}
	}
	
	
	public void printMap(Map<String,String> dbmap,String id) throws Exception
	{
		if(id.equals("1"))
		{
		
			String fileName = LoadParameters.params.get("output_folder") + "/Agrovocmap.txt";
		      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName))); 

			for(Map.Entry<String,String> m: dbmap.entrySet())
				{
				out.println(m.getKey() + " = " + m.getValue());
				
				}
		
		}
		
		
		
	}
	
	
}
