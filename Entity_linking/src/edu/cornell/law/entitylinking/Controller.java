package edu.cornell.law.entitylinking;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cornell.law.entitylinking.preprocessor.*;
import edu.cornell.law.entitylinking.utils.*;

public class Controller {
	public static Set<String> entitiesToBeMarked = new HashSet<String>();
	public static HashMap<String,String> entityToLinkMap = new HashMap<String,String>();
	public static HashMap<Integer,ArrayList<String>> paragraphEntities = new HashMap<>();

	public static int MAX_THREADS = 1;
	public final static String LIICORNELL_SUBJECT = "<http://liicornell.org/liicfr/";
	public final static String LIICORNELL_PREDICATE = "<http://liicornell.org/liicfr/containsEntity> ";
	public final static String LIICORNELL_DRUGBANK = "<http://www4.wiwiss.fu-berlin.de/drugbank/resource/drugs/";
	public final static String LIICORNELL_MESH = "<http://id.nlm.nih.gov/mesh/";
	public final static String LLICORNELL_AGROVOC = "";
	public static Integer pIndex = 0;
	public static Integer exIndex;
	public static Integer tableIndex;
	
	public static void main(String args[]) throws Exception {
		
		long startTime = System.currentTimeMillis();
		
		if(args.length == 0){
			System.out.println("Please provide parameters file path");
			System.exit(0);
		}
		
		String paramsFilePath = args[0];
		
		System.setProperty("wordnet.database.dir", "WordNet-3.0\\dict\\");
		LoadParameters.readParameters(paramsFilePath);
		
		//Initialize NLP Toolkits
		Utility.runCoreNLP();
		
		String database = LoadParameters.params.get("database");
		String path = "";
		if(database.equals("Drugbank"))
		{
			path = LoadParameters.params.get("drugbank_ontology");
			DrugbankPreProcessor.generateDrugsMap(path);
		}
		else if(database.equals("Mesh"))
		{
			path = LoadParameters.params.get("mesh_ontology");
			MeshPreProcessor.generateMeshMap(path);
		}
		else if(database.equals("Agrovoc"))
		{
			path = LoadParameters.params.get("agrovoc_ontology");
			AgrovocPreProcessor.generateAgrovocMap(path);
		}
		else if(database.equals("Dbpedia")){
		  //No preprocesssing
		  database = "Dbpedia";
		}
		
		MAX_THREADS=Integer.parseInt(LoadParameters.params.get("max_thread"));
		String inputTitleFolder = LoadParameters.params.get("input_title_folder");
		File titleFolder = new File(inputTitleFolder); 
		System.out.println(titleFolder);
		String titleId="";
		try{
			titleId = inputTitleFolder.substring(inputTitleFolder.indexOf("title")+5); 
			System.out.println("Title ID: "+titleId);
		}
		catch(Exception e){
			titleId = "unknown";
			System.out.println("Input folder is not of titlex form. Title ID will be unknown.");
			e.printStackTrace();
		}
		
		BufferedWriter tripletsWriter = null;
		String tripletsPath = LoadParameters.params.get("output_rdf");
		tripletsWriter = new BufferedWriter(new FileWriter(tripletsPath+"/NTriples_"+database+".nt"));
		
		File[] files = titleFolder.listFiles();
		
		//Create an Output Folder of the same TitleId
		String outputTitleFolder = inputTitleFolder.substring(inputTitleFolder.indexOf("title")); 
		if (files.length != 0) {
			boolean successOutputRdfFolder = (new File(LoadParameters.params.get("output_folder")+"/"+outputTitleFolder)).mkdirs();
			if (!successOutputRdfFolder) {
				System.out.println("Output folder was not created since the structure already exists");
			}
        }
		
		for(int i=0;i<files.length;i++){
			try{
			if(!files[i].getName().contains(".xml")){
				continue;
			}
			
			HashMap<String, Integer> localEntities = new HashMap<String, Integer>();
			
			String sectionId = files[i].getName().split(".xml")[0];
			sectionId = sectionId.split("_")[1];
			System.out.println(files[i].getName());
	
			// Returns a map of entities and their links
			XMLParser.xmlRead(files[i],titleId,sectionId, database);
			
			exIndex = XMLParser.pTagssize;
			tableIndex= XMLParser.pTagssize + XMLParser.extractTagsSize;
			
			
			System.out.println("number of ptags" + XMLParser.pTagssize);
			System.out.println("number of extract tags: " + XMLParser.extractTagsSize);
			System.out.println("number of table tags: " + XMLParser.tableTagsSize);
			
			//Files from title folder
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(files[i]),"UTF8"));
			// The marked up xml	
			File writerFile = new File(LoadParameters.params.get("output_folder")+"/"+outputTitleFolder+"/"+files[i].getName().split(".xml")[0]+"temp.xml");
			System.out.println(writerFile);
			
			FileOutputStream fos = new FileOutputStream(writerFile);  
			Writer out = new OutputStreamWriter(fos, "UTF8");
			
			String line = null;
			System.out.println("Started to write new file...");
			boolean eflag=false ; // flag for not ignoring lines within extract tags
			boolean tflag =false; // flag for not ignoring lines within table tags
			boolean pflag =false;
			//System.out.println(entityToLinkMap.size());
			int line_number = 0;
			
			while ((line = reader.readLine()) != null) {	
				
				line_number++;
				if(line_number == 38)
				{
					System.out.println("here");
				}
				
				   if ( (!line.contains("<head>") && !line.contains("<origin"))  && (line.contains("<p") || line.contains("<extract") || line.contains("<table") || eflag || tflag  ) ) {
				    
					   ArrayList<String> toTag = new ArrayList<String>();
						if (line.contains("<extract"))

						{
							eflag = true;
							

						} else if (line.contains("<table")) {
							tflag = true;
							
						} 
						else if(line.contains("<p"))
						{
							toTag=paragraphEntities.get(pIndex++);
							
						}
							

						if (line.contains("</extract>")) {
							eflag = false;
							exIndex++;
							
						} 
						else if (line.contains("</table>")) {
							tflag = false;
							tableIndex++;
							
						}
						
						
						if(eflag)
							toTag=paragraphEntities.get(exIndex);
						else if(tflag)
							toTag=paragraphEntities.get(tableIndex);
					
							
						
					   
				       //System.out.println(toTag);
				       
					   if(toTag!=null){
						   for (String tagMe : toTag) {
							   
							   if(line.contains(tagMe))
							   {
							   if(entityToLinkMap.containsKey(tagMe.toLowerCase())){
								   String patternString = "\\b"+tagMe+"\\b";
							       Pattern pattern = Pattern.compile(patternString);
							       Matcher matcher = pattern.matcher(line);
							       int occur = 1;
							       if(!localEntities.containsKey(tagMe.toLowerCase()))
							       {
							    	    String output = "";
										if(database.equals("Drugbank")) {
											 output = LIICORNELL_SUBJECT + titleId +"_CFR_" + sectionId + "> " + LIICORNELL_PREDICATE + LIICORNELL_DRUGBANK + entityToLinkMap.get(tagMe.toLowerCase()) + ">.";
									    }
										else if(database.equals("Mesh")) {
											 output = LIICORNELL_SUBJECT + titleId +"_CFR_" + sectionId + "> " + LIICORNELL_PREDICATE + LIICORNELL_MESH + entityToLinkMap.get(tagMe.toLowerCase()) + ">.";
										}
										else if(database.equals("Agrovoc")) {
											 output = LIICORNELL_SUBJECT + titleId +"_CFR_" + sectionId + "> " + LIICORNELL_PREDICATE + " <" + entityToLinkMap.get(tagMe.toLowerCase()) + ">.";
										}
										else if(database.equals("Dbpedia")) {
											output = LIICORNELL_SUBJECT + titleId +"_CFR_" + sectionId + "> " + LIICORNELL_PREDICATE + " <" + entityToLinkMap.get(tagMe.toLowerCase()) + ">.";
										}
										tripletsWriter.write(output + "\n");
							    	   localEntities.put(tagMe.toLowerCase(), occur);
							       }
							       else{
							    	   // The key is there, Increment the count
							    	   occur = localEntities.get(tagMe.toLowerCase())+1;
							    	   localEntities.put(tagMe.toLowerCase(), occur);
							       }
							       
							       
							       if(database.equals("Drugbank")) {
								       line = matcher.replaceAll("<linkedEntity src=\"Drugbank\" identifier=\"Entity:" 
												+ entityToLinkMap.get(tagMe.toLowerCase()) + "\" "
											    + "occur=\""+occur+"\">"
											    + tagMe + "</linkedEntity>");	
							       }
								   else if(database.equals("Mesh")) {
									   line = matcher.replaceAll("<linkedEntity src=\"Mesh\" identifier=\"Entity:" 
												+ entityToLinkMap.get(tagMe.toLowerCase()) + "\" "
											    + "occur=\""+occur+"\">"
											    + tagMe + "</linkedEntity>");													
								   }
								   else if(database.equals("Agrovoc")) {
									   if(!entityToLinkMap.get(tagMe.toLowerCase()).contains("dbpedia")){
										   line = matcher.replaceAll("<linkedEntity src=\"Agrovoc\" identifier=\"Entity:" 
													+ entityToLinkMap.get(tagMe.toLowerCase()).substring(entityToLinkMap.get(tagMe.toLowerCase()).indexOf("#")+1,entityToLinkMap.get(tagMe.toLowerCase()).length()) + "\" "
												    + "occur=\""+occur+"\">"
												    + tagMe + "</linkedEntity>");
									   }
									   else{
										   line = matcher.replaceAll("<linkedEntity src=\"Agrovoc\" identifier=\"Entity:" 
													+ entityToLinkMap.get(tagMe.toLowerCase()) + "\" "
												    + "occur=\""+occur+"\">"
												    + tagMe + "</linkedEntity>");
									   }
								   }
								   else if(database.equals("Dbpedia")) {
								     //System.out.println("Im here!");
									   line = matcher.replaceAll("<linkedEntity src=\"Dbpedia\" identifier=\"Entity:" 
											   + entityToLinkMap.get(tagMe.toLowerCase()) + "\" "
											   + "occur=\""+occur+"\">"
											   + tagMe + "</linkedEntity>");                         
								   }

								   //System.out.println("Replaced Line: "+line);

								   									
							   }
							  }
						   }
					   }
					  // pIndex++;   
				   }
				   out.write(line+"\n");
				   out.flush();
			}
						
			reader.close();
			out.close();
			
			XMLParser.postProcess(writerFile, entityToLinkMap, outputTitleFolder);
			System.out.println();
			System.out.println("Output file generated for "+files[i].getName());
			}
			catch(Exception e){
				e.printStackTrace();
			}
			
			entityToLinkMap.clear();
			paragraphEntities.clear();
			pIndex = 0;
		}
		
		tripletsWriter.close();
		System.out.println();
		System.out.println("Done. Please check "+LoadParameters.params.get("output_folder")+" folder for output.");
		
		long stopTime = System.currentTimeMillis();
		System.out.println("stop time is " + stopTime);
		long elapsedTime = stopTime - startTime;
		
		 System.out.println("Time required in secons is : " + elapsedTime/1000.0);
		
		
		
	}
	
	
	

}