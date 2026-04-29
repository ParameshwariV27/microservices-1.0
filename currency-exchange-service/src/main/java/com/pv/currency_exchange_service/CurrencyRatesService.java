package com.pv.currency_exchange_service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CurrencyRatesService {
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	CurrencyExchangeRepository repo;
	
	@Autowired
	Environment env;
	
	static Long counter = 10000L;
	
	@Transactional
	public Map<String, Double> fetchAndStoreRates()
	{
		
		String acctId = env.getProperty("acctId");
		System.out.println("#########################################################################################################");
		System.out.println("Printing acctId: "+acctId);
		System.out.println("#########################################################################################################");
		
		String locAcctId = env.getProperty("local.acctId");
		System.out.println("#########################################################################################################");
		System.out.println("Printing acctId: "+locAcctId);
		System.out.println("#########################################################################################################");
		
		String url = "https://openexchangerates.org/api/latest.json?app_id="+acctId;
	    
	    Map<String, Object> response = restTemplate.getForObject(url, Map.class);
	    Map<String, Object> ratesMap = (Map<String, Object>) response.get("rates");
	  
	    Map<String, Double> rates = new HashMap<>();
	     double usdToInr = ((Number) ratesMap.get("INR")).doubleValue();
//	     System.out.println("#########################################################################################################");
//	     System.out.println("USD TO INR VALUE: "+usdToInr);
//	     System.out.println("#########################################################################################################");
	    for (Map.Entry<String, Object> entry : ratesMap.entrySet()) {
	    	
	    	String currencyValue = entry.getKey();
//		     System.out.println("#########################################################################################################");
//		     System.out.println("currencyValue : "+currencyValue);
//		     System.out.println("#########################################################################################################");
	    	double rateRelativeToUsd = ((Number) entry.getValue()).doubleValue();
		     
	        rates.put(currencyValue, rateRelativeToUsd);
		     
	        CurrencyExchange currencyExchange = convertvalue( usdToInr, currencyValue, rateRelativeToUsd);
//		     System.out.println("#########################################################################################################");
//		     System.out.println("Cuurency Exchnage object "+currencyExchange);
//		     System.out.println("#########################################################################################################");
	        repo.save(currencyExchange);
	    }
	    System.out.println(rates);
	    return rates;
		
	}
	
	public CurrencyExchange convertvalue(Double usdToInr, String currency, double value)
	{
		counter++;
		if (currency.equals("INR")) 
		{
			return new CurrencyExchange(counter, currency,"INR",BigDecimal.valueOf(usdToInr));
		}

        BigDecimal inrValue = BigDecimal.valueOf(usdToInr)
                .divide(BigDecimal.valueOf(value), 6, RoundingMode.HALF_UP);
		return new CurrencyExchange(counter, currency,"INR",inrValue);
		
	}

}
