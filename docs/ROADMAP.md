# FastTTS Roadmap 🗺️

**Vision:** To provide a unified, high-performance interface for text-to-speech supporting multiple backends with zero framework bloat.

## 🟢 v0.1.1: Unified Backend Architecture (Current)
- [x] **Multi-Backend Support**: Piper, Windows SAPI, ElevenLabs, Deepgram
- [x] **Unified Demo**: Single demo script with timing metrics
- [x] **External Piper Support**: Environment variable and standard path discovery
- [x] **Documentation**: FastJava ecosystem style documentation
- [x] **Performance Metrics**: Load time, synthesis time, total time tracking

## 🟡 v0.2.0: Enhanced Features
- [ ] **Voice Selection**: Voice switching per backend
- [ ] **Streaming Support**: Real-time audio streaming during synthesis
- [ ] **Configuration Management**: Per-backend configuration files
- [ ] **Extended Backend Support**: Additional cloud and offline providers
- [ ] **Caching**: Audio output caching for repeated text

## 🟠 v0.5.0: Platform Expansion
- [ ] **Linux Support**: Native Linux TTS backends
- [ ] **macOS Support**: macOS system voices and model support
- [ ] **Cross-Platform Model Management**: Unified model installation across platforms
- [ ] **Advanced Metrics**: Detailed performance profiling and optimization

## 🔴 v1.0.0: Production Hardening
- [ ] **Full Stability Audit**: Long-run stress testing across all backends
- [ ] **Enterprise Support**: Batch processing, concurrent synthesis
- [ ] **Documentation**: Complete API reference and usage examples
- [ ] **Performance Optimization**: Further reduction in synthesis latency

---
**Focus:** Unified interface, maximum flexibility, zero bloat.