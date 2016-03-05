package edu.cornell.law.entitylinking;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.Set;
//import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.cornell.law.entitylinking.utils.*;

public class XMLParser {
	/**
	 * @param intermediateFile
	 * @param entityToLinkMap
	 */
	
	public static int pTagssize;
	public static int extractTagsSize;
	public static int tableTagsSize;
	
	
	
	public static void postProcess(File intermediateFile,HashMap<String, String> entityToLinkMap, String outputTitleFolder){
		try{
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		Document doc = docBuilder.parse(intermediateFile);
		
		
		
		Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        doc.setXmlStandalone(true);
        FileOutputStream fos = new FileOutputStream(LoadParameters.params.get("output_folder")+"/"+outputTitleFolder+"/"+intermediateFile.getName().split("temp")[0]+".xml");
        System.out.println(LoadParameters.params.get("output_folder")+"/"+outputTitleFolder+"/"+intermediateFile.getName().split("_")[0]+".xml");
        Writer out = new OutputStreamWriter(fos,"UTF8");
        tf.transform(new DOMSource(doc), new StreamResult(out));
        out.close();
        intermediateFile.delete();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @param partFileName
	 * @return
	 * @throws IOException 
	 */
	public static void xmlRead(File partFileName, String titleId, String sectionId, String database) throws IOException {
		
		try {
			HashSet<String> entitiesToBeRemoved = new HashSet<String>();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			
			Document doc = docBuilder.parse(partFileName);

			System.out.println("In XML Parser");
			// Retrieve all the section elements for the given part file
			NodeList nList = doc.getElementsByTagName("section");

			int sectionIndex = 0;
			// Iterate over the section elements
			HashMap<Integer, String> paraMap = new HashMap<Integer, String>();
			while (sectionIndex < nList.getLength()) {
				Node nNode = nList.item(sectionIndex);

				Element eElement = (Element) nNode;

				// Retrieve all the p tags under the current section element
				NodeList pTags = eElement.getElementsByTagName("p");
				NodeList extractTags = eElement.getElementsByTagName("extract");
				NodeList tableTags = eElement.getElementsByTagName("table");

				pTagssize = pTags.getLength();
				extractTagsSize = extractTags.getLength();
				tableTagsSize = tableTags.getLength();
				
				
				
				int pTagIndex = 0;
				//int extractTagIndex=0;
				
				while (pTagIndex < pTags.getLength()) {
					
					/* To avoid crossing the boundaries of "definiendum" tags */
					NodeList defin = ((Element) (pTags.item(pTagIndex)
							.getChildNodes()))
							.getElementsByTagName("definiendum");
					if (defin.getLength() > 0) {
						for (int c = 0; c < defin.getLength(); c++) {
							String name = defin.item(c).getTextContent();
							entitiesToBeRemoved.add(name.toLowerCase());
							}
					}
					
					/* To avoid crossing the boundaries of "linkedEntity" tags */
					NodeList linkedEntity = ((Element) (pTags.item(pTagIndex)
							.getChildNodes()))
							.getElementsByTagName("linkedEntity");
					if (linkedEntity.getLength() > 0) {
						for (int c = 0; c < linkedEntity.getLength(); c++) {
							String name = linkedEntity.item(c).getTextContent();
							entitiesToBeRemoved.add(name.toLowerCase());
							}
					}
					
					StringBuffer buffer = new StringBuffer();
					buffer.append(pTags.item(
							pTagIndex).getTextContent());
					
					paraMap.put(pTagIndex, buffer.toString());
					pTagIndex++;
				}
				
				
				//similar for extract tags
				for(int i=0; i < extractTags.getLength();i++) {
					
					
					
					StringBuffer buffer = new StringBuffer();
					buffer.append(extractTags.item(i).getTextContent());
					paraMap.put(pTagIndex, buffer.toString());
					pTagIndex++;
				}
			
				//similar for extract table tags
				
				for(int i=0; i < tableTags.getLength();i++) {
					
					
					
					StringBuffer buffer = new StringBuffer();
					buffer.append(tableTags.item(i).getTextContent());
					paraMap.put(pTagIndex, buffer.toString());
					pTagIndex++;
				}
				
				
			sectionIndex++;
			}
			
			List<LinkingThread> workerThreads = new ArrayList<LinkingThread>();
			List<HashMap<Integer, String>> dataList = new ArrayList<HashMap<Integer, String>>();
			for(int i=0;i<Controller.MAX_THREADS;i++){
				HashMap<Integer, String> map1 = new HashMap<Integer, String>();
				dataList.add(map1);
			}
			
			for(int i=0;i<paraMap.size();i++){
				int dataListIndex = i%Controller.MAX_THREADS;
				HashMap<Integer, String> data = dataList.get(dataListIndex);
				data.put(i, paraMap.get(i));
			}
			
			System.out.println("List size : "+paraMap.size());
			for(int i=0;i<Controller.MAX_THREADS;i++){
				LinkingThread thread = new LinkingThread((i+1)+"", dataList.get(i), database, entitiesToBeRemoved);
				workerThreads.add(thread);
				thread.start();
			}
			
			for(int i=0;i<workerThreads.size();i++){
				workerThreads.get(i).join();
			}
			
			for(int i=0;i<workerThreads.size();i++){
				Controller.entityToLinkMap.putAll(workerThreads.get(i).getLinkMap());
				Controller.paragraphEntities.putAll(workerThreads.get(i).getParaEntitiesMap());
			}
			
			/*
			System.out.println("*************************List of entities to be removed***************************");
			
			Iterator<String> iterator2 = entitiesToBeRemoved.iterator();
			while(iterator2.hasNext()){
				String key = iterator2.next();
				System.out.println(key);
			}
			System.out.println("Entity to link map size : "+Controller.entityToLinkMap.size());
			
			System.out.println("*************************List of linked entities***************************");
			
			Iterator<String> iterator = Controller.entityToLinkMap.keySet().iterator();
			while(iterator.hasNext()){
				String key = iterator.next();
				System.out.println(key+" ~~ "+Controller.entityToLinkMap.get(key));
			}
			System.out.println("Entity to link map size : "+Controller.entityToLinkMap.size());
			
			System.out.println("*************************List of paragraph entities***************************");
			
			Iterator<Integer> iterator1 = Controller.paragraphEntities.keySet().iterator();
			while(iterator1.hasNext()){
				Integer key = iterator1.next();
				System.out.println(key+" ~~ "+Controller.paragraphEntities.get(key).toString());
			}
			System.out.println("Entity to link map size : "+Controller.paragraphEntities.size());
			*/
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
