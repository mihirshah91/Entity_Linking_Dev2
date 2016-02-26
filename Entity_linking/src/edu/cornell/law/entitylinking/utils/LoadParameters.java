package edu.cornell.law.entitylinking.utils;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;


public class LoadParameters {
	public static HashMap<String,String> params = new HashMap<String, String>(); 
	public static void readParameters(String paramsFilePath) throws Exception{
		FileReader fRead = new FileReader(paramsFilePath);
		@SuppressWarnings("resource")
		BufferedReader bRead = new BufferedReader(fRead);
		String temp = bRead.readLine();
		while(temp!=null){
			String[] splt = temp.split("=");
			params.put(splt[0].trim(), splt[1].trim());
			temp = bRead.readLine();
		}
	}

}
