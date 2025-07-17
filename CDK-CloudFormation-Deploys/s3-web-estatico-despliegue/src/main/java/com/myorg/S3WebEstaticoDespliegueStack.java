package com.myorg;

import software.constructs.Construct;

import java.util.List;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.origins.S3BucketOrigin;
import software.amazon.awscdk.services.cloudfront.origins.S3StaticWebsiteOrigin;
import software.amazon.awscdk.services.cloudfront.origins.S3StaticWebsiteOriginProps;
import software.amazon.awscdk.services.iam.AnyPrincipal;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.BlockPublicAccessOptions;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketAccessControl;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;

public class S3WebEstaticoDespliegueStack extends Stack {
    public S3WebEstaticoDespliegueStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public S3WebEstaticoDespliegueStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        
        BlockPublicAccess noblock = BlockPublicAccess.Builder.create()
        		.blockPublicPolicy(false)
        		.ignorePublicAcls(false)
        		.blockPublicAcls(false)
        		.restrictPublicBuckets(false)
        		.build();
        
        // Se crea el bucket
        Bucket bucket = Bucket.Builder.create(this, "s3-cdk-web-estatico")
        		.websiteIndexDocument("index.html")
        		.websiteErrorDocument("error.html")
        		.removalPolicy(RemovalPolicy.DESTROY)
        		.autoDeleteObjects(true)
        		.blockPublicAccess(noblock)
        		.build();
        
        // Agregar archivos html
        BucketDeployment.Builder.create(this, "HTMLFiles")
                .sources(List.of(Source.asset("html-files/")))
                .destinationBucket(bucket)
                .build();
        
        // Se agrega la política para permitir el acceso público
        bucket.addToResourcePolicy(PolicyStatement.Builder.create()
        	    .effect(Effect.ALLOW)
        	    .principals(List.of(new AnyPrincipal()))
        	    .actions(List.of("s3:GetObject"))
        	    .resources(List.of(bucket.getBucketArn() + "/*"))
        	    .build()
        );
        
        
        // Creación del CloudFront
        Distribution.Builder.create(this, "cf-s3-web-estatico")
            .defaultBehavior(
                BehaviorOptions.builder()
                    .origin(
                        new S3StaticWebsiteOrigin(
                            bucket,
                            S3StaticWebsiteOriginProps.builder()
                                .build()
                        )
                    )
                    .build()
            )
            .build()
            .applyRemovalPolicy(RemovalPolicy.DESTROY);
    }
}
