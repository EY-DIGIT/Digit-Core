package org.egov.pg.service.gateways.easebuzz;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EasebuzzResponse {
 
    @JsonProperty("status")
    private String status;
 
    @JsonProperty("txnid")
    private String txnid;
 
    @JsonProperty("easepayid")
    private String easepayid;
 
    @JsonProperty("amount")
    private String amount;
 
    @JsonProperty("mode")
    private String mode;
 
    @JsonProperty("error")
    private String error;
 
    @JsonProperty("field1")
    private String field1;
 
    // Add more fields as needed from actual API response
}
