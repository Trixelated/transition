# OpenGL Transition App

This Android application demonstrates how to implement smooth transitions using OpenGL. The app allows users to transition between images with various OpenGL-based animations.

## Features

- Smooth transitions between images.
- Multiple transition effects like fade, slide, zoom, etc.
- Customizable transition duration and parameters.

## Implementation Details

- The app uses OpenGL for rendering transition effects.
- Transition logic is implemented in the `FilterGLProgram` class.
- Various transition effects are defined under `Filters` enum.
- Images to be transitioned are passed as textures to OpenGL.
- Transition parameters like duration, easing function, etc., are passed as uniform variables to the shaders.


## Steps to Run the App

1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the app on your Android device or emulator.

