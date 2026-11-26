import os, sys, zipfile, shutil, subprocess, pathlib, stat, time

archive, output_dir, requested = sys.argv[1:4]
out = pathlib.Path(output_dir)
work = pathlib.Path("/tmp/vex-project")
work.mkdir(parents=True, exist_ok=True)
out.mkdir(parents=True, exist_ok=True)

def safe_extract(zf, target):
    root = target.resolve()
    for info in zf.infolist():
        name = info.filename.replace("\\", "/")
        p = pathlib.PurePosixPath(name)
        if p.is_absolute() or ".." in p.parts:
            raise RuntimeError("Unsafe ZIP path rejected")
        mode = info.external_attr >> 16
        if stat.S_ISLNK(mode):
            raise RuntimeError("Symbolic links are not accepted")
        dest = (target / pathlib.Path(*p.parts)).resolve()
        if root not in dest.parents and dest != root:
            raise RuntimeError("Unsafe ZIP entry rejected")
        if info.file_size > 1024 * 1024 * 1024:
            raise RuntimeError("ZIP entry too large")
        zf.extract(info, target)

with zipfile.ZipFile(archive) as z:
    safe_extract(z, work)

wrappers = list(work.rglob("gradlew"))
wrappers = [p for p in wrappers if len(p.relative_to(work).parts) <= 4 and p.is_file()]
if len(wrappers) != 1:
    raise RuntimeError("ZIP must contain exactly one Android Gradle project wrapper (gradlew)")
project = wrappers[0].parent
wrapper = project / "gradlew"
wrapper.chmod(wrapper.stat().st_mode | 0o111)

tasks = []
if requested in ("apk", "both"): tasks.append("assembleRelease")
if requested in ("aab", "both"): tasks.append("bundleRelease")
cmd = [str(wrapper), "--no-daemon", "--stacktrace"] + tasks
start = time.time()
result = subprocess.run(cmd, cwd=project, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                        text=True, timeout=1500, env={**os.environ, "CI":"true"})
if result.returncode:
    # Do not return arbitrary Gradle output; it may contain credentials printed by build scripts.
    raise RuntimeError("Gradle build failed. Check the project and its Gradle configuration.")

found = []
for pattern in ("**/build/outputs/apk/release/*.apk", "**/build/outputs/bundle/release/*.aab"):
    for src in project.glob(pattern):
        if src.is_file():
            dest = out / src.name
            shutil.copy2(src, dest)
            found.append(dest.name)
if not found:
    raise RuntimeError("Build finished but no release APK/AAB artifacts were found.")
with open(out / "result.txt", "w") as f:
    f.write("\n".join(found))
