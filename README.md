# Using CssLinkResourceTransformer with caching

This repository is there to illustrate the [SPR-14597](https://jira.spring.io/browse/SPR-14597) bug report


## Repro Steps

- Launch this Spring boot application
- Access the following URL: http://localhost:8080/static/beta/style.css
- Notice that the logo file referenced in this css file is `logo-9066b55828deb3c10e27e609af322c40.png`
- Now, access the following URL: http://localhost:8080/static/alpha/style.css
- Notice that the logo file referenced in this css file is still `logo-9066b55828deb3c10e27e609af322c40.png`

Here are the expected MD5 values :

File name | Expected MD5
--------- | ------------
 static/alpha/logo.png | 1cf43f6ba5cbc71b4b2d040f2a358f3e
 static/beta/logo.png | 9066b55828deb3c10e27e609af322c40


## What's happening ?

When `CssLinkResourceTransformer` is invoked, it checks the selected css file
for external resources inclusions and executes the following code for each match :

```java
newLink = resolveUrlPath(link, request, resource, transformerChain);
```

Here's the source code of the `resolveUrlPath` method:

```java
protected String resolveUrlPath(String resourcePath, HttpServletRequest request,
			Resource resource, ResourceTransformerChain transformerChain) {

    if (resourcePath.startsWith("/")) {
        // full resource path
        ResourceUrlProvider urlProvider = findResourceUrlProvider(request);
        return (urlProvider != null ? urlProvider.getForRequestUrl(request, resourcePath) : null);
    }
    else {
        // try resolving as relative path
        return transformerChain.getResolverChain().resolveUrlPath(
                resourcePath, Collections.singletonList(resource));
    }
}
```

Since the 'logo.png' resource inclusion doesn't start with a '/', the component asks the resolver
chain to resolve the given path. The `resourcePath` variable here will be set to `logo.png`.

The first item from the resolver chain is none other than `CachingResourceResolver`, so
the following method is invoked:

```java
protected String resolveUrlPathInternal(String resourceUrlPath,
        List<? extends Resource> locations, ResourceResolverChain chain) {

    String key = RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath;
    String resolvedUrlPath = this.cache.get(key, String.class);

    if (resolvedUrlPath != null) {
        if (logger.isTraceEnabled()) {
            logger.trace("Found match: \"" + resolvedUrlPath + "\"");
        }
        return resolvedUrlPath;
    }

    resolvedUrlPath = chain.resolveUrlPath(resourceUrlPath, locations);
    if (resolvedUrlPath != null) {
        if (logger.isTraceEnabled()) {
            logger.trace("Putting resolved resource URL path in cache: \"" + resolvedUrlPath + "\"");
        }
        this.cache.put(key, resolvedUrlPath);
    }

    return resolvedUrlPath;
}
```

This essentially delegates url path resolution to the chain unless the path is in the internal cache.
However, the key chosen for cache storage is `String key = RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath`... which is none other than `RESOLVED_URL_PATH_CACHE_KEY_PREFIX + "logo.png"`.

This key is guaranteed not to be unique, as the project contains multiple files
called `logo.png` that are included in different css files.