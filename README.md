# Nosved

A modern Android video/audio downloader built from scratch, inspired by [Seal](https://github.com/JunkFood02/Seal) by JunkFood02.

## About

Nosved is a video and audio downloading application for Android that leverages powerful open-source libraries to provide seamless content downloading capabilities. This project was built from the ground up with inspiration from the Seal application's architecture and design principles.

## Features

- Download videos and audio from multiple platforms
- Modern Material Design UI
- Built with Kotlin and Android best practices
- Efficient media processing using embedded native libraries

## Attribution & Credits

### Inspired By
This project draws inspiration from [Seal](https://github.com/JunkFood02/Seal) by [JunkFood02](https://github.com/JunkFood02), an excellent GPL-3.0 licensed video/audio downloader for Android. We are grateful to the Seal project for demonstrating best practices in Android media downloading applications.

### Third-Party Libraries

This application uses the following open-source libraries:

#### youtubedl-android
- **Library**: [io.github.junkfood02.youtubedl-android](https://github.com/yausername/youtubedl-android)
- **Developer**: JunkFood02 (forked and maintained)
- **License**: GPL-3.0
- **Components Used**:
  - `io.github.junkfood02.youtubedl-android:library` - Core yt-dlp wrapper library
  - `io.github.junkfood02.youtubedl-android:ffmpeg` - FFmpeg for media processing
  - `io.github.junkfood02.youtubedl-android:aria2c` - Aria2c for download management

These libraries provide the core functionality for video downloading and processing.

## License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** - see the [LICENSE](LICENSE) file for details.

As required by the GPL-3.0 license, this application is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

### Third-Party Licenses

All third-party libraries and components used in this project retain their original licenses:
- youtubedl-android libraries: GPL-3.0
- Seal (inspiration): GPL-3.0

## Building

1. Clone this repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Build and run on your Android device

## Requirements

- Android 5.0 (API 21) or higher
- Android Studio (latest version recommended)

## Disclaimer

This application is intended for downloading content that you have the legal right to download. Users are responsible for complying with applicable laws and the terms of service of the platforms they download from.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Acknowledgments

- Special thanks to [JunkFood02](https://github.com/JunkFood02) for creating Seal and maintaining the youtubedl-android libraries
- Thanks to the yt-dlp community for their excellent video downloading tool
- Thanks to all contributors and open-source developers whose work made this project possible

## Contact

For questions, issues, or suggestions, please open an issue on this repository.

---

**Note**: This project is not affiliated with or endorsed by the Seal project or JunkFood02. It is an independent application inspired by Seal's approach to Android video downloading.
