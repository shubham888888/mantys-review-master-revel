package com.pearrity.mantys.repository.config;

import com.google.gson.Gson;
import com.pearrity.mantys.domain.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pearrity.mantys.domain.utils.Constants.*;

@Service
public class AwsSecretsService {

  private static final Logger log = Logger.getLogger(AwsSecretsService.class.getName());
  private final String accessKey;
  private final String secretKey;
  private final String region;
  private final String profile;
  private final Gson gson;

  public AwsSecretsService(@Autowired Environment environment) {
    accessKey = environment.getProperty("cloud.aws.credentials.accessKey");
    secretKey = environment.getProperty("cloud.aws.credentials.secretKey");
    region = environment.getProperty("cloud.aws.credentials.region");
    profile = environment.getProperty("spring.profiles.active");
    gson = new Gson();
  }

  public <T> T getSecret(String secretName, Class<T> type) {
    if (Objects.equals(profile, Constants.LOCAL) || Objects.equals(profile, Constants.DEV)) {
      try {
        HashMap map =
            gson.fromJson(
                getResourceFileAsString(AwsSecretsService.class, profile + "-kms-config.json"),
                HashMap.class);
        return gson.fromJson(gson.toJson(map.get(profile + "." + secretName)), type);
      } catch (Exception e) {
        throw new RuntimeException();
      }
    }

    final String secret_Name = profile + "/mantys";
    Region region = Region.of(this.region);

    AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKey, secretKey);
    SecretsManagerClient client =
        SecretsManagerClient.builder()
            .region(region)
            .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
            .build();

    GetSecretValueRequest getSecretValueRequest =
        GetSecretValueRequest.builder().secretId(secret_Name).build();
    GetSecretValueResponse getSecretValueResponse;
    try {
      getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage());
      throw e;
    }

    if (getSecretValueResponse.secretString() != null) {
      String secrets = getSecretValueResponse.secretString();
      HashMap map = gson.fromJson(secrets, HashMap.class);
      return gson.fromJson(((String) map.get(profile + "." + secretName)), type);
    }
    return null;
  }
}
