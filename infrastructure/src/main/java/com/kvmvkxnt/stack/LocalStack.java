package com.kvmvkxnt.stack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awscdk.App;
import software.amazon.awscdk.AppProps;
import software.amazon.awscdk.BootstraplessSynthesizer;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Token;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.CloudMapNamespaceOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

public class LocalStack extends Stack {

  private final Vpc vpc;
  private final Cluster ecsCluster;

  public LocalStack(final App scope, final String id, final StackProps props) {
    super(scope, id, props);

    this.vpc = createVpc();

    DatabaseInstance authServiceDb = createDatabase("AuthServiceDB", "auth-service-db");

    DatabaseInstance patientServiceDb = createDatabase("PatientServiceDB", "patient-service-db");

    CfnHealthCheck authDbHealthCheck =
        createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");

    CfnHealthCheck patientDbHealthCheck =
        createDbHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");

    CfnCluster mskCluster = createMskCluster();

    this.ecsCluster = createEcsCluster();

    FargateService authService =
        createFargateService(
            "AuthService",
            "auth-service",
            List.of(4005),
            authServiceDb,
            Map.of(
                "JWT_SECRET",
                "7c46379a74e02d520eb9c6da45ee7c3341f15ccc64a765351e373a91cc1bc2d3dc81c7cd5597fe3aa97c719056aa7ee456277adb5820b586bdcb84872124e6d5ae75f304239203ac97ca58546d7004bb55d12117bb2a133c46dc3565575c5227fa794a1384ff74bec74279333f36ed4252bac76d665583aa6549ff7f9fd509dd99032fc521c62e9d890eb5884511eb8c94c9babf55b589161284b19cf7e7166175d586fe64b2a95a799955df916e98fe146465585238891d1bc9e4149578f5144a1f8d285f34b5f63d86a14779fee928d4dcf5262b12d173c02d91aa947c21888e33cb6445e5d88257ee76326328d65bf3b3ca38b6bb6f08cb2d52f61718515b"));
    authService.getNode().addDependency(authDbHealthCheck);
    authService.getNode().addDependency(authServiceDb);

    FargateService billingService =
        createFargateService("BillingService", "billing-service", List.of(4001, 9001), null, null);

    FargateService analyticsService =
        createFargateService("AnalyticsService", "analytics-service", List.of(4002), null, null);
    analyticsService.getNode().addDependency(mskCluster);

    FargateService patientService =
        createFargateService(
            "PatientService",
            "patient-service",
            List.of(4000),
            patientServiceDb,
            Map.of(
                "BILLING_SERVICE_ADDRESS",
                "host.docker.internal",
                "BILLING_SERVICE_GRPC_PORT",
                "9001"));
    patientService.getNode().addDependency(patientServiceDb);
    patientService.getNode().addDependency(patientDbHealthCheck);
    patientService.getNode().addDependency(billingService);
    patientService.getNode().addDependency(mskCluster);

    createApiGatewayService();
  }

  private Vpc createVpc() {
    return Vpc.Builder.create(this, "PatientManagementVPC")
        .vpcName("PatientManagementVPC")
        .maxAzs(2) // Default is all AZs in region
        .build();
  }

  private DatabaseInstance createDatabase(String id, String dbName) {
    return DatabaseInstance.Builder.create(this, id)
        .engine(
            DatabaseInstanceEngine.postgres(
                PostgresInstanceEngineProps.builder()
                    .version(PostgresEngineVersion.VER_17_2)
                    .build()))
        .vpc(vpc)
        .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
        .allocatedStorage(20)
        .credentials(Credentials.fromGeneratedSecret("admin_user"))
        .databaseName(dbName)
        .removalPolicy(RemovalPolicy.DESTROY)
        .build();
  }

  private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
    return CfnHealthCheck.Builder.create(this, id)
        .healthCheckConfig(
            CfnHealthCheck.HealthCheckConfigProperty.builder()
                .type("TCP")
                .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                .ipAddress(db.getDbInstanceEndpointAddress())
                .requestInterval(30)
                .failureThreshold(3)
                .build())
        .build();
  }

  private CfnCluster createMskCluster() {
    return CfnCluster.Builder.create(this, "MskCluster")
        .clusterName("kafka-cluster")
        .kafkaVersion("2.8.0")
        .numberOfBrokerNodes(2)
        .brokerNodeGroupInfo(
            CfnCluster.BrokerNodeGroupInfoProperty.builder()
                .instanceType("kafka.m5.xlarge")
                .clientSubnets(
                    vpc.getPrivateSubnets().stream()
                        .map(ISubnet::getSubnetId)
                        .collect(Collectors.toList()))
                .brokerAzDistribution("DEFAULT")
                .build())
        .build();
  }

  private Cluster createEcsCluster() {
    return Cluster.Builder.create(this, "PatientManagementCluster")
        .vpc(vpc)
        .defaultCloudMapNamespace(
            CloudMapNamespaceOptions.builder().name("patient-management.local").build())
        .build();
  }

  private FargateService createFargateService(
      String id,
      String imageName,
      List<Integer> ports,
      DatabaseInstance db,
      Map<String, String> additionalEnvVars) {
    FargateTaskDefinition taskDefinition =
        FargateTaskDefinition.Builder.create(this, id + "Task")
            .cpu(256)
            .memoryLimitMiB(512)
            .build();
    ContainerDefinitionOptions.Builder containerOptions =
        ContainerDefinitionOptions.builder()
            .image(ContainerImage.fromRegistry(imageName))
            .portMappings(
                ports.stream()
                    .map(
                        port ->
                            PortMapping.builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build())
                    .toList())
            .logging(
                LogDriver.awsLogs(
                    AwsLogDriverProps.builder()
                        .logGroup(
                            LogGroup.Builder.create(this, id + "LogGroup")
                                .logGroupName("/ecs/" + imageName)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY)
                                .build())
                        .streamPrefix(imageName)
                        .build()));
    Map<String, String> envVars = new HashMap<>();
    envVars.put(
        "SPRING_KAFKA_BOOTSTRAP_SERVERS",
        "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511,"
            + " localhost.localstack.cloud:4512");

    if (additionalEnvVars != null) {
      envVars.putAll(additionalEnvVars);
    }

    if (db != null) {
      envVars.put(
          "SPRING_DATASOURCE_URL",
          "jdbc:postgresql://%s:%s/%s-db"
              .formatted(
                  db.getDbInstanceEndpointAddress(), db.getDbInstanceEndpointPort(), imageName));
      envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
      envVars.put(
          "SPRING_DATASOURCE_PASSWORD", db.getSecret().secretValueFromJson("password").toString());
      envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
      envVars.put("SPRING_SQL_INIT_MODE", "always");
      envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
    }

    containerOptions.environment(envVars);
    taskDefinition.addContainer(imageName + "Container", containerOptions.build());

    return FargateService.Builder.create(this, id)
        .cluster(ecsCluster)
        .taskDefinition(taskDefinition)
        .assignPublicIp(false)
        .serviceName(imageName)
        .build();
  }

  private void createApiGatewayService() {

    FargateTaskDefinition taskDefinition =
        FargateTaskDefinition.Builder.create(this, "APIGatewayTaskDefinition")
            .cpu(256)
            .memoryLimitMiB(512)
            .build();

    ContainerDefinitionOptions containerOptions =
        ContainerDefinitionOptions.builder()
            .image(ContainerImage.fromRegistry("api-gateway"))
            .environment(
                Map.of(
                    "SPRING_PROFILES_ACTIVE",
                    "prod",
                    "AUTH_SERVICE_URL",
                    "http://host.docker.internal:4005"))
            .portMappings(
                List.of(4004).stream()
                    .map(
                        port ->
                            PortMapping.builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build())
                    .toList())
            .logging(
                LogDriver.awsLogs(
                    AwsLogDriverProps.builder()
                        .logGroup(
                            LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                .logGroupName("/ecs/" + "api-gateway")
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY)
                                .build())
                        .streamPrefix("api-gateway")
                        .build()))
            .build();

    taskDefinition.addContainer("APIGatewayContainer", containerOptions);

    ApplicationLoadBalancedFargateService apiGateway =
        ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService")
            .cluster(ecsCluster)
            .serviceName("api-gateway")
            .taskDefinition(taskDefinition)
            .desiredCount(1)
            .healthCheckGracePeriod(Duration.seconds(60))
            .build();
  }

  public static void main(String[] args) {
    App app = new App(AppProps.builder().outdir("./cdk.out").build());
    StackProps props = StackProps.builder().synthesizer(new BootstraplessSynthesizer()).build();

    new LocalStack(app, "localstack", props);
    app.synth();
    System.out.println("App synthesizing in progress...");
  }
}
