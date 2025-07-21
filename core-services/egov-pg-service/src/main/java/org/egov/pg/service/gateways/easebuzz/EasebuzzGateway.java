package org.egov.pg.service.gateways.easebuzz;

import lombok.extern.slf4j.Slf4j;
import org.egov.pg.models.Transaction;
import org.egov.pg.service.Gateway;
import org.egov.pg.utils.Utils;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
 
import java.net.URI;
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
    }
 
    @Override
    public URI generateRedirectURI(Transaction transaction) {
        try {
            MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
 
//            paramMap.add("key", MERCHANT_KEY);
//            paramMap.add("txnid", transaction.getTxnId());
//            paramMap.add("amount", Utils.formatAmtAsRupee(transaction.getTxnAmount()));
//            paramMap.add("productinfo", "Service Fee");
//            paramMap.add("firstname", transaction.getUser().getName());
//            paramMap.add("email", transaction.getUser().getEmailId());
//            paramMap.add("phone", transaction.getUser().getMobileNumber());
//            paramMap.add("surl", transaction.getCallbackUrl()); // success URL
//            paramMap.add("furl", transaction.getCallbackUrl()); // failure URL
 
            // Optional: Generate hash using merchant_key + txnid + amount + productinfo + firstname + email + salt
            // Note: Actual implementation would use secure hash method (e.g., SHA512) as per Easebuzz documentation
 
            String hashString = MERCHANT_KEY + "|" + transaction.getTxnId() + "|" + Utils.formatAmtAsRupee(transaction.getTxnAmount())
                    + "|Service Fee|" + transaction.getUser().getName() + "|" + transaction.getUser().getEmailId() + "|||||||||||" + SALT;
 
            String hash = Utils.generateSha512Hash(hashString);
            paramMap.add("hash", hash);
 
            UriComponents uriComponents = UriComponentsBuilder
                    .fromHttpUrl(MERCHANT_URL_DEBIT)
                    .queryParams(paramMap)
                    .build()
                    .encode();
 
            return uriComponents.toUri();
        } catch (Exception e) {
            log.error("Easebuzz hash generation failed", e);
            throw new CustomException("HASH_GEN_FAILED", "Hash generation failed, gateway redirect URI cannot be generated");
        }
    }
 
    @Override
    public Transaction fetchStatus(Transaction currentStatus, Map<String, String> params) {
        try {
            MultiValueMap<String, String> statusRequest = new LinkedMultiValueMap<>();
            statusRequest.add("txnid", currentStatus.getTxnId());
            statusRequest.add("key", MERCHANT_KEY);
 
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
 
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(statusRequest, headers);
 
            ResponseEntity<EasebuzzResponse> response = restTemplate.postForEntity(
                    MERCHANT_URL_STATUS, request, EasebuzzResponse.class);
 
            return transformRawResponse(response.getBody(), currentStatus);
        } catch (Exception e) {
            log.error("Unable to fetch status from Easebuzz", e);
            throw new CustomException("STATUS_FETCH_FAILED", "Failed to fetch payment status from Easebuzz");
        }
    }
 
    private Transaction transformRawResponse(EasebuzzResponse resp, Transaction currentStatus) {
        Transaction.TxnStatusEnum status = Transaction.TxnStatusEnum.PENDING;
 
        if ("success".equalsIgnoreCase(resp.getStatus()))
            status = Transaction.TxnStatusEnum.SUCCESS;
        else if ("failure".equalsIgnoreCase(resp.getStatus()))
            status = Transaction.TxnStatusEnum.FAILURE;
 
        return Transaction.builder()
                .txnId(currentStatus.getTxnId())
                .txnAmount(Utils.formatAmtAsRupee(resp.getAmount()))
                .txnStatus(status)
                .gatewayTxnId(resp.getEasepayid())
                .gatewayPaymentMode(resp.getMode())
                .gatewayStatusCode(resp.getStatus())
                .gatewayStatusMsg(resp.getError())
                .responseJson(resp)
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
