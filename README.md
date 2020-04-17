# Che API sidecar image

This repo hosts code necessary for building a container that can serve as a simple implementation of the full [Eclipse Che](https://github.com/eclipse/che) server API. It is intended to be used as a sidecar container to allow a che-theia container to obtain workspace information without additional workarounds.

## Dependencies
This project uses Quarkus and GraalVM to build a small, efficient container suitable for deployment as a sidecar. It depends on
- Quarkus 0.21.2
- GraalVM 19.1.1

Using a different version of either dependency will likely cause issues.

## Building

### Building the native binary using GraalVM
There are two options for building the native executable

#### Building locally
Install GraalVM as described above and execute
```
package -Pnative -Dquarkus.native.container-build=true
```
This method depends on the specific version of GraalVM being installed and configured according to the Quarkus documentation.

#### Building in a docker container
Included in this repo is `build.Dockerfile`, which can be used to produce the required binary without the need to install GraalVM.

```
# Build binary in docker container
docker build --ulimit nofile=122880:122880 -m 5G \
    -t che-rest-apis-builder -f build.Dockerfile .
# Create a container out of the image
docker create --name builder che-rest-apis-builder
# Copy target folder out of image
docker cp builder:/usr/src/app/target ./target
# Clean up
docker rm builder
```
The `ulimit` parameter is required to work around an issue with error
> ```
> library initialization failed - unable to allocate file descriptor table - out of memory#
> ```
related to Java builds allocating too many file descriptors; it may not be necessary depending on the system.

### Building the native binary container
The final che-rest-apis container depends on the output of the build above, and is created using the dockerfile in `src/main/docker/Dockerfile`:
```
docker build -t che-rest-apis -f ./src/main/docker/Dockerfile .
```
