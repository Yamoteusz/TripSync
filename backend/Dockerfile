FROM python:3.13-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /app
RUN groupadd --gid 10001 tripsync && useradd --uid 10001 --gid tripsync --no-create-home tripsync && mkdir /data && chown tripsync:tripsync /data
COPY server.py /app/server.py
USER tripsync
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8080/health', timeout=3)"
CMD ["python", "server.py", "--host", "0.0.0.0", "--db", "/data/tripsync.sqlite"]
