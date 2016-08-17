package fr.heffebaycay.css;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.resource.VersionResourceResolver;

@Configuration
public class WebConfig extends DelegatingWebMvcConfiguration {

    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        VersionResourceResolver versionResourceResolver = new VersionResourceResolver()
                .addContentVersionStrategy("/**");

        registry
                .addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0)
                .resourceChain(true)
                .addResolver(versionResourceResolver);

    }
}
