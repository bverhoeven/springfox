/*
 *
 *  Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package springfox.documentation.spring.web.readers.operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import springfox.documentation.OperationNameGenerator;
import springfox.documentation.builders.OperationBuilder;
import springfox.documentation.service.Operation;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spi.service.contexts.PathContext;
import springfox.documentation.spi.service.contexts.RequestMappingContext;
import springfox.documentation.spring.web.OperationsKeyGenerator;
import springfox.documentation.spring.web.plugins.DocumentationPluginsManager;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.FluentIterable.*;
import static com.google.common.collect.Lists.*;
import static java.util.Arrays.asList;

@Component
public class ApiOperationReader {

  private static final Set<RequestMethod> allRequestMethods
          = new LinkedHashSet<RequestMethod>(asList(RequestMethod.values()));
  private final DocumentationPluginsManager pluginsManager;
  private final OperationNameGenerator nameGenerator;

  @Autowired
  public ApiOperationReader(DocumentationPluginsManager pluginsManager, OperationNameGenerator nameGenerator) {
    this.pluginsManager = pluginsManager;
    this.nameGenerator = nameGenerator;
  }

  @Cacheable(value = "operations", key = OperationsKeyGenerator.OPERATION_KEY_SPEL)
  public List<Operation> read(RequestMappingContext outerContext) {

    RequestMappingInfo requestMappingInfo = outerContext.getRequestMappingInfo();
    String requestMappingPattern = outerContext.getRequestMappingPattern();
    RequestMethodsRequestCondition requestMethodsRequestCondition = requestMappingInfo.getMethodsCondition();
    List<Operation> operations = newArrayList();

    Set<RequestMethod> requestMethods = requestMethodsRequestCondition.getMethods();
    Set<RequestMethod> supportedMethods = supportedMethods(requestMethods);

    //Setup response message list
    Integer currentCount = 0;
    for (RequestMethod httpRequestMethod : supportedMethods) {
      OperationContext operationContext = new OperationContext(new OperationBuilder(nameGenerator),
              httpRequestMethod,
              outerContext.getHandlerMethod(),
              currentCount,
              requestMappingInfo,
              outerContext.getDocumentationContext(), requestMappingPattern);

      Operation operation = pluginsManager.operation(operationContext);
      if (!operation.isHidden()) {
        operations.add(operation);
        currentCount++;
      }
    }
    Collections.sort(operations, outerContext.operationOrdering());
    outerContext.apiDescriptionBuilder().operations(operations);
    outerContext.apiDescriptionBuilder().pathDecorator(
        pluginsManager.decorator(new PathContext(outerContext, from(operations).first())));
    return operations;
  }

  private Set<RequestMethod> supportedMethods(Set<RequestMethod> requestMethods) {
    return requestMethods == null || requestMethods.isEmpty()
            ? allRequestMethods
            : requestMethods;
  }

}
