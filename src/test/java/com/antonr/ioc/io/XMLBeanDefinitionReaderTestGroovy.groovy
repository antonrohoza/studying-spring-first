package com.antonr.ioc.io

import com.antonr.ioc.entity.BeanDefinition
import com.antonr.ioc.exception.SourceParseException

class XMLBeanDefinitionReaderTestGroovy extends GroovyTestCase {
    private def expectedBeanDefinitions = []

    @Override
    void setUp() {
        expectedBeanDefinitions.add(new BeanDefinition(id: "mailService",
                beanClassName: "com.antonr.ioc.service.MailService",
                dependencies: ["protocol": "POP3", "port": "3000"]))
        expectedBeanDefinitions.add(new BeanDefinition(id: "userService",
                beanClassName: "com.antonr.ioc.service.UserService",
                // password dependency from context.xml
                dependencies: ["password": "lalala"],
                refDependencies: ["mailService": "mailService"]))
        expectedBeanDefinitions.add(new BeanDefinition(id: "paymentService",
                beanClassName: "com.antonr.ioc.service.PaymentService",
                refDependencies: ["mailService": "mailService"]))
        expectedBeanDefinitions.add(new BeanDefinition(id: "paymentWithMaxService",
                beanClassName: "com.antonr.ioc.service.PaymentService", dependencies:
                ["maxAmount": "500"], refDependencies: ["mailService": "mailService"]))

    }

    void testGetBeanDefinitions() {
        XMLBeanDefinitionReader xmlBeanDefinitionReader = new XMLBeanDefinitionReader("src/test/resources/context.xml")
        def actualBeanDefinitions = xmlBeanDefinitionReader.getBeanDefinitions()
        expectedBeanDefinitions.each { assertTrue(actualBeanDefinitions.remove(it)) }
    }

    void testGetBeanDefinitionsException() {
        shouldFail(SourceParseException) {
            new XMLBeanDefinitionReader("src/test/resources/context-exception.xml").getBeanDefinitions()
        }
    }
}
