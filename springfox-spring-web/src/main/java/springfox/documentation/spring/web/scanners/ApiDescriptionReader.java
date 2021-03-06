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

package springfox.documentation.spring.web.scanners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import springfox.documentation.service.ApiDescription;
import springfox.documentation.spi.service.contexts.ApiSelector;
import springfox.documentation.spi.service.contexts.RequestMappingContext;
import springfox.documentation.spring.web.readers.operation.ApiOperationReader;

import java.util.List;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Ordering.*;

@Component
public class ApiDescriptionReader {

  private final ApiOperationReader operationReader;

  @Autowired
  public ApiDescriptionReader(ApiOperationReader operationReader) {
    this.operationReader = operationReader;
  }

  public List<ApiDescription> read(RequestMappingContext outerContext) {
    RequestMappingInfo requestMappingInfo = outerContext.getRequestMappingInfo();
    HandlerMethod handlerMethod = outerContext.getHandlerMethod();
    PatternsRequestCondition patternsCondition = requestMappingInfo.getPatternsCondition();
    ApiSelector selector = outerContext.getDocumentationContext().getApiSelector();

    List<ApiDescription> apiDescriptionList = newArrayList();
    for (String path : matchingPaths(selector, patternsCondition)) {
        String methodName = handlerMethod.getMethod().getName();
        RequestMappingContext operationContext = outerContext.copyPatternUsing(path);

        operationReader.read(operationContext);
        operationContext.apiDescriptionBuilder().path(path);
        operationContext.apiDescriptionBuilder().description(methodName);
        operationContext.apiDescriptionBuilder().hidden(false);
        apiDescriptionList.add(operationContext.apiDescriptionBuilder().build());
    }
    return apiDescriptionList;
  }

    private List<String> matchingPaths(ApiSelector selector, PatternsRequestCondition patternsCondition) {
        return natural().sortedCopy(from(patternsCondition.getPatterns())
            .filter(selector.getPathSelector()));
    }

}
