package com.antonr.ioc.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Bean {

  private String id;
  private Object value;

}
