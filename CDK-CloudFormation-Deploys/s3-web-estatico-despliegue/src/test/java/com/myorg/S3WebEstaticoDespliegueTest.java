// package com.myorg;

// import software.amazon.awscdk.App;
// import software.amazon.awscdk.assertions.Template;
// import java.io.IOException;

// import java.util.HashMap;

// import org.junit.jupiter.api.Test;

// example test. To run these tests, uncomment this file, along with the
// example resource in java/src/main/java/com/myorg/S3WebEstaticoDespliegueStack.java
// public class S3WebEstaticoDespliegueTest {

//     @Test
//     public void testStack() throws IOException {
//         App app = new App();
//         S3WebEstaticoDespliegueStack stack = new S3WebEstaticoDespliegueStack(app, "test");

//         Template template = Template.fromStack(stack);

//         template.hasResourceProperties("AWS::SQS::Queue", new HashMap<String, Number>() {{
//           put("VisibilityTimeout", 300);
//         }});
//     }
// }
