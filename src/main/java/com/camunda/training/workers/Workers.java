package com.camunda.training.workers;

import com.camunda.training.services.CreditCardService;
import com.camunda.training.services.CustomerService;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.worker.JobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Component
public class Workers {

  @Autowired
  ZeebeClient zeebeClient;
  
  private static final Logger LOG = LoggerFactory.getLogger(Workers.class);


  @JobWorker
  public void logging(ActivatedJob job) {
    LOG.info("Works for process instance {}", job.getProcessInstanceKey());
  }

  @JobWorker( type = "credit-deduction", autoComplete = false)
  public void creditDeduction (ActivatedJob job, JobClient client){
    LOG.info("Working on job {} for element {}", job.getKey(), job.getElementId());
    CustomerService customerService = new CustomerService();
    Map<String, Object> inputVariables = job.getVariablesAsMap();
    String customerId = (String)inputVariables.get("customerId");
    Double amount = (Double) inputVariables.get("orderTotal");
    Double customerCredit = customerService.getCustomerCredit(customerId);


    Map<String,Object> resultVariables = new HashMap<>();
    resultVariables.put("customerCredit", customerCredit);

    Double remainingAmount = amount - customerCredit;


      resultVariables.put("amount", remainingAmount);


    LOG.info("deducted credit: {}", resultVariables);
    client.newCompleteCommand(job).variables(resultVariables).send().join();
  }



  @JobWorker( type = "charge-credit", autoComplete = false)
  public void chargeCredit (ActivatedJob job, JobClient client){
    LOG.info("Working on job {} for element {}", job.getKey(), job.getElementId());
    CustomerService customerService = new CustomerService();
    Map<String, Object> inputVariables = job.getVariablesAsMap();
    CreditCardService creditCardService = new CreditCardService();
    String cardNumber = (String)inputVariables.get("cardNumber");
    String cvc = (String)inputVariables.get("cvc");
    String expiryDate = (String)inputVariables.get("expiryDate");
    Double amount = (Double)inputVariables.get("amount");
    LOG.info("Received credit card number: {}, cvc: {}, expiryDate: {}, amount: {}", cardNumber,cvc,expiryDate,amount);
    if (!creditCardService.validateExpiryDate(expiryDate)) {
//      client.newFailCommand(job.getKey()).retries(0).errorMessage("Card validation failed").send().exceptionally(throwable -> {
//        throw new RuntimeException("Could not completed the job", throwable);
//      });
      client.newThrowErrorCommand(job)
              .errorCode("creditCardChargeError")
              .errorMessage("card validation failed")
              .send()
              .exceptionally(t -> {throw new RuntimeException("Could not throw BPMN error: " + t.getMessage(), t);});
    }else {
      creditCardService.chargeAmount(cardNumber, cvc, expiryDate,  amount);
      client.newCompleteCommand(job).send().join();
    }
  }

  @JobWorker ( type="payment-invocation", autoComplete = false)
    public void invokePayment (ActivatedJob job, JobClient client){
      LOG.info("Invoke payment for process instances {}", job.getProcessInstanceKey());
      Random random = new Random();
      String orderId = random.nextInt(10000) + "";
      Map<String, Object> payload = job.getVariablesAsMap();
      payload.put("orderId",orderId);
      publishMessage("paymentRequestMessage",orderId,payload);
      client.newCompleteCommand(job).variables(Map.of("orderId",orderId)).send().join();

    }

    protected void publishMessage(String messageName, String orderId, Map<String,Object> variables){
    zeebeClient.newPublishMessageCommand().messageName(messageName).correlationKey(orderId).variables(variables).send().join();
    LOG.info("message {} send", messageName);
  }



  @JobWorker ( type="payment-completion", autoComplete = false)
  public void paymentcompletion (ActivatedJob job, JobClient client){
    LOG.info("Invoke payment completion for job:{}", job.getProcessInstanceKey());
    Random random = new Random();

    Map<String, Object> payload = job.getVariablesAsMap();
    String orderId = (String) payload.get("orderId");
    zeebeClient.newPublishMessageCommand().messageName("paymentCompletedMessage").correlationKey(orderId).send().join();
    client.newCompleteCommand(job).send().join();
  }


}
