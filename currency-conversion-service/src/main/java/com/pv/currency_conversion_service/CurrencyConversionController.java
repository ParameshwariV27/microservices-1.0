package com.pv.currency_conversion_service;

import java.math.BigDecimal;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;


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
public class CurrencyConversionController {
	
	@Autowired
	private CurrencyExchangeProxy proxy;
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private Environment environment;
	
	@GetMapping("/currency-conversion-rest/from/{from}/to/{to}/quantity/{quantity}")
	@Retry(name="conversion-api", fallbackMethod = "fallBackMethod")
	@CircuitBreaker(name = "conversion-api", fallbackMethod = "fallBackMethod")
	public CurrencyConversion calculateCurrencyConversion(
			@PathVariable String from,
			@PathVariable String to,
			@PathVariable BigDecimal quantity)
	{
		
		HashMap<String, String> uriVariables = new HashMap<>();
		uriVariables.put("from", from);
		uriVariables.put("to", to);
		
		String port = environment.getProperty("server.port");
		System.out.println("###########################################################################################");
		System.out.println("port : "+port);
		System.out.println("###########################################################################################");
		String url = "http://localhost:8000/currency-exchange/from/{from}/to/{to}";
		
		System.out.println("###########################################################################################");
		System.out.println("URL : "+url);
		System.out.println("###########################################################################################");
		ResponseEntity<CurrencyConversion> responseEntity = restTemplate.getForEntity(url, CurrencyConversion.class, uriVariables);
		
		CurrencyConversion currencyConversion = responseEntity.getBody();
		return new CurrencyConversion(currencyConversion.getId(), from, to,quantity,currencyConversion.getConversionMultiple(), 
				quantity.multiply(currencyConversion.getConversionMultiple()),currencyConversion.getEnvironment()+" (rest call)");
	}
	
	@GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
	@Retry(name="conversion-api", fallbackMethod = "fallBackMethod")
	@CircuitBreaker(name = "conversion-api", fallbackMethod = "fallBackMethod")
	@RateLimiter(name = "conversion-api", fallbackMethod = "fallBackMethod")
	@Bulkhead(name = "conversion-api", fallbackMethod = "fallBackMethod")
	public CurrencyConversion calculateCurrencyConversionFeign(
			@PathVariable String from,
			@PathVariable String to,
			@PathVariable BigDecimal quantity)
	{
		
		CurrencyConversion currencyConversion = proxy.retrieveExchangeValue(from, to);
		CurrencyConversion resCurrencyConversion = new CurrencyConversion(currencyConversion.getId(), from, to,quantity,currencyConversion.getConversionMultiple(), 
				quantity.multiply(currencyConversion.getConversionMultiple()),currencyConversion.getEnvironment());
		System.out.println("###########################################################################################");
		System.out.println("Result of currency conversion:  ");
		System.out.println(resCurrencyConversion);
		System.out.println("###########################################################################################");
		return resCurrencyConversion;
	}
	
	public CurrencyConversion fallBackMethod(
	        String from,
	        String to,
	        BigDecimal quantity,
	        Exception ex)
	{
	    return new CurrencyConversion(
	            0L,
	            from,
	            to,
	            quantity,
	            BigDecimal.ZERO,
	            BigDecimal.ZERO,
	            "couldnt get response from service"
	    );
	}
}
