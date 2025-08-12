package org.egov.web.notification.sms.models;

import lombok.*;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;


import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Setter
public class Sms {

    private String mobileNumber;
    private String message;
    private Category category;
    private Long expiryTime;
    
    //add new Field status
    private String templateId;
    
    public boolean isValid() {

        return isNotEmpty(mobileNumber) && isNotEmpty(message);
    }

	public Sms(String mobileNumber, String message, Category category, Long expiryTime) {
		super();
		this.mobileNumber = mobileNumber;
		this.message = message;
		this.category = category;
		this.expiryTime = expiryTime;
	}
}
