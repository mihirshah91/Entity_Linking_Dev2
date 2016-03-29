package edu.cornell.law.entitylinking.preprocessor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import edu.cornell.law.entitylinking.utils.Constants;

public class AgrovocLinker {

	
public static String getAgrovocLinkDisambiguate(String label, HashSet<String> context)
	{
		
	 QueryExecution qexec = null;
	 Map<String,List<String>> entityToLanguageMap = new HashMap<>();
		
	 
	// query no.1 to get the entity url 
	String sparqlquery = Constants.prefixTextAgrovoc + "SELECT ?" + Constants.selectTerm + " WHERE { ?" + Constants.selectTerm + " skos:prefLabel \"" +   label +   "\" @en .}" ;
	String mainEntityUri = executeQuery(sparqlquery,Constants.Agrovoc_EndPoint);
	String mainentityid = extractId(mainEntityUri);
	System.out.println("id= " + mainEntityUri);
	
	
	// query no.2 to get the scope of entity 
	String scope = executeQueryUtil(mainEntityUri,true);
	System.out.println("scope of main entity = " + scope);
	
	if(scope!=null)
		buildMap(scope,mainentityid,entityToLanguageMap);
		
	// query no.3 to get the broder entiyid
	String broaderEntityUri = executeQueryUtil(mainEntityUri,false);
	String broaderentityid = extractId(broaderEntityUri); 
	
	// query no.4 to get scope of broader
	scope = executeQueryUtil(broaderEntityUri,true);
	if(scope!=null)
		buildMap(scope,broaderentityid,entityToLanguageMap);
	
	// query no.5 to get the narrower entiyid
	String narrowerEntityUri = executeQueryUtil(mainEntityUri,false);
	String narrowerentityid = extractId(broaderEntityUri); 
	
	// query no.6 to get scope of broader
		scope = executeQueryUtil(narrowerEntityUri,true);
		if(scope!=null)
			buildMap(scope,narrowerentityid,entityToLanguageMap);
	
		System.out.println(entityToLanguageMap);
	
	return "";	
		
	
	}

public static String executeQueryUtil(String uri, boolean filterFlag )
{
	String sparqlQuery = Constants.prefixTextAgrovoc + giveSelectClause(formUrl(uri), Constants.skosScopeNoteTerm,filterFlag) ;   
	String scope = executeQuery(sparqlQuery,Constants.Agrovoc_EndPoint);
	return scope;
}


public static void buildMap(String scope,String entityid, Map<String,List<String>> entityToLanguageMap)
{
	 entityToLanguageMap.put(entityid, formList(scope));
	 String sparqlScopeQuery = "";
	 Pattern pattern = Pattern.compile(Constants.regexScope);
	 Matcher matcher = pattern.matcher(scope);
	 
	 while(matcher.find())
	 {
		 String temp = matcher.group();
		 String tempurl = Constants.AGROVOCURL + temp.substring(1,temp.length());
		 sparqlScopeQuery = Constants.prefixTextAgrovoc + giveSelectClause(tempurl, Constants.skosScopeNoteTerm,true) ;   
		 scope = executeQuery(sparqlScopeQuery,Constants.Agrovoc_EndPoint);
		 
		 if(scope!=null)
			 entityToLanguageMap.put( temp.substring(1,temp.length()-1) , formList(scope));
		 
	 }
}



public static String extractId(String uri)
{
	int index = uri.lastIndexOf("c_");
	String sub = uri.substring(index,uri.length());
	return sub;
	
}


public static List<String> formList(String scope)
{
	
	String split[] = scope.split("\\s");
	List<String> list = Arrays.asList(split);
	return list;
	
	
}



public static String formUrl(String id)
{
	return "<" + id + ">";
}


public static String executeQuery(String sparqlQuery, String endpoint)
{
	Query query = QueryFactory.create(sparqlQuery);
	QueryExecution qexec =   QueryExecutionFactory.sparqlService(endpoint, query);
	 ResultSet results = qexec.execSelect();
	 String entityid = null;
	 
	 while(results.hasNext())
	 {
		 QuerySolution soln = results.nextSolution() ;
		 entityid = new String(soln.get("?" + Constants.selectTerm).toString());
		 System.out.println("id=" +entityid);
	 }
	 
	 return entityid;
}


public static String giveSelectClause(String label, String skosTerm, boolean filterFlag)
{
	String select =  "SELECT ?" + Constants.selectTerm + " WHERE {"
					+ label +  " skos:" + skosTerm + " ?" + Constants.selectTerm + " . ";
	
	if(filterFlag)
		select = select + "FILTER(lang(?"+ Constants.selectTerm + ") = 'en') }" ;

	return select;
}


public static void main(String args[])
{
	getAgrovocLinkDisambiguate("banks", new HashSet<String>(Arrays.asList("mihir","hello world")));
	
}
	
	
}
