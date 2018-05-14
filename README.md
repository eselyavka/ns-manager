# Table of contents
1. [About ns-manager](#about-ns-manager)
2. [Notes on compatibility](#notes-on-compatibility)
3. [How to build](#how-to-build)
4. [How to use](#how-to-use)

# About ns-manager
NS-manager is a tool to manager [Citrix Netscaler](https://www.citrix.com/) app utilizing [nitro API](https://docs.citrix.com/en-us/netscaler/11-1/nitro-api.html).

# Notes on compatibility
As of version **1.2** tool was tested and should work with the _Citrix Netscaler VPX 11.1_ and following version of external dependencies:

| Dependency          | Versions                     |
| :-----------------: | :--------------------------: |
| Nitro               | 10.1                         |

# How to build

Like any other maven project ```mvn clean package```.

# How to use

You need username with password which have full access to services in Citrix Netscaler.
 
 ```java -jar ServiceManager.jar <ip,ip2,...ipn> <username> <password> <servername> <port> <action>```
 Where:
 
 * `<ip,ip2,...ipn>` - ip of the Citrix Netscaler host (delimiter - `,` if action must be performed, across several instances)
 * `username` - username in Citrix Netscaler user database
 * `password` - password in Citrix Netscaler user database
 * `servername` - server name which should be `enable/disabled`
 * `port` - server name port which should be `enable/disabled`
 * `action` - `enable/disabled`
 