package org.egov.pg.utils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

public class Utils {

    private static final DecimalFormat CURRENCY_FORMATTER_RUPEE = new DecimalFormat("0.00");
    private static final DecimalFormat CURRENCY_FORMATTER_PAISE = new DecimalFormat("0");

    private Utils() {
    };

    public static String formatAmtAsRupee(String txnAmount) {
        return CURRENCY_FORMATTER_RUPEE.format(Double.valueOf(txnAmount));
    }

    public static String formatAmtAsPaise(String txnAmount) {
        return CURRENCY_FORMATTER_PAISE.format(Double.valueOf(txnAmount) * 100);
    }

    public static String convertPaiseToRupee(String paise){
        return new BigDecimal(paise).movePointLeft(2).toPlainString();
    }
    
    public static String generateSha512Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
 
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
 
            return sb.toString();
 
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating SHA-512 hash", e);
        }
    }
 
    // Existing utility method
    public static String formatAmtAsRupee(Double amount) {
        return String.format("%.2f", amount);
    }

}
