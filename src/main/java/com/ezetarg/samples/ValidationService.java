package com.ezetarg.samples;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import javax.inject.Singleton;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class ValidationService {
    private static final Logger logger = Logger.getLogger(ValidationService.class.getName());
    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    public PhoneValidationResult validatePhone(String regionDialingFrom, String number) {
        boolean valid = phoneUtil.isPossibleNumber(number, regionDialingFrom);

        PhoneValidationResult result = new PhoneValidationResult();
        result.setValid(valid);
        result.setNumber(number);

        if (valid) {
            try {
                Phonenumber.PhoneNumber phoneNumber = phoneUtil.parse(number, regionDialingFrom);
                result.setFormattedNumber(phoneUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
            } catch (NumberParseException e) {
                result.setValid(false);
                logger.log(Level.INFO, "Error parsing phone number", e);
            }
        }

        return result;
    }
}
