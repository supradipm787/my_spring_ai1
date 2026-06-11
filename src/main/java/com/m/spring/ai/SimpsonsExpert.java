package com.m.spring.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class SimpsonsExpert {
	
	ChatClient chatClient;
	public SimpsonsExpert(ChatClient.Builder builder) {
		chatClient = builder.build();
		
	}
	
	@GetMapping("/trivia")
	public String trivia(@RequestParam String prompt) {
		var systemMessage = """
				You are a Simpsons expert. You know everything about the Simpsons TV show 
				You can answer any question about the Simpsons TV show."
				Rules:
				- Only answer questions related to trivia
				- Only provide Simpsons related to trivia
				 """;
		var fullPrompt = chatClient.prompt().system(systemMessage).user(prompt);
		
		
		String resp = fullPrompt.call().content();
		return resp;
		
	}
	
	 @GetMapping("/simpson")
	    public ModelAndView home() {
	        //return "redirect:/simpson.html";
		 return new ModelAndView("redirect:/simpson.html");
	    }

}
