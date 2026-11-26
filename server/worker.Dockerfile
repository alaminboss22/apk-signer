FROM ghcr.io/cirruslabs/android-sdk:35
USER root
RUN apt-get update && apt-get install -y --no-install-recommends python3 ca-certificates && rm -rf /var/lib/apt/lists/*
COPY worker.py /opt/vex-worker.py
ENTRYPOINT ["python3", "/opt/vex-worker.py"]
