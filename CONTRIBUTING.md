# Contributing to SynapSys

Thanks for taking the time to contribute! This document covers the basics to get you started.

---

## Before you start

- Check the [open issues](../../issues) — your idea or bug might already be tracked
- For significant changes, **open an issue first** to discuss your approach before writing code
- All contributions are subject to the [Apache 2.0 License](LICENSE)

---

## Development setup

See the [README](README.md) for prerequisites and local setup instructions.

---

## Workflow

1. Fork the repository
2. Create a branch from `main`:
   ```bash
   git checkout -b feat/your-feature-name
   # or
   git checkout -b fix/your-bug-description
   ```
3. Make your changes
4. Add or update tests if applicable
5. Commit with a clear message (see below)
6. Push and open a Pull Request against `main`

---

## Commit messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add enrollment token expiry validation
fix: handle WebSocket reconnect race condition
docs: update deployment guide
refactor: extract certificate manager to separate package
test: add unit tests for permission resolution
chore: bump Go version to 1.23
```

---

## Pull Request guidelines

- Keep PRs focused — one feature or fix per PR
- Include a clear description of what changed and why
- Link to the relevant issue if one exists (`Closes #42`)
- Make sure CI passes before requesting review

---

## Reporting bugs

Open an issue with:
- A clear title and description
- Steps to reproduce
- Expected vs actual behavior
- Version of the API / agent and OS

---

## Code of conduct

Be respectful. We're all here to build something useful.
