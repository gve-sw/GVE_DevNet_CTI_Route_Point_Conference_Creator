# GVE_DevNet_CTI_Route_Point_Conference_Creator

## Contacts

* Rey Diaz (rediaz@cisco.com)

## Solution Components

* Cisco Unified Communications Manager
* Cisco Jabber

## Installation/Configuration

### Requirements

- [OpenJDK](https://openjdk.java.net/) 11
- [Apache Maven](https://maven.apache.org/) 3.6.3
- [Visual Studio Code](https://code.visualstudio.com/)
- A working Cisco Unified Communications Manager environment:

   - An CUCM application-user or end-user username/password, with the following roles:

      - `Standard CTI Allow Control of Phones supporting Connected Xfer and conf`
      - `Standard CTI Allow Control of Phones supporting Rollover Mode`
      - `Standard CTI Enabled`
      - `Standard CTI Allow Control of all Devices`

   -  [CTI suported phone devices](https://developer.cisco.com/site/jtapi/documents/cti-tapi-jtapi-supported-device-matrix/) (i.e. Jabber soft phones), configured with at least one shared directory number.

      > Note, ensure all internal directory numbers have `Allow Control of Device from CTI` enabled

### Getting Started

1. Make sure you have OpenJDK 11 installed, `java` is available in the path, and `$JAVA_HOME` points to the right directory:

```bash {"id":"01HWAXPM2EXPE5ESV6DKGNKBWW"}
$ java -version
openjdk 11.0.8 2020-07-14
OpenJDK Runtime Environment (build 11.0.8+10-post-Ubuntu-0ubuntu120.04)
OpenJDK 64-Bit Server VM (build 11.0.8+10-post-Ubuntu-0ubuntu120.04, mixed mode, sharing)

```

```bash {"id":"01HWAXPM2EXPE5ESV6DP15ZH2N"}
$ echo $JAVA_HOME
/usr/lib/jvm/java-1.11.0-openjdk-amd64

```

2. Open a terminal and use `git` to clone this repository

```bash {"id":"01HWAXPM2EXPE5ESV6DRGV6KA1"}
git clone https://wwwin-github.cisco.com/gve/VE_DevNet_Jabber_Conference_Initiation_With_Original_User.git

```

3. Open the Java project in [Visual Studio Code](https://code.visualstudio.com/):

```bash {"id":"01HWAXPM2EXPE5ESV6DRPEC220"}
cd GVE_DevNet_Jabber_Custom_Tab_Shared_Lines_Status
code .

```

4. Edit rename `.env.example` to `.env`, and edit to specify environment variable config for the samples you wish to run.

7. In `monitorLines.java` on line 22, specify the shared line extensions to monitor by DN. For example:
   `String[] lineDNs = { "5016", "5017"};`

![Launch](/IMAGES/launch.png)

## Usage

Once the project is running, it will start monitoring the shared lines specified in the lineDNs[] array.

Upon a call being created to the CTI route point (885016, line 200 on handler.java) A conference will initiate from 5016 to 4030, and then 5017 will be conferenced in.

## Notes

1. In this project, the 11.5 and 12.5 versions of the JTAPI Java library have been deployed to the project's local Maven repo (in `lib/`), with 12.5 being the configured version.

If you want to use 11.5 (or you deploy another version, as below), modify `pom.xml` to specify the desired JTAPI version dependency.  Modify `<version>`:

```xml {"id":"01HWAXPM2EXPE5ESV6DWMQD26M"}
<dependency>
    <groupId>com.cisco.jtapi</groupId>
    <artifactId>jtapi</artifactId>
    <version>12.5</version>
</dependency>

```

2. If  you want to use another JTAPI version in the project:

* Download and install/extract the JTAPI plugin from CUCM (**Applications** / **Plugins**)

* From this repository's root, use Maven to deploy the new version of `jtapi.jar` to the local repo.  You will need to identify the full path to the new `jtapi.jar` installed above:

```bash {"id":"01HWAXPM2EXPE5ESV6DWQV7193"}
mvn deploy:deploy-file -DgroupId=com.cisco.jtapi -DartifactId=jtapi -Dversion={version} -Durl=file:./lib -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile={/path/to/jtapi.jar}

```

> Note: be sure to update {version} and {/path/to/jtapi.jar} to your actual values

3. JTAPI configuration - e.g. trace log number/size/location and various timeouts - can be configured in `jtapi_config/jtapi.ini` (defined as a resource in `pom.xml`)

4. As of v12.5, the Cisco `jtapi.jar` does not implement the [Java Platform Module System](https://www.oracle.com/corporate/features/understanding-java-9-modules.html) (JPMS).  See this [issue](https://github.com/CiscoDevNet/jtapi-samples/issues/1) for more info.

### LICENSE

Provided under Cisco Sample Code License, for details see [LICENSE](LICENSE.md)

### CODE_OF_CONDUCT

Our code of conduct is available [here](CODE_OF_CONDUCT.md)

### CONTRIBUTING

See our contributing guidelines [here](CONTRIBUTING.md)

#### DISCLAIMER:

Please note: This script is meant for demo purposes only. All tools/ scripts in this repo are released for use "AS IS" without any warranties of any kind, including, but not limited to their installation, use, or performance. Any use of these scripts and tools is at your own risk. There is no guarantee that they have been through thorough testing in a comparable environment and we are not responsible for any damage or data loss incurred with their use.
You are responsible for reviewing and testing any scripts you run thoroughly before use in any non-testing environment.
