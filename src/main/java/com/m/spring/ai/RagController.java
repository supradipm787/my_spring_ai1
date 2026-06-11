package com.m.spring.ai;

import java.net.URI;
import org.springframework.core.io.UrlResource;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

@RestController
public class RagController {
	ChatClient chatClient;
	VectorStore vectorStore;
	
	public RagController(ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel) {
		chatClient = chatClientBuilder.build();
		vectorStore =  SimpleVectorStore.builder(embeddingModel).build();
	}
	
	@PostConstruct
	void injestBookonStartup() throws Exception {
		// Ingest the book into the vector store on startup
		var pdfUrl = "https://certificationexams.pro/docs/pickeringisspringfield.pdf";
		var resource = new UrlResource(URI.create(pdfUrl));
		var pages = new PagePdfDocumentReader(resource).get();
		vectorStore.add(pages);
		
		
		
		IO.println("Ingested book into vector store" + pdfUrl );
	}
	
	@RequestMapping("/ask")
	public String ask(@RequestParam String question) {
		var retrievalQuery = SearchRequest.builder().query(question).topK(5).build();
		var retrievedPages = vectorStore.similaritySearch(retrievalQuery);
		int sizeRetrievedPages = retrievedPages.size();
		
		var augmentedContext = new StringBuilder();
		
		for (int i=0; i<sizeRetrievedPages; i++) {
			var page = retrievedPages.get(i);
			
			augmentedContext.append("Pages from the book").append(page.getText());
		}
		
		IO.println(augmentedContext);
		
		var prompt = """
			You are answering questions using only the context provided from the popular pickering is Springfield book
			If the answer is not in the context say : "Sorry I don't know given the pages I have read"
			Provide me
			
			 CONTEXT: 
			 %s
			 
			 QUESTION: 
			 %s
			""".formatted(augmentedContext.toString(), question);

		//Earlier Code
		//var generatedContent = chatClient.prompt(prompt).call().content();
		
		
		// attach the user prompt so the ChatClient will populate messages in the request
		var fullPrompt = chatClient.prompt().user(prompt);
		var generatedContent = fullPrompt.call().content();
		
		
		
		return generatedContent;
		
	} 

}
