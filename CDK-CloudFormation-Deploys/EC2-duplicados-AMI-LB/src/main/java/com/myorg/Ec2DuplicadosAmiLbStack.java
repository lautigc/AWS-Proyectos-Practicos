package com.myorg;

import software.constructs.Construct;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.BlockDevice;
import software.amazon.awscdk.services.ec2.BlockDeviceVolume;
import software.amazon.awscdk.services.ec2.EbsDeviceOptions;
import software.amazon.awscdk.services.ec2.EbsDeviceVolumeType;
import software.amazon.awscdk.services.ec2.IMachineImage;
import software.amazon.awscdk.services.ec2.Instance;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.UserData;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.targets.InstanceIdTarget;


public class Ec2DuplicadosAmiLbStack extends Stack {
    public Ec2DuplicadosAmiLbStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public Ec2DuplicadosAmiLbStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        
        // Default VPC with public subnet
        Vpc vpc = Vpc.Builder.create(this, "Default-VPC")
				.subnetConfiguration(List.of(SubnetConfiguration.builder()
						.subnetType(SubnetType.PUBLIC)
						.name("Ingress")
						.build()
						)
				)
        		.build();
        vpc.applyRemovalPolicy(RemovalPolicy.DESTROY);
        
        // Free tier Image for sao-paulo region, for other region add it.
        IMachineImage AMIFree = MachineImage.genericLinux(Map.of(
        		"sa-east-1", "ami-0f3564425eaa7d1d8"
        		)
        	);
        
        // Basic Free tier Instance Type
        InstanceType t2Micro = InstanceType.of(InstanceClass.T2, InstanceSize.MICRO);

        // Basic storage with 8GB and GP3 technology
        // observe that path "/dev/xvda" is the root volume for selected AMI
        BlockDevice blockDevice = BlockDevice.builder()
        		.deviceName("/dev/xvda")
        		.volume(BlockDeviceVolume.ebs(
        				16,
        				EbsDeviceOptions.builder()
        				.volumeType(EbsDeviceVolumeType.GP3)
        				.build()
        			)
        		)
        		.build();

        // Security group with open http port and anyone out
        SecurityGroup securityGroup = SecurityGroup.Builder.create(this, "custom-sg")
        		.allowAllOutbound(Boolean.TRUE)
        		.vpc(vpc)
        		.build();
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.HTTP);
        securityGroup.addIngressRule(Peer.anyIpv6(), Port.HTTP);
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.SSH);
        securityGroup.addIngressRule(Peer.anyIpv6(), Port.SSH);
        securityGroup.applyRemovalPolicy(RemovalPolicy.DESTROY);
        
        // Config script to create http server on ec2 instance
        UserData configScript = UserData.forLinux();
        configScript.addCommands(
            "#!/bin/bash",
            "yum update -y",
            "yum install -y httpd",
            "systemctl start httpd",
            "systemctl enable httpd",
            "echo \"<h1>Hola desde el EC2: $(hostname -f)</h1>\" > /var/www/html/index.html"
        );
        
        // Create "N" instances for load balancer
        final int N = 3;
        List<Instance> instances = IntStream.range(0, N)
        	    .mapToObj(i -> Instance.Builder.create(this, "Base-EC2-" + i)
        	            .machineImage(AMIFree)
        	            .instanceType(t2Micro)
        	            .blockDevices(List.of(blockDevice))
        	            .vpc(vpc)
        	            .securityGroup(securityGroup)
        	            .associatePublicIpAddress(Boolean.TRUE)
        	            .userData(configScript)
        	            .build()
        	    )
        	    .peek(instance -> instance.applyRemovalPolicy(RemovalPolicy.DESTROY))
        	    .toList();
        
        // Create load balancer
        ApplicationLoadBalancer lb = ApplicationLoadBalancer.Builder.create(this, "LoadBalancer")
                .vpc(vpc)
                .internetFacing(Boolean.TRUE)
                .securityGroup(securityGroup)
                .build();
        lb.applyRemovalPolicy(RemovalPolicy.DESTROY);
        
        // Adding listener and target group
        ApplicationListener listener = lb.addListener("HTTP-listener", BaseApplicationListenerProps.builder()
        		.port(80)
        		.open(Boolean.TRUE)
        		.build()
        );
        listener.addTargets("Local-target-group", AddApplicationTargetsProps.builder()
        		.port(80)
        		.targets(
        			instances.stream()
        				.map(i -> new InstanceIdTarget(i.getInstanceId()))
        				.toList()
        		)
        		.build()
        );
    }
}
