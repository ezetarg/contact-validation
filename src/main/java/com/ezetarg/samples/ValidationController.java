package com.ezetarg.samples;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Produces;

@Controller("/validate")
public class ValidationController {
    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @Get("/phone/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    public PhoneValidationResult validatePhone(@PathVariable String number) {
        return validationService.validatePhone("AR", number);
    }

    @Get("/phone/{region}/{number}")
    @Produces(MediaType.APPLICATION_JSON)
    public PhoneValidationResult validatePhone(@PathVariable String region, @PathVariable String number) {
        return validationService.validatePhone(region, number);
    }

}
