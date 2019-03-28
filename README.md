# ndn-photo-app

npChat is a photo and file sharing application built on Android and is inspired by Snapchat. The project's
goals are to develop a completely decentralized application that runs over the Named Data Network (NDN), utilize
a partial sync protocol, and utilize a Web-Of-Trust like model instead of the traditional NDN hierarchical model.

## Prerequisites
npChat requires the NDN Forwarding Daemon be run alongside it. NFD can be installed via [Google Play](https://play.google.com/store/apps/details?id=net.named_data.nfd) or directly from the [source](https://github.com/named-data-mobile/NFD-android).

## Building

To build the application, it is recommended that one uses Android Studio. Simply pull this repository and allow the project to build itself. No other installations are required at this point of time. All dependencies are pulled from external sources when the build file executes. This application only works on Android OS 6.0 and above.

## Testing
Launch NFD and check "NFD is started." 

npChat can communicate via:
1. NFD's Wifi Direct feature: Within the NFD app setup WiFi direct between phones manually. Usually this is done once and NFD remembers. If there are problems, please force close NFD and setup the WiFi direct again. More details about NFD's wifi direct feature can be found [here](https://redmine.named-data.net/projects/nfd-android/wiki/NDN_Over_WiFi_Direct_Protocol_Specification).

2. Automatically over a network that supports UDP multicast. Please note that UDP multicast has been known to exhibit higher packet loss than UDP unicast over WiFi direct.

Once NFD is started, launch npChat and register a username and password for that device. Friends are added under the "Friends" menu by having each device display its QR code and the other device scanning the code in turn.

Click "Camera," take a picture, and follow the on-screen steps to share a picture with friends.

For our tests, we have been using two MotoX (2nd Gen) phones running LineageOS version 13 with Android 6.0.1.

## Contributing
Please read the [NDN Contributing Guide](https://github.com/named-data/NFD/blob/master/CONTRIBUTING.md). We are not currently using Gerrit on this project, so that part can be ignored for now. Contributions should follow the [NDN Style Guide](https://named-data.net/codebase/platform/documentation/ndn-platform-development-guidelines/cpp-code-guidelines/).

## Communication

Our chat channel is on [Slack](https://named-data.slack.com/messages)

Please join our mailing list to discuss questions regarding the project: https://named-data.net/codebase/platform/support/mailing-lists/

