package org.egov.pg.service.gateways.easebuzz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class EasebuzzTransaction {

	 private String txnid;
	    private String firstname;
	    private String email;
	    private String phone;
	    private String key;
	    private String mode;
	    private String unmappedstatus;
	    private String cardCategory;
	    private String addedon;
	    
	    @JsonProperty("payment_source")
	    private String paymentSource;

	    @JsonProperty("PG_TYPE")
	    private String pgType;

	    @JsonProperty("bank_ref_num")
	    private String bankRefNum;

	    private String bankcode;
	    private String error;

	    @JsonProperty("error_Message")
	    private String errorMessage;

	    @JsonProperty("name_on_card")
	    private String nameOnCard;

	    @JsonProperty("upi_va")
	    private String upiVa;

	    private String cardnum;

	    @JsonProperty("issuing_bank")
	    private String issuingBank;

	    private String easepayid;
	    private String amount;

	    @JsonProperty("net_amount_debit")
	    private String netAmountDebit;

	    @JsonProperty("cash_back_percentage")
	    private String cashBackPercentage;

	    @JsonProperty("deduction_percentage")
	    private String deductionPercentage;

	    @JsonProperty("merchant_logo")
	    private String merchantLogo;

	    private String surl;
	    private String furl;
	    private String productinfo;

	    private String udf1;
	    private String udf2;
	    private String udf3;
	    private String udf4;
	    private String udf5;
	    private String udf6;
	    private String udf7;
	    private String udf8;
	    private String udf9;
	    private String udf10;

	    @JsonProperty("card_type")
	    private String cardType;

	    private String hash;
	    private String status;

	    @JsonProperty("bank_name")
	    private String bankName;

	    @JsonProperty("auth_code")
	    private String authCode;

	    @JsonProperty("auth_ref_num")
	    private String authRefNum;

	    @JsonProperty("response_code")
	    private String responseCode;

	    @JsonProperty("error_code")
	    private String errorCode;
	
}
