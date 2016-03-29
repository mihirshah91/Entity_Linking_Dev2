package edu.cornell.law.entitylinking.utils;

public interface Constants {

	public String Agrovoc_EndPoint = "http://202.45.139.84:10035/catalogs/fao/repositories/agrovoc";
	
	public String prefixTextAgrovoc = "PREFIX guo:<http://www.w3.org/2008/05/skos-xl>" + "\n" +
			"PREFIX skos-xl:<http://www.w3.org/2008/05/skos-xl>" + "\n" +
			"PREFIX shian:<http://aims.fao.org/aos/agrovoc/>" + "\n" 
			+ "PREFIX ns:<http://aims.fao.org/aos/agrovoc/>" + "\n"
			+"PREFIX dc:<http://purl.org/dc/elements/1.1/>" + "\n"
			+"PREFIX dcterms:<http://purl.org/dc/terms/>" + "\n"
			+"PREFIX err:<http://www.w3.org/2005/xqt-errors#>" + "\n"
			+"PREFIX fn:<http://www.w3.org/2005/xpath-functions#>" + "\n"
			+"PREFIX foaf:<http://xmlns.com/foaf/0.1/>" + "\n"
			+"PREFIX fti:<http://franz.com/ns/allegrograph/2.2/textindex/>" + "\n"
			+"PREFIX owl:<http://www.w3.org/2002/07/owl#>" + "\n"
			+"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + "\n"
			+"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>" + "\n"
			+"PREFIX skos:<http://www.w3.org/2004/02/skos/core#>" + "\n"
			+"PREFIX xs:<http://www.w3.org/2001/XMLSchema#>" + "\n"
			+"PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>" + "\n";
	
	public String skosScopeNoteTerm = "scopeNote";
	
	public String selectTerm = "output";
	
	public String regexScope = "\\<(.*?)\\>";
	
	public String AGROVOCURL =  "{ <http://aims.fao.org/aos/agrovoc/c_";
			
	
}
