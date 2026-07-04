# 014 Docker And Deployment Skeleton

## Goal

Create the initial Docker packaging and Kubernetes deployment skeleton for AWS.

## Scope

- Add Dockerfile.
- Add Docker build documentation or Gradle task.
- Add Kubernetes manifest or Helm skeleton.
- Configure `prod` profile activation.
- Do not include production secrets.

## Deliverables

- Dockerfile.
- `.dockerignore`.
- Kubernetes or Helm skeleton.
- Example non-secret deployment configuration.

## Tests

- Verify Docker image builds locally when Docker is available.
- Verify manifests do not include secrets.

## Done When

- The application can be packaged as a Docker image.
- Deployment skeleton targets Kubernetes on AWS.
- Secrets remain external.
