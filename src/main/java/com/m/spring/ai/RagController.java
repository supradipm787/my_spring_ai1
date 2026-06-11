package com.m.spring.ai;

import java.net.URI;
import org.springframework.core.io.UrlResource;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
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
	ChatMemory chatMemory;
	
	public RagController(ChatClient.Builder chatClientBuilder, EmbeddingModel embeddingModel, ChatMemory chatMemory) {
		
		//MessageChatMemoryAdvisor keep track of the user messages
		var memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
		
		//chatClient = chatClientBuilder.build();
		
		chatClient = chatClientBuilder.defaultAdvisors(memoryAdvisor).build();
		
		
		vectorStore =  SimpleVectorStore.builder(embeddingModel).build();
		this.chatMemory = chatMemory;
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
	
	@RequestMapping("/askAgain")
	public String askAgain(@RequestParam String question, @RequestParam String cid) {
		var userMessage = generateAugmentedPrompt(question);
		var prompt = chatClient.prompt().user(userMessage);
		// advisor params expects a Map<String,Object>
		var advisedPrompt = prompt.advisors(spec -> spec.params(Map.of(ChatMemory.CONVERSATION_ID, cid)));
		
		return advisedPrompt.call().content();
	}
	
	@RequestMapping("/ask")
	public String ask(@RequestParam String question, @RequestParam String cid) {
		
		chatMemory.add(cid, new UserMessage(question));
		var history = chatMemory.get(cid);
		
		var prompt = generateAugmentedPrompt(question);
		
		
		// attach the user prompt so the ChatClient will populate messages in the request
		var fullPrompt = chatClient.prompt().user(prompt);
		var generatedContent = fullPrompt.call().content();
		
		chatMemory.add(cid, new AssistantMessage(generatedContent));
		
		
		
		return generatedContent;
		
	}

	private String generateAugmentedPrompt(String question) {
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
		return prompt;
	} 

}
