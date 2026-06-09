package com.m.spring.ai;

import java.util.Map;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
//import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple controller for demo purposes.
 */
@RestController
public class ShopController {
	
	static final String systemMessage = """
			You are a tweet rewrite engine that does not use emdashes & prefer to use sentences rather than use semicolon
			
			Goal:
			Given one original tweet produce 5 rewitten tweets each in different voice: 
			1) TECH BRO
			2) INSPIRATIONAL SPEAKER
			3) CASUAL
			4) SARCASTIC
			5) MONK
			
			Modernization:
			Refresh dated concepts to the degree specified by the MODERNIZATION_LEVEL
			
			Hard rules:
			:Outputs must contain exactly 5 tweets one per voice, & nothing else
			
			Output format(strict):
			Give a line break after each tweet & tweet must start with a bullet point
			TECH BRO: tweet <br> & give a line break after each tweet & tweet must start with an emogi
			INSPIRATIONAL SPEAKER: tweet <br> & give a line break after each tweet & tweet must start with an emogi
			CASUAL: tweet <br> & give a line break after each tweet & tweet must start with an emogi
			SARCASTIC: tweet <br> & give a line break after each tweet & tweet must start with an emogi
			MONK: tweet <br> & give a line break after each tweet & tweet must start with an emogi
			
			""";
	
	enum Voice { TECH_BRO, INSPIRATIONAL_SPEAKER, CASUAL, SARCASTIC, MONK}
    record TweetVariant(String tweet, Voice voice) {}
    record NewTweets(List<TweetVariant> tweets) {}
	
	ChatClient chatClient;
    // ChatClient.Builder.defaultOptions expects a ChatOptions.Builder, not a built ChatOptions instance
    static final ChatOptions.Builder optionsBuilder = ChatOptions.builder().model("gpt-5.2").temperature(0.99).topP(.95);
	
    //static final org.springframework.ai.chat.prompt.ChatOptions options = ChatOptions.builder().model("gpt-5.2").temperature(0.99).topP(.95).build();
	
    public ShopController(ChatClient.Builder builder) {
		super();
        //this.chatClient = builder.build();
        // pass the builder (not a built ChatOptions) to defaultOptions
        
		//this.chatClient = builder.defaultOptions(optionsBuilder).build();
		this.chatClient = builder.defaultSystem(systemMessage).defaultOptions(optionsBuilder).build();
		
	}
	

    @GetMapping("/tweets")
    public NewTweets tweets(
            @RequestParam(name = "aI", defaultValue = "How to run AI program fast") String aI,
            @RequestParam(name = "springAI", defaultValue = "Spring AI") String springAI,
            @RequestParam(name = "priority", defaultValue = "high") String priority,
            @RequestParam(name = "level", defaultValue = "extreme") String modernizationLevel) {
        
    	
    	// intentionally empty: endpoint returns HTTP 200 OK with no body
         	//return ResponseEntity.ok().build();
    	
    	
    	
		
    		//var fullPrompt = chatClient.prompt().system(systemMessage).user(prompt);
    		//String resp = fullPrompt.call().content();     
    		String userTemplate = """
    				
    				MODERNIZATION_LEVEL: {modernizationLevel}
    				
    				OPTIONAL_TWEET: {postText} 
    				
    				TOPIC_HINT: {topicHint}
    				    				    				
    				PRIORITY: {priority}
    				""";
    	PromptTemplate promptTemplate = new PromptTemplate(userTemplate);
    	var userPrompt = promptTemplate.render(
    			Map.of("postText", aI, 
    					"topicHint", springAI, 
    					"modernizationLevel", modernizationLevel,
    					"priority", priority));
    	
    	IO.println(userPrompt);
    	
    	BeanOutputConverter <NewTweets> converter = new BeanOutputConverter <>(NewTweets.class);
    	IO.println(converter.getFormat());
    	
    	//return chatClient.prompt().system(systemMessage).user(userPrompt).call().content();
    	return chatClient.prompt().user(userPrompt).call().entity(converter);
		
    	//return "OK";
    }
    
    
    
}
