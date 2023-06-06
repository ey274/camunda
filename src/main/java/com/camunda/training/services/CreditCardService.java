package com.camunda.training.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CreditCardService {

  private static final Logger LOG = LoggerFactory.getLogger(CreditCardService.class);

  public void chargeAmount(String cardNumber, String cvc, String expiryDate, Double amount) {
    LOG.info("charging card {} that expires on {} and has cvc {} with amount of {}",
            cardNumber, expiryDate, cvc, amount);
    LOG.info("payment completed");
  }

  public boolean validateExpiryDate(String expiryDate) {

    if (expiryDate.length() != 5) {
      LOG.info("Card validation failed");
      return false;
    }else{
      return true;
    }

  }
}