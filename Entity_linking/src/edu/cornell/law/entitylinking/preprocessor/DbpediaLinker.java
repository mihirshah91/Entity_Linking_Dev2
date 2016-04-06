package edu.cornell.law.entitylinking.preprocessor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public class DbpediaLinker {
  
  public static String getDbpediaLinkDisambiguate(String label,HashSet<String> contextWords){
      String bestUri = null;
      int maxScore = Integer.MIN_VALUE;
      
      QueryExecution qexec = null;
      long httpExceptions = 0;
      try {
      String sparqlQueryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"  +
                                  "\n" + 
                                  "select distinct ?s ?dis ?abs ?comment where {" + 
                                  "\n" +
                                  "?s rdfs:label \""+label+"\"@en . "+
                                  "\n" +
                                  "?s <http://dbpedia.org/ontology/wikiPageDisambiguates> ?dis . "+
                                  "\n" + 
                                  "?dis <http://dbpedia.org/ontology/abstract> ?abs. "+
                                  "\n" +
                                  "?dis <http://www.w3.org/2000/01/rdf-schema#comment> ?comment. "+
                                  "\n" +
                                  "FILTER (LANG(?abs)='en' && LANG(?comment)='en') "+"} ";


                    Query query = QueryFactory.create(sparqlQueryString);

                    qexec =   QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);


                    
                        ResultSet results = qexec.execSelect();
                        for ( ; results.hasNext() ; )
                    {
                        QuerySolution soln = results.nextSolution() ;
                        //System.out.println("URI:"+soln.get("?dis"));
                        StringBuilder context = new StringBuilder(soln.get("?abs").toString());
                        context.append(" ");
                        context.append(soln.get("?comment").toString());
                        /*System.out.println("Abstract:"+soln.get("?abs"));
                        System.out.println("Comment:"+soln.get("?comment"));*/
                        
                        HashSet<String> presentContext = extractContentWords(context.toString());
                        presentContext.retainAll(contextWords);
                        int overlap = presentContext.size();
                        if(overlap > maxScore){
                          maxScore = overlap;
                          bestUri = soln.get("?dis").toString();
                        }
                    }
    }
    catch(HttpException e){
        httpExceptions += 1;
        e.printStackTrace();
    }    
    catch(Exception e){
        
        e.printStackTrace();
    }    

    finally { qexec.close(); System.out.println("Number of httpExceptions is"+httpExceptions);}

      
      System.out.println("Disambiguation best uri for label " + label + " is " + bestUri);
      
      return bestUri;
  }
  
  public static String getDbpediaLink(String label){
    String uri = null;
    
    
    QueryExecution qexec = null;
    long httpExceptions = 0;
    try {
    String sparqlQueryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"  +
                                "\n" + 
                                "select distinct ?s ?type where {" + 
                                "\n" +
                                "?s rdfs:label \""+label+"\"@en . "+
                                "\n" +
                                 "} ";


                  Query query = QueryFactory.create(sparqlQueryString);

                  qexec =   QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);


                  
                      ResultSet results = qexec.execSelect();
                      for ( ; results.hasNext() ; )
                  {
                      QuerySolution soln = results.nextSolution() ;
                        
                        uri = soln.get("?s").toString();
                        if(uri.toString().startsWith("http://dbpedia.org/resource/") && !uri.toString().startsWith("http://dbpedia.org/resource/Category:")){
                          //System.out.println("URI:"+soln.get("?s"));
                          break;
                        }
                      uri = null;
                  }
  }
  catch(HttpException e){
      httpExceptions += 1;
      e.printStackTrace();
  }    
  catch(Exception e){
      
      e.printStackTrace();
  }    

  finally { qexec.close(); System.out.println("Number of httpExceptions is"+httpExceptions);}

    
    
    
    return uri;
}
  
  
  public static HashSet<String> extractContentWords(String content){
    
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader("./stopwordout"));
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    //Retrieve all stop words and store
    String line = null;  
    HashSet<String> stopwords = new HashSet<String>();
    try {
      while ((line = reader.readLine()) != null)  
      {  
          stopwords.add(line);
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } 
    
    StringBuffer context = new StringBuffer("");
    content = content.replaceAll("[$&+,:;=?@#|'<>.^*()%!/-]", "").toLowerCase();
    context.append(content);
  
    //Split;Remove punctuation and stop words
    String[] allContextWords = context.toString().toLowerCase().split("\\s+");
    
    HashSet<String> contentWords = new HashSet<String>();
    for(String word: allContextWords){
      if(!stopwords.contains(word) && !word.equals("")){
        contentWords.add(word);
      }
    }
   
    //System.out.println("Content: "+content);
    //System.out.println("contentWords:"+contentWords);
    
    
    return contentWords;
  }
     
}
