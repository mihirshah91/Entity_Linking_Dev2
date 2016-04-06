package edu.cornell.law.entitylinking.preprocessor;

import java.util.ArrayList;
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
		Map<String,HashSet<String>> entityToLanguageMap = new HashMap<>();


		// query no.1 to get the entity url 
		String sparqlquery = Constants.prefixTextAgrovoc + "SELECT ?" + Constants.selectTerm + " WHERE { ?" + Constants.selectTerm + " skos:prefLabel \"" +   label +   "\" @en .}" ;
		List<String> list = executeQuery(sparqlquery,Constants.Agrovoc_EndPoint);


		for( String mainEntityUri :list)
		{
			String mainentityid = extractId(mainEntityUri);

			System.out.println("id= " + mainEntityUri);


			// query no.2 to get the scope of entity 
			List<String> result = executeQueryUtil(mainEntityUri,true,  Constants.skosScopeNoteTerm);
			for(String tempscope: result)
				buildMap(tempscope,mainentityid,entityToLanguageMap,true);

			// query no.3 to get the broder entiyid
			result = executeQueryUtil(mainEntityUri,false, Constants.skosBroaderTerm);


			for(String broaderEntityUri: result)
			{
				String broaderentityid = extractId(broaderEntityUri); 

				// query no.4 to get scope of broader
				List<String> tempresult = executeQueryUtil(broaderEntityUri,true, Constants.skosScopeNoteTerm);
				for(String tempscope: tempresult)
					buildMap(tempscope,broaderentityid,entityToLanguageMap,false);
			}


			// query no.5 to get the narrower entiyid
			/*result = executeQueryUtil(mainEntityUri,false,Constants.skosNarrowerTerm);
	for(String narrowerEntityUri: result)
	{	
		String narrowerentityid = extractId(narrowerEntityUri); 
		// query no.6 to get scope of narrower
		List<String> tempresult = executeQueryUtil(narrowerEntityUri,true,Constants.skosScopeNoteTerm);
		for(String tempscope: tempresult)
			buildMap(tempscope,narrowerentityid,entityToLanguageMap,false);
	}*/

			// query no. 6 to get DBPedia links if present
			String bestUri = null;
			int maxScore = Integer.MIN_VALUE;
			result = executeQueryUtil(mainEntityUri,false,Constants.skosCloseMatchTerm);
			for(String dBPediaLink : result)
			{	
				String dbPediaQueryString = "select distinct ?dis ?abs ?comment where {" + 
						"\n" +
						"<"+dBPediaLink+">"+"<http://dbpedia.org/ontology/wikiPageDisambiguates> ?dis . "+
						"\n" + 
						"?dis <http://dbpedia.org/ontology/abstract> ?abs. "+
						"\n" +
						"?dis <http://www.w3.org/2000/01/rdf-schema#comment> ?comment. "+
						"\n" +
						"FILTER (LANG(?abs)='en' && LANG(?comment)='en') "+"} ";
				Query dbPediaQuery = QueryFactory.create(dbPediaQueryString);
				QueryExecution qexecution =   QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", dbPediaQuery);
				ResultSet resultsForDBPediaQuery = qexecution.execSelect();
				for ( ; resultsForDBPediaQuery.hasNext() ; )
				{
					QuerySolution dbSoln = resultsForDBPediaQuery.nextSolution() ;
					//System.out.println("URI:"+soln.get("?dis"));
					StringBuilder dbPediaLanguage = new StringBuilder(dbSoln.get("?abs").toString());
					dbPediaLanguage.append(" ");
					dbPediaLanguage.append(dbSoln.get("?comment").toString());   
					HashSet<String> dbPediaLanguageWords = DbpediaLinker.extractContentWords(dbPediaLanguage.toString());
					dbPediaLanguageWords.retainAll(context);
					/*int overlap = dbPediaLanguageWords.size();
					if(overlap > maxScore){
						maxScore = overlap;
						bestUri = dbSoln.get("?dis").toString();
					}*/
					entityToLanguageMap.put(dbSoln.get("?dis").toString(), dbPediaLanguageWords);
				}
			}
		}
		System.out.println(entityToLanguageMap);
		String uri = performWSD(entityToLanguageMap, context);
		//System.out.println("uri=" + uri);
		return uri;	
	}

	public static String performWSD( Map<String,HashSet<String>> entityToLanguageMap, HashSet<String> context)
	{
		int maxScore = Integer.MIN_VALUE;
		String uri = null;

		for(Map.Entry<String, HashSet<String>> temp: entityToLanguageMap.entrySet())
		{
			String id = temp.getKey();
			HashSet<String> presentContext = temp.getValue();
			presentContext.retainAll(context);
			int score = presentContext.size();
			if(score > maxScore)
			{
				maxScore = score;
				uri = id;
			}

		}
		return uri;
	}


	public static List<String> executeQueryUtil(String uri, boolean filterFlag, String term )
	{
		String sparqlQuery = Constants.prefixTextAgrovoc + giveSelectClause(formUrl(uri),term,filterFlag) ;   
		List<String> result = executeQuery(sparqlQuery,Constants.Agrovoc_EndPoint);
		return result;
	}


	public static void buildMap(String scope,String entityid, Map<String,HashSet<String>> entityToLanguageMap, boolean deepCheckFlag)
	{

		String sparqlScopeQuery = "";
		Pattern pattern = Pattern.compile(Constants.regexScope);
		Matcher matcher = pattern.matcher(scope);
		if(matcher.groupCount()==0)
			entityToLanguageMap.put(entityid, formList(scope));

		while(matcher.find() && deepCheckFlag)
		{
			String temp = matcher.group();
			String tempurl = Constants.AGROVOCURL + temp.substring(1,temp.length());
			sparqlScopeQuery = Constants.prefixTextAgrovoc + giveSelectClause(tempurl, Constants.skosScopeNoteTerm,true) ;   
			List<String> tempresult = executeQuery(sparqlScopeQuery,Constants.Agrovoc_EndPoint);


			for(String tempscope: tempresult)
			{
				entityToLanguageMap.put( "c_" + temp.substring(1,temp.length()-1) , formList(tempscope));
			}

		}
	}



	public static String extractId(String uri)
	{
		int index = uri.lastIndexOf("c_");
		String sub = uri.substring(index,uri.length());
		//ystem.out.println("sub=" + sub);
		return sub;

	}


	public static HashSet<String> formList(String scope)
	{
		scope = scope.replaceAll("@en", "");
		HashSet<String> set = DbpediaLinker.extractContentWords(scope);
		return set;


	}



	public static String formUrl(String id)
	{
		return "<" + id + ">";
	}


	public static List<String> executeQuery(String sparqlQuery, String endpoint)
	{
		Query query = QueryFactory.create(sparqlQuery);
		QueryExecution qexec =   QueryExecutionFactory.sparqlService(endpoint, query);
		ResultSet results = qexec.execSelect();
		List<String> result = new ArrayList<String>();

		while(results.hasNext())
		{
			QuerySolution soln = results.nextSolution() ;
			String temp = new String(soln.get("?" + Constants.selectTerm).toString());
			result.add(temp);
			//System.out.println("id=" +entityid);
		}

		return result;
	}


	public static String giveSelectClause(String label, String skosTerm, boolean filterFlag)
	{
		String select =  "SELECT ?" + Constants.selectTerm + " WHERE {"
				+ label +  " skos:" + skosTerm + " ?" + Constants.selectTerm + " . ";

		if(filterFlag)
			select = select + "FILTER(lang(?"+ Constants.selectTerm + ") = 'en') }" ;
		else
			select = select + "}";

		return select;
	}


	/*public static void main(String args[])
{
	getAgrovocLinkDisambiguate("absorption", new HashSet<String>(Arrays.asList("mihir","hello world")));

}*/


}
