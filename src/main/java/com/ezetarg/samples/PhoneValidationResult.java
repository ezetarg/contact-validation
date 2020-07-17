package com.ezetarg.samples;

import lombok.Data;

@Data
public class PhoneValidationResult {
    private boolean valid;
    private String number;
    private String formattedNumber;
}
