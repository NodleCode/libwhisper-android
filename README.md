# Whisper Tracing Protocol
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE.txt)

Whisper Tracing is a decentralized and proximity-based contact tracing
protocol. When running, the library locally generates temporary IDs and
uses Bluetooth Low Energy (BLE) to advertise those IDs and detect
*proximity event* with other whisper users. The library can be fed
with *infected* IDs that are processed locally to compute a risk
score based on the proximity log. You can read the
[white paper](https://docsend.com/view/nis3dac) for more details.

Note that this library does not interact directly with
the backend and does not manage its lifecycle to run continuously
in the background. This must be done by the host application that embeds
the library. The reference application for whisper tracing is  
<img src="https://uploads-ssl.webflow.com/5e835869a55982d3145edfec/5e835aaaa559827e0a5eeae4_ios_icon-32x32px.png" width="15">**COALITION**. You can download it from google play:

<p align="center">
    <a href="https://play.google.com/store/apps/details?id=world.coalition.app">
        <img src="https://cdn.rawgit.com/steverichey/google-play-badge-svg/master/img/en_get.svg" width="200">
    </a>
</p>

## Using the library

1. Add the JitPack repository.
```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```

2. Add the libwhisper dependency
```
dependencies {
        implementation 'com.github.NodleCode:libwhisper-android:3.0'
}
```

## Additional Notes

Whisper Tracing is a work in progress and contribution are welcome. If  
you have any question, ideas or if you found a bug, please open an issue!
