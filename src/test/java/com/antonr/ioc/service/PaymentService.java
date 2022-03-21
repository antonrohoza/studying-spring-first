package com.antonr.ioc.service;

import lombok.Data;
import lombok.Setter;

@Data
@Setter
public class PaymentService {

  private MailService mailService;
  private int maxAmount;

}
