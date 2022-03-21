package com.antonr.ioc.exception;

public class BeanInstantiationException extends RuntimeException {

  public BeanInstantiationException(String message, Throwable e) {
    super(message, e);
  }

}
