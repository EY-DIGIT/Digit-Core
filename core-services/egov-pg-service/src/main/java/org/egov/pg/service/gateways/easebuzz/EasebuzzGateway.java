package org.egov.pg.service.gateways.easebuzz;

import lombok.extern.slf4j.Slf4j;
import org.egov.pg.models.Transaction;
import org.egov.pg.service.Gateway;
import org.egov.pg.utils.Utils;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
public class EasebuzzGateway implements Gateway {

 
    private static final String GATEWAY_NAME = "EASEBUZZ";
 
    private final String MERCHANT_KEY;
    private final String SALT;
    private final String MERCHANT_URL_DEBIT;
    private final String MERCHANT_URL_STATUS;
    private final String MERCHANT_URL_PAY;
    private final boolean ACTIVE;
 
    private final RestTemplate restTemplate;
 
    @Autowired
    public EasebuzzGateway(RestTemplate restTemplate, Environment environment) {
        this.restTemplate = restTemplate;
 
        ACTIVE = Boolean.parseBoolean(environment.getRequiredProperty("easebuzz.active"));
        MERCHANT_KEY = environment.getRequiredProperty("easebuzz.merchant.key");
        SALT = environment.getRequiredProperty("easebuzz.merchant.salt");
        MERCHANT_URL_DEBIT = environment.getRequiredProperty("easebuzz.url.debit");
        MERCHANT_URL_STATUS = environment.getRequiredProperty("easebuzz.url.status");
        MERCHANT_URL_PAY = environment.getRequiredProperty("easebuzz.test.url.pay");
     
    }
 
    @SuppressWarnings("null")
	@Override
    public URI generateRedirectURI(Transaction transaction) {
        try {
           
            // Optional: Generate hash using merchant_key + txnid + amount + productinfo + firstname + email + salt
            // Note: Actual implementation would use secure hash method (e.g., SHA512) as per Easebuzz documentation
            String hashString = MERCHANT_KEY + "|" + transaction.getTxnId() + "|" + Utils.formatAmtAsRupee(transaction.getTxnAmount())
                    + "|"+transaction.getProductInfo()+"|" + transaction.getUser().getName() + "|" + transaction.getUser().getEmailId() + "|||||||||||" + SALT;
            //System.out.println(hashString);
            String hash = Utils.generateSha512Hash(hashString);
 
            String requestBody = "key="+MERCHANT_KEY
                    + "&txnid="+transaction.getTxnId()
                    + "&amount="+Utils.formatAmtAsRupee(transaction.getTxnAmount())
                    + "&productinfo="+transaction.getProductInfo()
                    + "&firstname="+transaction.getUser().getName()
                    + "&email="+transaction.getUser().getEmailId()
                    + "&phone="+transaction.getUser().getMobileNumber()
                    + "&surl="+transaction.getCallbackUrl()
                    + "&furl="+transaction.getCallbackUrl()
                    + "&hash="+hash;
            
            HttpRequest request = HttpRequest.newBuilder()
            	    .uri(URI.create(MERCHANT_URL_DEBIT))
            	    .header("Content-Type", "application/x-www-form-urlencoded")
            	    .header("Accept", "application/json")
            	    .method("POST", HttpRequest.BodyPublishers.ofString(requestBody))
            	    .build();
            	HttpResponse<String> response = null;
				try {
					log.info("Sending request to "+MERCHANT_URL_DEBIT);
					log.info("Query : "+requestBody);
					response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
					log.info("Received response "+MERCHANT_URL_DEBIT);
				} catch (IOException | InterruptedException e) {
					log.error("Error occurred while sending request to Easebuzz: {}", e.getMessage(), e);
				}
				

				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(response.body());
				String dataValue =null;
				if (rootNode.get("status").asInt() == 1) {
					 dataValue = rootNode.get("data").asText();
				}else {
					String errorMessage = rootNode.has("error_desc") ? rootNode.get("error_desc").asText()+rootNode.get("data").asText(): "Unexpected error from payment gateway.";
				    throw new RuntimeException("Payment initiation failed. Reason: " + errorMessage);
				}
				UriComponents uriComponents = null;
			if(dataValue != null || dataValue.isBlank()) {
             uriComponents = UriComponentsBuilder
                    .fromHttpUrl(MERCHANT_URL_PAY+dataValue)
                    .build()
                    .encode();
			}else {
				log.error("Error occurred while Received response "+MERCHANT_URL_DEBIT);
				throw new IllegalArgumentException("data cannot be null or empty");
			}
            return uriComponents.toUri();
			
        } catch (Exception e) {
            log.error("Easebuzz hash generation failed", e);
            throw new CustomException("HASH_GEN_FAILED", "Hash generation failed, gateway redirect URI cannot be generated");
        }
    }
 
    @Override
    public Transaction fetchStatus(Transaction currentStatus, Map<String, String> params) {
        try {
                 
			String hashString = MERCHANT_KEY + "|" + currentStatus.getTxnId() + "|" + SALT;
			// System.out.println(hashString);
			String hash = Utils.generateSha512Hash(hashString);
			String body = "key=" + URLEncoder.encode(MERCHANT_KEY, "UTF-8")
            + "&txnid=" + URLEncoder.encode(currentStatus.getTxnId(), "UTF-8")
            + "&hash=" + URLEncoder.encode(hash, "UTF-8");

			HttpRequest request = HttpRequest.newBuilder()
				    .uri(URI.create(MERCHANT_URL_STATUS))
				    .header("Content-Type", "application/x-www-form-urlencoded")
				    .header("Accept", "application/json")
				    .method("POST", HttpRequest.BodyPublishers.ofString(body))
				    .build();
			HttpResponse<String> response = null;
			try {
				log.info("Sending request to "+MERCHANT_URL_STATUS);
				log.info("Query : "+body);
				response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
				log.info("Received response "+MERCHANT_URL_STATUS);
			} catch (IOException | InterruptedException e) {
				log.error("Error occurred while sending request to Easebuzz: {}", e.getMessage(), e);
			}
			if(response != null)
				return transformRawResponse(response.body(), currentStatus);
			else {
				log.error("Unable to fetch response from Easebuzz");
				throw new CustomException("STATUS_FETCH_FAILED", "Failed to fetch transection status from Easebuzz");
			}
				
        } catch (Exception e) {
            log.error("Unable to fetch status from Easebuzz", e);
            throw new CustomException("STATUS_FETCH_FAILED", "Failed to fetch payment status from Easebuzz");
        }
    }
 
    private Transaction transformRawResponse(String body, Transaction currentStatus) {
    	
    	ObjectMapper mapper = new ObjectMapper();
    	EasebuzzResponse easebuzzResponse = null;
		try {
			easebuzzResponse = mapper.readValue(body, EasebuzzResponse.class);
		} catch (JsonProcessingException e) {
			log.error("Error while parsing Easebuzz API response: {}", body, e);
		}
    	
    	 Transaction.TxnStatusEnum status = Transaction.TxnStatusEnum.PENDING;
    	 EasebuzzTransaction transaction = null;
    	 try {
    		    if (easebuzzResponse == null) {
    		        throw new IllegalStateException("Easebuzz response is null.");
    		    }

    		    if (!easebuzzResponse.isStatus()) {
    		        throw new IllegalStateException("Easebuzz response indicates failure. Status=false.");
    		    }

    		    if (easebuzzResponse.getMsg() == null || easebuzzResponse.getMsg().isEmpty()) {
    		        throw new IllegalStateException("Easebuzz response 'msg' field is null or empty.");
    		    }

    		    transaction = easebuzzResponse.getMsg().get(0);
    		} catch (Exception e) {
    		   log.error("Error while extracting transaction from Easebuzz response: " + e.getMessage());
    		}
    	 
         if (easebuzzResponse.isStatus() && easebuzzResponse != null)
             status = Transaction.TxnStatusEnum.SUCCESS;
         else 
             status = Transaction.TxnStatusEnum.FAILURE;
  
         return Transaction.builder()
                 .txnId(currentStatus.getTxnId())
                 .txnAmount(Utils.formatAmtAsRupee(transaction.getAmount()))
                 .txnStatus(status)
                 .gatewayTxnId(transaction.getEasepayid())
                 .gatewayPaymentMode(transaction.getCardType())
                 .gatewayStatusCode(transaction.getStatus())
                 .gatewayStatusMsg(transaction.getError())
                 .responseJson(transaction)
                 .build();

	}

 
    @Override
    public boolean isActive() {
        return ACTIVE;
    }
 
    @Override
    public String gatewayName() {
        return GATEWAY_NAME;
    }
 
    @Override
    public String transactionIdKeyInResponse() {
        return "txnid";
    }
}
