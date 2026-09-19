# QuietOS Alpha 0.1 Architecture

## Separation of concerns

### Capture
Android NotificationListenerService adapter receives eligible notification events.

### Attention Engine
Transforms normalized events into an explainable decision:
- NOW
- SOON
- DIGEST
- QUIET

Emergency handling must have a deterministic safety path and must not rely solely on generative inference.

### Local AI
A model adapter isolates LiteRT-LM/local-model implementation details from the Attention Engine. The current leading model candidate is Gemma 3 1B IT INT4; prior Qwen benchmarks remain part of the evidence record.

### Delivery
Delivery adapters implement immediate notification, delayed reminder, digest, quiet storage, and later spoken delivery.

### Conversation
Conversation interprets follow-up requests about captured events. It must consume controlled application services rather than reaching directly into Android or connected accounts.

### Tool Router
Future phone/service actions use explicit typed tools. Each tool declares required permission and consequence level.

### Assistant Profile
Assistant identity is data, not hardcoded branding. Personal configuration uses Gemma. Production can default to QuietOS and allow the owner to rename the assistant.

### Presentation
UI, voice, face animation, listening/thinking/speaking states and lip sync remain replaceable presentation modules. They do not own attention decisions.

## Trust boundary
The language model may propose or interpret. Deterministic application policy authorizes and executes consequential actions.

## Alpha exclusions
- Cross-device Attention Token
- QuietOS Network synchronization
- Production signing
- Final visual polish
- Claims of verified local-model performance before physical benchmarks
