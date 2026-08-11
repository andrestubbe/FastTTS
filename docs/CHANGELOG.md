# Changelog

All notable changes to this project will be documented in this file.

## [0.1.1] - 2026-08-11

### Added
- Unified demo script with timing metrics for all backends
- Support for external Piper installation via environment variable
- Automatic Piper discovery in standard installation paths
- Performance timing (load time, synthesis time, total time)
- Audio duration calculation and display
- Documentation improvements matching FastJava ecosystem style

### Changed
- Simplified project structure (removed experimental ONNX backend)
- Updated README.md to match FastAIRag documentation style
- Moved model files to dedicated `models/` directory
- Improved error messages and demo output
- Refactored backend selection logic

### Removed
- Experimental PiperONNXBackend (phonemization issues)
- Complex demo examples (replaced with single unified demo)
- Installer examples (documentation simplified)

## [0.1.0] - 2026-05-23

### Added
- Initial release
- Standardized FastJava ecosystem module