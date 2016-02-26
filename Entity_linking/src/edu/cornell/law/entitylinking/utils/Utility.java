package edu.cornell.law.entitylinking.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class Utility {

		private static MaxentTagger tagger;
		private static StanfordCoreNLP pipeline;		
		private static Properties props = new Properties();
		
		public static void runCoreNLP() {
		    tagger = new MaxentTagger("edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse"); 
			pipeline = new StanfordCoreNLP(props);
		}

		/**
		 * Function that adds POS Tags to a given string
		 * @param paragraph
		 * @return
		 */
		public static String getPOSTagging(String paragraph) {
		  //System.out.println(tagger.tagString(paragraph));
			return tagger.tagString(paragraph);
		}
		
		public static List<String> findPOSTags(String paragraph) {
			List<String> potentialEntities = new ArrayList<String>(); 
			String POSTaggedPara = Utility.getPOSTagging(paragraph);
			String [] taggedWords = POSTaggedPara.trim().split(" ");	
			for (int i = 0; i < taggedWords.length; i++) {
				if(taggedWords[i].contains("NN") || taggedWords[i].contains("NNP") || taggedWords[i].contains("FW")) {
					potentialEntities.add(taggedWords[i].split("_")[0]);
				}
			}
			return potentialEntities;
		}
		
		
		
		public static List<String> getInnerNounPhrases(String paragraph)
		{
			List<String> nounPhrases = new ArrayList<String>(); 
			try{
				StringTokenizer tokenizer = new StringTokenizer(paragraph, "\\.;?,:");
				while(tokenizer.hasMoreTokens()){	
					Annotation document = new Annotation(tokenizer.nextToken());
			        pipeline.annotate(document);
			        Tree tree = null;
			        // these are all the sentences in this document
			        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
			        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			        
			        for(CoreMap sentence: sentences) {
			         	// the parse tree of the current sentence
			            tree = sentence.get(TreeAnnotation.class);
			           
			            List<Tree> phraseList=new ArrayList<Tree>();
			            for (Tree subtree: tree)
			            {
			              if((subtree.label().value().equals("NP"))||(subtree.label().value().equals("WHNP")))
			              {
			                phraseList.add(subtree);
			              }
			            }    
			            
			           if(!phraseList.isEmpty())
			           {
			          	 String skipPhrase = "false";
			          	 for (Tree subList: phraseList)
			          	 { 
			          		 StringBuilder phraseString = new StringBuilder();
			          		 String phrase = subList.toString();
			          		 String[] tokens = phrase.split(" ");
			          		 for( String token : tokens)
			          		 {
			          			 if(token.contains("("))
			          			 {
			          				 if(token.contains("(NP")) 
			  	        			 {
			  	        				 // Check if there are more NP or WHNP in it?
			  	        				 String subPhrase = phrase.replaceFirst("\\(NP", "");
			  	        				 if((subPhrase.contains("(NP"))||(subPhrase.contains("(WHNP")))
			  	        				 {
			  	        					skipPhrase = "true";
			  	        					break; 
			  	        				 }
			  	        			 }
			  	        			 else if(token.contains("(WHNP")) 
			  	        			 {
			  	        				 // Check if there are more NP or WHNP in it?
			  	        				 String subPhrase = phrase.replaceFirst("\\(WHNP", "");
			  	        				 if((subPhrase.contains("(NP"))||(subPhrase.contains("(WHNP")))
			  	        				 {
			  	        					skipPhrase = "true";
			  	        					break; 
			  	        				 }
			  	        			 }
			  	        			 else
			  	        			 {
			  	        				 // do nothing, just drop the keyword.
			  	        			 }
			          			 }
			          			 else
			          			 {
			          				 token = token.replace(")", "");     				 
			          				 phraseString.append(token + " ");
			          				 skipPhrase = "false";             		       		 
			          			 }
			          				 
			          		 }
			          		 if(!skipPhrase.equals("true"))
			          		 {
			          			 String temp = phraseString.toString().trim();
			          			 if(temp.startsWith("(?i)the")) 
			          				temp = temp.replaceFirst("(?i)the ", "");
			          			 
			          			 else if (temp.startsWith("(?i)a")) 
			          				temp = temp.replaceFirst("(?i)a ", "");
			          
			          			 else if (temp.startsWith("(?i)an"))
			          				temp = temp.replaceFirst("(?i)an ", "");
			          			 
			          			 if(temp.contains(" or "))
			          			 {
			          				 String[] nptokens = temp.split(" or ");
			          				 for(String s: nptokens)
			          				 {
			          					 nounPhrases.add(s);
			          				 }
			          			 }
			          			 else
			          			 {
			          				 nounPhrases.add(temp);  
			          			 }
			          		 }
			          	 } 	 
			           }
		          }
				}
			}
			catch (OutOfMemoryError e) {
				System.out.println("Result too long to read into memory");
			}
			return nounPhrases;
	  	}
		
		public static List<String> getAllNounPhrases(String paragraph)
		{
			List<String> nounPhrases = new ArrayList<String>(); 
			try{
				StringTokenizer tokenizer = new StringTokenizer(paragraph, "\\.;?:,");
				while(tokenizer.hasMoreTokens()){	        
				        Annotation document = new Annotation(tokenizer.nextToken());
				        pipeline.annotate(document);
				        Tree tree = null;
				        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		        
				        for(CoreMap sentence: sentences) {
				          // this is the parse tree of the current sentence
				          tree = sentence.get(TreeAnnotation.class);
				         
				          for (Tree subtree: tree)
				          {
				        	if((subtree.label().value().equals("NP"))||(subtree.label().value().equals("WHNP")))
				            {
				        		String phraseString = Sentence.listToString(subtree.yieldWords()).replace(" -LRB- ", "(").replace(" -RRB- ", ")");
				        		
				        		String temp = phraseString.trim();
			         			 if(temp.startsWith("(?i)the")) 
			         				temp = temp.replaceFirst("(?i)the ", "");
			         			 
			         			 else if (temp.startsWith("(?i)a")) 
			         				temp = temp.replaceFirst("(?i)a ", "");
			         
			         			 else if (temp.startsWith("(?i)an"))
			         				temp = temp.replaceFirst("(?i)an ", "");
			         			 
			         			 if (subtree.getChildrenAsList().contains(tree.label().value().equals("NN"))) {
			         				 //System.out.println("PHRASE");
			         			 }
			         			 
			         			 if(temp.contains(" or "))
			         			 {
			         				 String[] nptokens = temp.split(" or ");
			         				 for(String s: nptokens)
			         				 {
			         					 nounPhrases.add(s);
			         				 }
			         			 }
			         			 else
			         			 {
			         				 nounPhrases.add(temp);  
			         			 }
				            }
				          }    
				        }
				}
			}
			catch (OutOfMemoryError e) {
				System.out.println("Result too long to read into memory");
			}
	        
	       return nounPhrases;
		}
		
		/**
		 * 
		 * @param args
		 */
		/*public static void main(String args[]) {
			System.out.println("**************************");
			String test = "(b)";
			System.out.println(test.substring(test.indexOf("#")+1,test.length()));*/
					//"sodium lauryl sulfate"
					//"3-[3-(2,3-dihydroxy-propylamino)-phenyl]-4-(5-fluoro-1-methyl-1h-indol-3-yl)-pyrrole-2,5-dione";
					//"2-(Phosphonooxy)Butanoic Acid"; 
					//"intermediate handling facility hand.";
					//"United States Department of Agriculture for injection into";
					//"<i>Approved brucella vaccine.</i> A product approved by and produced under license of the United States Department of Agriculture for injection into cattle or bison to enhance their resistance to brucellosis.";
					//"brucellosis negative classification";
					//"An animal subjected to one or more official tests resulting in a brucellosis negative classification or reclassified as brucellosis negative by a designated epidemiologist as provided for in the definition of official test.";
					//"An animal subjected to an official test resulting in a brucellosis reactor or subjected to a bacteriological examination.";
					//"That portion of any State which has a separate brucellosis classification under this part.";
					//"The brucellosis reactor is located in a herd in a different State than the State where the MCI blood sample was collected.";
					//"This is a test paragraph. Cette paragraphe une teste."; //"2-(Phosphonooxy)Butanoic Acid"; 
			/*System.out.println(test);
			System.out.println("check regex.."); 
			String tagMe= "hand";
			if (test.matches(".*[\\s.,:;]+"+tagMe+"[\\s.,:;]+.*")) {
				test = test.replace(tagMe , " <linkedEntity src=\"mesh\" identifier=\"meshEntity:" 
						+ "fejhf" + "\" "
					    + "occur=\"1\">"
					    + tagMe + "</linkedEntity> ");
				System.out.println(test);
			}
			if (test.contains(tagMe))
				System.out.println(test);*/
			/*String posTaggedTest = Utility.getPOSTagging(test);
			System.out.println("POS TAGS:   " + posTaggedTest);	*/
			
			/*List<String> posTagged = Utility.findPOSTags(test);
			
			System.out.println("*****************************");
			for (String s :posTagged) {
	        	System.out.println(s);
	        }
			*/
			/*List<String> nounPhrases = getNounPhrases(test);
			System.out.println("*****************************");
			for (String s :nounPhrases) {
	        	System.out.println("Final NPs: "+s);
	        }
			nounPhrases = getNounPhrases1(test);
			System.out.println("*****************************");
	        for (String s :nounPhrases) {
	        	System.out.println("Final NPs: "+s);
	        }*/
		/*}*/

}
