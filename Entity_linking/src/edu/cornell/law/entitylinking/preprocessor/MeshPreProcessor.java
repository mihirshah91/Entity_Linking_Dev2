package edu.cornell.law.entitylinking.preprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MeshPreProcessor {
  
  static class MeshData{
    String descName;
    String meshId;
    HashSet<String> scope;
    public MeshData(String descName, String meshId, HashSet<String> scope) {
      this.descName = descName;
      this.meshId = meshId;
      this.scope = scope;
    }
    
  }
  
public static Map<String,String> meshMap =  new TreeMap<String,String>();
public static Map<String,MeshData> newmeshMap =  new TreeMap<String,MeshData>();

public static void generateMeshMap(String path) {
	
	class DescRecord {
		private String descName;
		private String meshID;
		
		public String getName() {
			return this.descName;
		}
		
		public void setName(String name) {
			this.descName  = name;
		}
		
		public String getMeshID() {
			return this.meshID;
		}
		
		public void setMeshID(String meshID) {
			this.meshID  = meshID;
		}
	}
	
	try {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();

		DefaultHandler handler = new DefaultHandler() {
		
		List<DescRecord> recList = new ArrayList<DescRecord>();
		DescRecord rec = null;
		boolean prevEleIsDescRec = false;
		boolean descName = false;
		boolean stringUnderDescName = false;
		boolean dString = false;
		boolean prevEleIsconcept = false;
		boolean conceptUI = false;

		public void startElement (String uri, String localName,String qName, Attributes attributes) throws SAXException {

			if (qName.equals("DescriptorRecord")) {
				rec = new DescRecord();
				prevEleIsDescRec = true;
			}
							
			if (qName.equals("DescriptorName") && prevEleIsDescRec == true) {
				descName = true;
				stringUnderDescName = true;
				prevEleIsDescRec = false;
			}
			
			if (qName.equals("String") && stringUnderDescName==true) {
				  dString = true;
				  stringUnderDescName=false;
			}
			
			if (qName.equals("ConceptList")) {
				prevEleIsconcept = true;
			}

			if (qName.equals("ConceptUI") && prevEleIsconcept == true) {
				conceptUI = true;
				prevEleIsconcept = false;
			}
		}

		public void endElement(String uri, String localName,
			String qName) throws SAXException {
			if (qName.equals("DescriptorRecord")) {
				recList.add(rec);
			}
		}

		public void characters(char ch[], int start, int length) throws SAXException {

			if (descName) {
				descName = false;
			}
			
			if (dString) {
				rec.setName(new String(ch, start, length));
				dString = false;
			}

			if (conceptUI) {
				rec.setMeshID(new String(ch, start, length));
				meshMap.put(stringInvert(rec.getName().trim()),rec.getMeshID().trim());
				conceptUI = false;
			}

		}
		
		private String stringInvert (String in) {
			if (in.contains(",")) {
				int index = in.indexOf(',');
				StringBuffer sb = new StringBuffer();
				sb.append(in.subSequence(index+1, in.length()));
				sb.append(" ");
				sb.append(in.subSequence(0, index));
				return sb.toString().toLowerCase();
			}
			return in.toLowerCase();
		}

	  };

	  saxParser.parse(path, handler);
	  
	/*  BufferedWriter writer = new BufferedWriter(new FileWriter("./preprocessedMaps/Mesh_Map.out"));
	  System.out.println("Size of meshMap: "+meshMap.size());
		System.out.println("***************MESH LIST**************");
		for (String key : meshMap.keySet()) {
			writer.write(key + "\t" + meshMap.get(key)+"\n");
		}
		
		writer.close();*/
	 
	     } catch (Exception e) {
	       e.printStackTrace();
	     }
}

/*public static void generateMeshMap1(String path) throws IOException {
	
	try{
	  
		@SuppressWarnings("resource")
		BufferedReader reader = new BufferedReader(new FileReader("./stopwordout"));
		BufferedWriter writer = new BufferedWriter(new FileWriter("./preprocessedMaps/mesh_Map.out"));
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		Document doc = docBuilder.parse(path);  

		//Retrieve all stop words and store
		String line = null;  
		HashSet<String> stopwords = new HashSet<String>();
		while ((line = reader.readLine()) != null)  
		{  
		    stopwords.add(line);
		} 
		  
		// Retrieve all the section elements for the given part file
		NodeList DescRecList = doc.getElementsByTagName("DescriptorRecord");
		
		int DescRecIndex = 0;
		while (DescRecIndex < DescRecList.getLength()) {
			  
			Node nDescRec = DescRecList.item(DescRecIndex);
			Element eElement = (Element) nDescRec;
			
			// Retrieve the <DescriptorName> and <ConceptList> tags under the current Descriptor Record element
			NodeList descName = (NodeList)(eElement.getElementsByTagName("DescriptorName"));
			String tempdescName = descName.item(0).getTextContent().trim();
			
			NodeList meshId = (NodeList)(eElement.getElementsByTagName("ConceptUI"));
			String tempmeshId = meshId.item(0).getTextContent().trim();
			
			NodeList scope = (NodeList)(eElement.getElementsByTagName("ScopeNote"));
			
			StringBuffer context = new StringBuffer("");
			String tempscope;
			if(scope != null && scope.item(0)!= null){
			   tempscope = scope.item(0).getTextContent().trim();
			   tempscope = tempscope.replaceAll("[$&+,:;=?@#|'<>.^*()%!/-]", "").toLowerCase();
			   context.append(tempscope);
			}
        
	        NodeList annotation = (NodeList)(eElement.getElementsByTagName("Annotation"));
	        
	        String annot;
	        if(annotation != null && annotation.item(0) != null ){
	          
	           annot = annotation.item(0).getTextContent().trim();
	           annot = annot.replaceAll("[$&+,:;=?@#|'<>.^*()%!/-]", "").toLowerCase();
	           context.append(annot);
	        }
	        
	        //Split;Remove punctuation and stop words
	        String[] allContextWords = context.toString().toLowerCase().split("\\s+");
	        
	        HashSet<String> contentWords = new HashSet<String>();
	        for(String word: allContextWords){
	          if(!stopwords.contains(word) && !word.equals("")){
	            contentWords.add(word);
	          }
	        }
				
	        MeshData current = new MeshData(tempdescName.toLowerCase(),tempmeshId,contentWords);
	        newmeshMap.put(tempdescName.toLowerCase(), current);
					meshMap.put(tempdescName.toLowerCase(), tempmeshId);				
					DescRecIndex++;
		}
			
		System.out.println("Size of meshMap: "+meshMap.size());
		System.out.println("***************MESH LIST**************");
		for (String key : meshMap.keySet()) {
			System.out.println(key+" : "+meshMap.get(key));
			MeshData current =  newmeshMap.get(key);
			writer.write(key + "\t" + current.meshId+ "\t" + current.scope+"\n");
		}
		
		writer.close();
		
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}*/
}
