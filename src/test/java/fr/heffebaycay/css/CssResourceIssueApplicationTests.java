package fr.heffebaycay.css;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CssResourceIssueApplicationTests {

    @LocalServerPort
    int port;


    @Test
    public void issue_SPR_14597_external_resource_name_is_cached_using_non_unique_key() throws IOException {
        String alphaCss = getCssForTheme("alpha");
        String alphaLogoName = getExternalCssResourceName(alphaCss);

        // This should succeed
        assertThat(alphaLogoName).isEqualTo("logo-1cf43f6ba5cbc71b4b2d040f2a358f3e.png");

        String betaCss = getCssForTheme("beta");
        String betaLogoName = getExternalCssResourceName(betaCss);

        // This will fail because of the resource name caching issue
        assertThat(betaLogoName).isEqualTo("logo-9066b55828deb3c10e27e609af322c40.png");
    }

    private String getCssForTheme(String theme) throws IOException {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(String.format("http://127.0.0.1:%d/static/%s/style.css", port, theme));

        HttpResponse response = httpClient.execute(request);

        return IOUtils.toString(response.getEntity().getContent(), "UTF-8");
    }

    private String getExternalCssResourceName(String alphaCss) {
        Pattern pattern = Pattern.compile("url\\(\"(.*)\"\\)");
        Matcher matcher = pattern.matcher(alphaCss);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

}
