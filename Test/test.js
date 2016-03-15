$( document ).ready(function() {
	$.ajax({url:"section_4.56.xml",context: document.body, success: function(response){
        	$("#main-div").html(response);
		setEventHandlersOnTag();
	}});
	
});
function showDefinition(e){
	console.log("Entity clicked");
}
function getMeshXmlDocument(){
	var Connect = new XMLHttpRequest();
  	Connect.open("GET", "desc2015.xml", false);
  	Connect.setRequestHeader("Content-Type", "text/xml");
  	Connect.send(null);
	var document = Connect.responseXML;
	return document;
}

function onEntityClick(e){
	console.log("Entity clicked");
	var database = e.target.attributes[2].value;
	var entityTag = e.target.attributes[0];
	var databaseIdTag = entityTag.value;
	var databaseId = databaseIdTag.split(":");

	if(database == "Mesh"){
		var link = "http://id.nlm.nih.gov/mesh/" + databaseId[1];
	}
	else if(database == "Drugbank"){
		var link = "http://www.drugbank.ca/drugs/" + databaseId[1];
	}	
	else if(database == "Agrovoc"){
		var link = "http://www.fao.org/aos/agrovoc#" + databaseId[1];	
	}
	else if(database == "Dbpedia"){
		var link = "http://www.fao.org/aos/agrovoc#" + databaseId[1];	
	}
	window.open(link);
}
function setEventHandlersOnTag() {
	$("linkedEntity").on("click", onEntityClick);
}
