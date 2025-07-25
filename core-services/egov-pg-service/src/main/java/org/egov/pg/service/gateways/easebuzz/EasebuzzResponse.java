package org.egov.pg.service.gateways.easebuzz;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
 
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EasebuzzResponse {

    @JsonProperty("status")
    private boolean status;

    @JsonProperty("msg")
    private List<EasebuzzTransaction> msg;

    public boolean isStatus() {
        return status;
    }

    public List<EasebuzzTransaction> getMsg() {
        return msg;
    }
}
