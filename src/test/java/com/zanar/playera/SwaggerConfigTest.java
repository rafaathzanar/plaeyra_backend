package com.zanar.playera;

import com.zanar.playera.config.SwaggerConfig;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "app.base-url=http://localhost:8080",
    "app.frontend-url=http://localhost:3000"
})
public class SwaggerConfigTest {

  @Autowired
  private SwaggerConfig swaggerConfig;

  @Test
  public void testSwaggerConfigBeanCreation() {
    assertNotNull(swaggerConfig);
  }

  @Test
  public void testOpenAPIConfiguration() {
    OpenAPI openAPI = swaggerConfig.customOpenAPI();

    assertNotNull(openAPI);
    assertNotNull(openAPI.getInfo());
    assertEquals("PlayEra API Documentation", openAPI.getInfo().getTitle());
    assertEquals("1.0.0", openAPI.getInfo().getVersion());
    assertNotNull(openAPI.getInfo().getDescription());

    assertNotNull(openAPI.getServers());
    assertFalse(openAPI.getServers().isEmpty());

    assertNotNull(openAPI.getComponents());
    assertNotNull(openAPI.getComponents().getSecuritySchemes());
    assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("Bearer Authentication"));
  }

  @Test
  public void testSwaggerConfigProperties() {
    // Test that the configuration can be loaded
    assertDoesNotThrow(() -> {
      swaggerConfig.customOpenAPI();
    });
  }
}


