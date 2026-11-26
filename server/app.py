import os, re, uuid, zipfile, shutil, threading, time
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
from fastapi import FastAPI, UploadFile, File, Form, Header, HTTPException
from fastapi.responses import FileResponse
import docker

app = FastAPI(title="VEX APK Signer Build API", version="0.1.0")
ROOT = Path(os.getenv("JOB_ROOT", "/var/lib/vex/jobs"))
TOKEN = os.getenv("VEX_API_TOKEN", "")
WORKER_IMAGE = os.getenv("VEX_WORKER_IMAGE", "vex-apk-worker:latest")
MAX_UPLOAD = 200 * 1024 * 1024
ROOT.mkdir(parents=True, exist_ok=True)
jobs = {}
lock = threading.Lock()
pool = ThreadPoolExecutor(max_workers=int(os.getenv("MAX_CONCURRENT_BUILDS", "1")))

def authorize(header):
    if not TOKEN or header != f"Bearer {TOKEN}":
        raise HTTPException(401, "Invalid or missing API token")

def validate_zip(path):
    try:
        with zipfile.ZipFile(path) as z:
            entries = z.infolist()
            if not entries or len(entries) > 5000:
                raise ValueError()
            total = 0
            for x in entries:
                name = x.filename.replace("\\", "/")
                if name.startswith("/") or ".." in Path(name).parts:
                    raise ValueError()
                total += x.file_size
                if total > 2 * 1024 * 1024 * 1024:
                    raise ValueError()
            if z.testzip() is not None:
                raise ValueError()
    except Exception:
        raise HTTPException(400, "Invalid, unsafe, or oversized project ZIP")

def run_build(job_id, source, output, fmt):
    with lock:
        jobs[job_id]["status"] = "running"
        jobs[job_id]["message"] = "Build running in an isolated worker"
    client = docker.from_env()
    try:
        container = client.containers.run(
            WORKER_IMAGE,
            command=[f"/input/project.zip", "/output", fmt],
            detach=True,
            network_mode="bridge",
            volumes={
                str(source): {"bind": "/input/project.zip", "mode": "ro"},
                str(output): {"bind": "/output", "mode": "rw"},
            },
            mem_limit="4g", nano_cpus=2_000_000_000, pids_limit=256,
            cap_drop=["ALL"], security_opt=["no-new-privileges:true"],
            read_only=True, tmpfs={"/tmp": "rw,nosuid,nodev,size=3g"},
            environment={"GRADLE_USER_HOME": "/tmp/gradle", "ANDROID_USER_HOME": "/tmp/android"},
            auto_remove=False
        )
        result = container.wait(timeout=1600)
        code = result.get("StatusCode", 1)
        if code != 0 or not (output / "result.txt").exists():
            with lock:
                jobs[job_id].update(status="failed", message="Build failed. Check the Android Gradle project.")
            return
        names = (output / "result.txt").read_text().splitlines()
        with lock:
            jobs[job_id].update(status="succeeded", message="Build artifacts are ready (unsigned).", artifacts=names)
    except Exception:
        with lock:
            jobs[job_id].update(status="failed", message="Build worker error. Check server and worker configuration.")
    finally:
        try:
            container.remove(force=True)
        except Exception:
            pass
        client.close()

@app.get("/health")
def health():
    return {"status": "ok", "service": "VEX APK Signer Build API"}

@app.post("/v1/builds")
async def create_build(project_zip: UploadFile = File(...), output: str = Form("both"),
                       authorization: str = Header(default="")):
    authorize(authorization)
    if output not in ("apk", "aab", "both"):
        raise HTTPException(400, "output must be apk, aab, or both")
    job_id = uuid.uuid4().hex
    job_dir = ROOT / job_id
    job_dir.mkdir(mode=0o700)
    source = job_dir / "project.zip"
    size = 0
    try:
        with source.open("wb") as f:
            while chunk := await project_zip.read(1024 * 1024):
                size += len(chunk)
                if size > MAX_UPLOAD:
                    raise HTTPException(413, "Project ZIP exceeds 200 MB")
                f.write(chunk)
    except Exception:
        shutil.rmtree(job_dir, ignore_errors=True)
        raise
    validate_zip(source)
    output_dir = job_dir / "artifacts"
    output_dir.mkdir(mode=0o700)
    with lock:
        jobs[job_id] = {"status":"queued", "message":"Build queued", "created":int(time.time()), "artifacts":[]}
    pool.submit(run_build, job_id, source, output_dir, output)
    return {"build_id": job_id, "status":"queued"}

@app.get("/v1/builds/{job_id}")
def get_build(job_id: str, authorization: str = Header(default="")):
    authorize(authorization)
    with lock:
        job = jobs.get(job_id)
        if not job: raise HTTPException(404, "Build not found")
        return {"build_id":job_id, "status":job["status"], "message":job["message"]}

@app.get("/v1/builds/{job_id}/artifacts")
def get_artifacts(job_id: str, authorization: str = Header(default="")):
    authorize(authorization)
    with lock:
        job = jobs.get(job_id)
        if not job: raise HTTPException(404, "Build not found")
        if job["status"] != "succeeded": raise HTTPException(409, "Build is not complete")
    folder = ROOT / job_id / "artifacts"
    files = [p for p in folder.iterdir() if p.is_file() and p.suffix in (".apk", ".aab")]
    if not files: raise HTTPException(404, "No artifacts found")
    import zipfile, io
    bundle = folder / "vex-build-artifacts.zip"
    with zipfile.ZipFile(bundle, "w", zipfile.ZIP_DEFLATED) as z:
        for p in files: z.write(p, p.name)
    return FileResponse(bundle, filename="VEX-Build-Artifacts.zip", media_type="application/zip")
