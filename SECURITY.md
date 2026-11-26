# Security

VEX APK Signer must treat uploaded Android project ZIP files as untrusted input.

- Build projects only in disposable, isolated workers.
- Never mount host filesystems, Docker sockets, cloud credentials, or production secrets into build jobs.
- Apply CPU, memory, disk, time, and network limits.
- Keep signing keystores and passwords out of build containers and logs.
- Inject signing credentials only in a separate signing stage after compilation.
- Authenticate users, authorize access to each build, and expire uploaded files and artifacts.
- Validate ZIP paths and reject path traversal, oversized archives, and unsupported project types.

Do not upload personal keystores or passwords into project ZIP files.