package com.pv.currency_exchange_service;
import com.pv.currency_exchange_service.CurrencyExchange;

import java.math.BigDecimal;
import java.util.Map;

import org.slf4j.Logger;


import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;


@Configuration(proxyBeanMethods = false)
class RestTemplateConfiguration
{
	@Bean
	RestTemplate restTemplate(RestTemplateBuilder builder)
	{
		return builder.build();
	}
}

@RestController
public class CurrencyExchangeController {
	
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private CurrencyExchangeRepository repository;
	@Autowired
	private Environment environment;
	
	@Autowired
	private CurrencyRatesService cs;
	
	@GetMapping("/currency-exchange/from/{from}/to/{to}")
	@Retry(name = "sample-api", fallbackMethod = "hardcodedResponse")
	@CircuitBreaker(name = "sample-api", fallbackMethod = "hardcodedResponse")
	@RateLimiter(name = "sample-api" , fallbackMethod = "hardcodedResponse")
	@Bulkhead(name = "sample-api", fallbackMethod = "hardcodedResponse")
	public CurrencyExchange retrieveExchangeValue( @PathVariable String from, @PathVariable String to)

	{
		logger.info("retriveExchangeValue called with {} to {}", from,to);
		//CurrencyExchange currencyExchange = new CurrencyExchange(100L, from, to,BigDecimal.valueOf(50));
		CurrencyExchange currencyExchange = repository.findByFromAndTo(from, to);
		if(currencyExchange==null)
		{
			throw new RuntimeException("Unable to find");
		}
		String port =environment.getProperty("local.server.port");
		currencyExchange.setEnvironment(port);
		System.out.println("###########################################################################################");
		System.out.println("From currency Exchaneg::  ");
		System.out.println(currencyExchange);
		System.out.println("###########################################################################################");
		return currencyExchange;
	}
	
	public CurrencyExchange hardcodedResponse(String from, String to, Exception ex) {
		//return "inside hardcoded response method";
	    CurrencyExchange fallbackobj =  new CurrencyExchange(
	            0L,
	            from,
	            to,
	            BigDecimal.ZERO
	    );
	    fallbackobj.setEnvironment("couldnt get response from service");
	    
	    return fallbackobj;
	}
	
	@GetMapping("/currency-exchange/rates")
	public Map<String, Double> getRates()
	{
		
		return  cs.fetchAndStoreRates();
	 
		
	}


}
